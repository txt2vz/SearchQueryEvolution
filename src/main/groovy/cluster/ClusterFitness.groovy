package cluster

import ec.simple.SimpleFitness
import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.search.*

@CompileStatic
public class ClusterFitness extends SimpleFitness {

    Map<Query, Integer> queryMap = [:]
    private double positiveScoreTotal = 0.0
    private double negativeScoreTotal = 0.0
    private double fraction = 0.0
    private double baseFitness = 0.0
    private double scoreOnly = 0.0
    private double scorePlus = 0.0

    private int positiveHits = 0
    private int negativeHits = 0
    private int hitsOnly = 0
    private int hitsPlus =0
    private int coreClusterPenalty = 0
    private int totalHits = 0
    private int missedDocs = 0
    private int zeroHitsCount = 0
    private int coreHitPenalty = 0

    private int hitsPerPage = Indexes.indexReader.maxDoc()
    private final static int coreClusterSize = 10

    double getFitness() {
        return baseFitness;
    }
/**
 * @param bqbArray an array of lucene boolean queries
 */
    void setClusterFitness(List<BooleanQuery.Builder> bqbArray) {
        assert bqbArray.size() == Indexes.NUMBER_OF_CLUSTERS

        positiveScoreTotal = 0.0
        negativeScoreTotal = 0.0
        fraction = 0.0
        baseFitness = 0.0
        positiveHits = 0
        negativeHits = 0
        coreClusterPenalty = 0
        totalHits = 0
        missedDocs = 0
        zeroHitsCount = 0
        scoreOnly = 0.0
        scorePlus = 0.0
        hitsOnly = 0
        hitsPlus = 0
        coreHitPenalty=1

        Map<Query, Integer> qMap = new HashMap<Query, Integer>()
        Set<Integer> allHits = [] as Set

        bqbArray.eachWithIndex { BooleanQuery.Builder bqb, index ->

            Query q = bqb.build()

            Set<Integer> otherdocIdSet = [] as Set<Integer>
            List<BooleanQuery.Builder> otherQueries = bqbArray - bqb

            BooleanQuery.Builder bqbOthers = new BooleanQuery.Builder();
            otherQueries.each { obqb ->
                bqbOthers.add(obqb.build(), BooleanClause.Occur.SHOULD)
            }
            Query otherBQ = bqbOthers.build()

            TopDocs otherTopDocs = Indexes.indexSearcher.search(otherBQ, hitsPerPage)
            ScoreDoc[] hitsOthers = otherTopDocs.scoreDocs;
            hitsOthers.each { ScoreDoc otherHit -> otherdocIdSet << otherHit.doc }

            TopDocs docs = Indexes.indexSearcher.search(q, hitsPerPage)
            ScoreDoc[] hits = docs.scoreDocs;
            qMap.put(q, hits.size())

            if (hits.size() < 1) zeroHitsCount++

            hits.eachWithIndex { ScoreDoc d, int position ->
                allHits << d.doc

                if (otherdocIdSet.contains(d.doc)) {
                    negativeHits++;

                    negativeScoreTotal = negativeScoreTotal + d.score
                    if (position < coreClusterSize) {
                        coreClusterPenalty++
                        coreHitPenalty = coreHitPenalty + (coreClusterSize - position)
                    }
                } else {
                    positiveHits++
                    positiveScoreTotal = positiveScoreTotal + d.score
                }
            }
        }

        final int minScore = -2000;
        queryMap = qMap.asImmutable()
        if (zeroHitsCount == 0) {
            totalHits = allHits.size()
            fraction = totalHits / Indexes.indexReader.maxDoc()
            missedDocs = Indexes.indexReader.maxDoc() - allHits.size()
            scoreOnly = positiveScoreTotal - negativeScoreTotal    //(negativeScoreTotal + coreHitPenalty)
            scorePlus = (scoreOnly < minScore) ? 0 : scoreOnly + Math.abs(minScore)
            //baseFitness = scorePlus

            hitsOnly = positiveHits - (negativeHits + coreHitPenalty)
            hitsPlus = (hitsOnly <= minScore) ? 0 : hitsOnly + Math.abs(minScore)
            baseFitness = (double) hitsPlus
          //  baseFitness = (double) hitsPlus * fraction * fraction

            //* fraction * fraction
           // baseFitness = (scorePlus / (coreClusterPenalty + 1)) //* fraction * fraction
        }
    }

    void generationStats(long generation) {
        println "Gereration $generation BaseFitness: ${baseFitness.round(2)} ScorePlus: ${scorePlus.round(2)} ${queryShort()}"
        println "PosHits: $positiveHits NegHits: $negativeHits PosScr: ${positiveScoreTotal.round(2)} NegScr: ${negativeScoreTotal.round(2)} CoreClstPen: $coreClusterPenalty CoreHitPenalty: $coreHitPenalty"
        println "TotalHits: $totalHits TotalDocs: ${Indexes.indexReader.maxDoc()} MissedDocs: $missedDocs Fraction: $fraction ZeroHits: $zeroHitsCount hitsOnly: $hitsOnly "
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

    public String fitnessToStringForHumans() {
        return "ClusterQuery Fitness: ${this.fitness()}  ${queryShort()}"
    }

    public String toString(int gen) {
        return "Gen: $gen ClusterQuery Fitness: ${this.fitness()} qMap: $queryMap}"
    }
}
