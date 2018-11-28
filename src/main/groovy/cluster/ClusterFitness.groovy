package cluster

import ec.simple.SimpleFitness
import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.search.*

@CompileStatic
enum FitnessMethod {
    SCORE, HITS, PSEUDOF1
}

@CompileStatic
enum IntersectMethod {
     HITS10, HITS20, HITS30, TEN_PERECENT_TOTAL_DIV_K, COOCCURRENCE
}

@CompileStatic
public class ClusterFitness extends SimpleFitness {

    static FitnessMethod fitnessMethod// = FitnessMethod.SCORE
    static IntersectMethod intersectMethod// = cluster.IntersectMethod.TEN_PERECENT_TOTAL_DIV_K

    private final int totalDocs = Indexes.indexReader.maxDoc()

    Map<Query, Integer> queryMap = [:]
    double baseFitness = 0.0
    double scorePlus = 0.0

    double positiveScoreTotal = 0.0
    double negativeScoreTotal = 0.0
    double scoreOnly = 0.0
    double pseudo_precision = 0.0
    double pseudo_recall = 0.0
    double pseudo_f1 = 0.0

    int hitsPlus = 0
    int positiveHits = 0
    int negativeHits = 0
    int hitsOnly = 0
    int totalHits = 0
    int missedDocs = 0

    private final int hitsPerPage = totalDocs

    double getFitness() {
        return baseFitness;
    }

    void setClusterFitness(Set<BooleanQuery.Builder> bqbSet) {

        positiveScoreTotal = 0.0
        negativeScoreTotal = 0.0
        baseFitness = 0.0
        positiveHits = 0
        negativeHits = 0
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

            // otherQueries should not be null
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

            for (ScoreDoc otherHit : hitsOthers) {
                otherDocIdSet << otherHit.doc
            }

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

        queryMap = qMap.asImmutable()

        totalHits = allHits.size()
        missedDocs = totalDocs - allHits.size()

        pseudo_precision = positiveHits / totalHits
        pseudo_recall = totalHits / totalDocs
        pseudo_f1 = 2 * (pseudo_precision * pseudo_recall) / (pseudo_precision + pseudo_recall)

        final int minScore = -2000;
        switch (fitnessMethod) {

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

            case fitnessMethod.PSEUDOF1:
                baseFitness = pseudo_f1
                break
        }
    }

    void generationStats(long generation) {
        println "Gereration $generation BaseFitness: ${baseFitness.round(2)} ${queryShort()}"
        println "PosHits: $positiveHits NegHits: $negativeHits PosScr: ${positiveScoreTotal.round(2)} NegScr: ${negativeScoreTotal.round(2)} pseudo_precision $pseudo_precision pseudo_recall $pseudo_recall pseudo_f1 $pseudo_f1"
        println "TotalHits: $totalHits TotalDocs: $totalDocs MissedDocs: $missedDocs hitsOnly: $hitsOnly scoreOnly: ${scoreOnly.round(2)} hitsPlus : $hitsPlus ScorePlus: ${scorePlus.round(2)} "
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
        return "ClusterQuery Fitness: ${this.fitness()}" //scorePlus $scorePlus hitsPlus $hitsPlus "// ${queryShort()}"
    }

    public String toString(int gen) {
        return "Gen: $gen ClusterQuery Fitness: ${this.fitness()} qMap: $queryMap}"
    }
}