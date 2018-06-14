package cluster

import ec.simple.SimpleFitness
import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.search.*

@CompileStatic
enum FitnessMethod {
    SCORE, HITS, P_TIMES_R, SETK, POS_DIV_NEG
}

@CompileStatic
public class ClusterFitness extends SimpleFitness {

    static FitnessMethod fitnessMethod// = FitnessMethod.SCORE

    Map<Query, Integer> queryMap = [:]
    double baseFitness = 0.0
    double scorePlus = 0.0
    int hitsPlus = 0

    private double positiveScoreTotal = 0.0
    private double negativeScoreTotal = 0.0
    private double fraction = 0.0
    private double scoreOnly = 0.0
    private double precision = 0.0
    private double recall = 0.0
    private int positiveHits = 0
    private int negativeHits = 0
    private int hitsOnly = 0
    private int coreClusterPenalty = 0
    private int totalHits = 0
    private int missedDocs = 0
    private int lowHitsCount = 0
   // private int coreHitPenalty = 0
    int numberOfClusters = 0

    private int hitsPerPage = Indexes.indexReader.maxDoc()
    private final static int coreClusterSize = 10

    double getFitness() {
        return baseFitness;
    }

    void setClusterFitness(List<BooleanQuery.Builder> bqbArray, int k) {
        numberOfClusters = k
        setClusterFitness(bqbArray)
    }

    void setClusterFitness(List<BooleanQuery.Builder> bqbArray) {
        numberOfClusters = bqbArray.size()
        //        assert bqbArray.size() == Indexes.NUMBER_OF_CLUSTERS

        positiveScoreTotal = 0.0
        negativeScoreTotal = 0.0
        fraction = 0.0
        baseFitness = 0.0
        positiveHits = 0
        negativeHits = 0
        coreClusterPenalty = 0
        totalHits = 0
        missedDocs = 0
        lowHitsCount = 0
        scoreOnly = 0.0
        scorePlus = 0.0
        hitsOnly = 0
        hitsPlus = 0
        //coreHitPenalty = 1
        precision = 0.0
        recall = 0.0

        Map<Query, Integer> qMap = new HashMap<Query, Integer>()
        Set<Integer> allHits = [] as Set

        for (BooleanQuery.Builder bqb : bqbArray) {

            Query q = bqb.build()

            Set<Integer> otherDocIdSet = [] as Set<Integer>
            List<BooleanQuery.Builder> otherQueries = bqbArray - bqb

            BooleanQuery.Builder bqbOthers = new BooleanQuery.Builder();

            for (BooleanQuery.Builder obqb : otherQueries) {
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

            if (hits.size() < 2) lowHitsCount++
            for (ScoreDoc d : hits) {

            //   if (fitnessMethod == FitnessMethod.P_TIMES_R) {
                   allHits << d.doc
            //   }

                if (otherDocIdSet.contains(d.doc)) {
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
        if (lowHitsCount == 0) {

            // fraction = totalHits / Indexes.indexReader.maxDoc()
           // missedDocs = Indexes.indexReader.maxDoc() - allHits.size()

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
                case fitnessMethod.P_TIMES_R:
                    totalHits = allHits.size()
                    precision = positiveHits / totalHits
                    recall = totalHits / Indexes.indexReader.maxDoc()
                    baseFitness = precision * recall
                    break
                case fitnessMethod.SETK:
                    hitsOnly = positiveHits - negativeHits
                    hitsPlus = (hitsOnly <= minScore) ? 0 : hitsOnly + Math.abs(minScore)
                    baseFitness = hitsPlus
                    break;
                case fitnessMethod.POS_DIV_NEG:
                    baseFitness = (double) positiveHits / (negativeHits + 1)
                    break;
            }
        }

        //  baseFitness = (2 * precision * recall) / (precision + recall)
        //  baseFitness = (double) hitsPlus * fraction * fraction
        // baseFitness = (scorePlus / (coreClusterPenalty + 1)) //* fraction * fraction
    }

    void generationStats(long generation) {
        println "Gereration $generation BaseFitness: ${baseFitness.round(2)} ${queryShort()}"
        println "PosHits: $positiveHits NegHits: $negativeHits PosScr: ${positiveScoreTotal.round(2)} NegScr: ${negativeScoreTotal.round(2)} CoreClstPen: $coreClusterPenalty precision $precision recall $recall"
        println "TotalHits: $totalHits TotalDocs: ${Indexes.indexReader.maxDoc()} MissedDocs: $missedDocs Fraction: ${fraction.round(2)} hitsOnly: $hitsOnly scoreOnly: ${scoreOnly.round(2)} hitsPlus : $hitsPlus ScorePlus: ${scorePlus.round(2)} "
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
