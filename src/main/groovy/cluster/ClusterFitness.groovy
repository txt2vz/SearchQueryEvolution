package cluster

import ec.simple.SimpleFitness
import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.search.*

@CompileStatic
enum FitnessMethod {
    SCORE, HITS, PSEUDOF1, F1_0
}

@CompileStatic
public class ClusterFitness extends SimpleFitness {

    static FitnessMethod fitnessMethod// = FitnessMethod.SCORE

    Map<Query, Integer> queryMap = [:]
    double baseFitness = 0.0
    double scorePlus = 0.0
    int hitsPlus = 0

    double positiveScoreTotal = 0.0
    double negativeScoreTotal = 0.0
    double scoreOnly = 0.0
    double pseudo_precision = 0.0
    double pseudo_recall = 0.0
    double pseudo_f1 = 0.0
    int positiveHits = 0
    int negativeHits = 0
    int hitsOnly = 0
    int coreClusterPenalty = 0
    int totalHits = 0
    int missedDocs = 0

    private int hitsPerPage = Indexes.indexReader.maxDoc()

    double getFitness() {
        return baseFitness;
    }

    void setClusterFitness(Set<BooleanQuery.Builder> bqbSet) {

        positiveScoreTotal = 0.0
        negativeScoreTotal = 0.0
        baseFitness = 0.0
        positiveHits = 0
        negativeHits = 0
        coreClusterPenalty = 0
        totalHits = 0
        missedDocs = 0
        scoreOnly = 0.0
        scorePlus = 0.0
        hitsOnly = 0
        hitsPlus = 0
        pseudo_precision = 0.0
        pseudo_recall = 0.0

        Map<Query, Integer> qMap = new HashMap<Query, Integer>()
        Set<Integer> allHits = [] as Set
       // Set<Integer> negDocs = [] as Set  //to count negDoc only once

        for (BooleanQuery.Builder bqb : bqbSet) {

            Query q = bqb.build()

            Set<Integer> otherDocIdSet = [] as Set<Integer>
            Set<BooleanQuery.Builder> otherQueries = bqbSet - bqb

            // if (otherQueries == null) println "other queries null"
            assert otherQueries

            BooleanQuery.Builder bqbOthers = new BooleanQuery.Builder();

            for (BooleanQuery.Builder obqb : otherQueries) {

                //obqb should not be null
                assert obqb
                bqbOthers.add(obqb.build(), BooleanClause.Occur.SHOULD)
            }
            Query otherBQ = bqbOthers.build()

            //collect docid from other queries
            TopDocs otherTopDocs = Indexes.indexSearcher.search(otherBQ, hitsPerPage)
            ScoreDoc[] hitsOthers = otherTopDocs.scoreDocs;
            hitsOthers.each { ScoreDoc otherHit -> otherDocIdSet << otherHit.doc }

            TopDocs docs = Indexes.indexSearcher.search(q, hitsPerPage)
            ScoreDoc[] hits = docs.scoreDocs;
            qMap.put(q, hits.size())

            for (ScoreDoc d : hits) {

                allHits << d.doc

                if (otherDocIdSet.contains(d.doc)) {
                    //if (negDocs.add(d.doc)){  // count negDoc only once
                    negativeHits++
                    negativeScoreTotal = negativeScoreTotal + d.score

                } else {
                    positiveHits++
                    positiveScoreTotal = positiveScoreTotal + d.score
                }
            }
        }

        final int minScore = -2000;
        queryMap = qMap.asImmutable()

        totalHits = allHits.size()
        missedDocs = Indexes.indexReader.maxDoc() - allHits.size()
        pseudo_precision = positiveHits / totalHits
        pseudo_recall = totalHits / Indexes.indexReader.maxDoc()
        pseudo_f1 = 2 * (pseudo_precision * pseudo_recall) / (pseudo_precision + pseudo_recall)

        switch (fitnessMethod) {
            case fitnessMethod.POS:
                baseFitness = positiveScoreTotal

            case fitnessMethod.SCORE:
                scoreOnly = positiveScoreTotal - negativeScoreTotal
                scorePlus = (scoreOnly < minScore) ? 0 : scoreOnly + Math.abs(minScore)
                baseFitness = scorePlus
                break;
            case fitnessMethod.HITS:
                hitsOnly = positiveHits - negativeHits
                hitsPlus = (hitsOnly <= minScore) ? 0 : hitsOnly + Math.abs(minScore)
                baseFitness = hitsPlus
                break;

            case fitnessMethod.F1_0:
                baseFitness = pseudo_f1
                break
            case fitnessMethod.POS_DIV_NEG:
                baseFitness = (double) positiveHits / (negativeHits + 1)
                break;
            case fitnessMethod.PSEUDOF1:
                baseFitness = 2 * positiveHits / (2 * positiveHits + negativeHits + missedDocs)
                break
        }
    }

    void generationStats(long generation) {
        println "Gereration $generation BaseFitness: ${baseFitness.round(2)} ${queryShort()}"
        println "PosHits: $positiveHits NegHits: $negativeHits PosScr: ${positiveScoreTotal.round(2)} NegScr: ${negativeScoreTotal.round(2)} pseudo_precision $pseudo_precision pseudo_recall $pseudo_recall pseudo_f1 $pseudo_f1"
        println "TotalHits: $totalHits TotalDocs: ${Indexes.indexReader.maxDoc()} MissedDocs: $missedDocs hitsOnly: $hitsOnly scoreOnly: ${scoreOnly.round(2)} hitsPlus : $hitsPlus ScorePlus: ${scorePlus.round(2)} "
        println ""
    }

    String queryShort() {
        def s = "\n"// "queryMap.size ${queryMap.size()} \n"
        queryMap.keySet().eachWithIndex { Query q, int index ->
            if (index > 0) s += '\n';
            s += "ClusterQuery: $index :  ${queryMap.get(q)}  ${q.toString(Indexes.FIELD_CONTENTS)}"
        }
        return s
    }

    //sent to stat file in statDump
    public String fitnessToStringForHumans() {
        return "ClusterQuery Fitness: ${this.fitness()} scorePlus $scorePlus hitsPlus $hitsPlus "// ${queryShort()}"
    }

    public String toString(int gen) {
        return "Gen: $gen ClusterQuery Fitness: ${this.fitness()} qMap: $queryMap}"
    }
}