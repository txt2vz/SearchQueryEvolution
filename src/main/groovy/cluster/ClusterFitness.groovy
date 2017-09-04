package cluster

import ec.simple.SimpleFitness
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import index.IndexInfo
import org.apache.lucene.document.Document
import org.apache.lucene.index.Term
import org.apache.lucene.search.*

@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
public class ClusterFitness extends SimpleFitness {

    private Map<Query, Integer> queryMap = [:]
    private double positiveScoreTotal = 0.0
    private double negativeScoreTotal = 0.0
    private double fraction = 0.0
    private double baseFitness = 0.0
    private double scorePlus1000 = 0.0
    private double scoreOnly = 0.0
    private double posScrMinusNegScrTimes2 = 0.0

    private int positiveHits = 0
    private int negativeHits = 0
    private int coreClusterPenalty = 0
    private int totalHits = 0
    private int missedDocs = 0
    private int zeroHitsCount = 0
    private boolean gpDummy = false
    boolean gpDummy = false
    boolean emptyQueries = false
    int duplicateCount = 0     //   int lowSubqHits = 0

    private final static int hitsPerPage = IndexInfo.indexReader.maxDoc()
    private final static int coreClusterSize = 20
    private final static IndexSearcher searcher = IndexInfo.indexSearcher;

    double getFitness() {
        return baseFitness;
    }

    void setClusterFitness(List<BooleanQuery.Builder> bqbArray) {
        assert bqbArray.size() == IndexInfo.NUMBER_OF_CLUSTERS

       // println "in fitness bqbarray size " + bqbArray.size()
        boolean gp = true

        positiveScoreTotal = 0.0
        negativeScoreTotal = 0.0
        fraction = 0.0
        baseFitness = 0.0
        scorePlus1000 = 0.0
        scoreOnly = 0.0
        posScrMinusNegScrTimes2 = 0.0
        positiveHits = 0
        negativeHits = 0
        coreClusterPenalty = 0
        totalHits = 0
        missedDocs = 0
        zeroHitsCount = 0
        duplicateCount = 0
        //    lowSubqHits = 0
        //   emptyQueries = false

        Map<Query, Integer> qMap = new HashMap<Query, Integer>()
        Set<Integer> allHits = [] as Set

        boolean gpe = false


        bqbArray.eachWithIndex { BooleanQuery.Builder bqb, index ->

            Query q = bqb?.build()
          //  println "q $q index $index"

//			if (gp && queryMap.size() != IndexInfo.NUMBER_OF_CLUSTERS) {
//				emptyQueries = true
//			}
//			//requires gp parameter to fitness
//			gpDummy = gp && (q.toString(IndexInfo.FIELD_CONTENTS).contains("DummyXX") || q == null || q.toString(IndexInfo.FIELD_CONTENTS) == ''
//					|| emptyQueries)
//
//			if (gpDummy || emptyQueries){
//				baseFitness = 0.0
//			}

            if (q == null || q.toString(IndexInfo.FIELD_CONTENTS).contains("DummyXX") || q.toString(IndexInfo.FIELD_CONTENTS) == '') {
                gpe = true
            }

            Set<Integer> otherdocIdSet = [] as Set<Integer>
            List<BooleanQuery.Builder> otherQueries = bqbArray - bqb

            BooleanQuery.Builder bqbOthers = new BooleanQuery.Builder();
            otherQueries.each { obqb ->
                bqbOthers?.add(obqb.build(), BooleanClause.Occur.SHOULD)
            }
            Query otherBQ = bqbOthers.build()

            TopDocs otherTopDocs = searcher.search(otherBQ, hitsPerPage)
            ScoreDoc[] hitsOthers = otherTopDocs.scoreDocs;
            hitsOthers.each { ScoreDoc otherHit -> otherdocIdSet << otherHit.doc }

            TopDocs docs = searcher.search(q, hitsPerPage)
            ScoreDoc[] hits = docs.scoreDocs;
             qMap.put(q, hits.size())
            if (hits.size() < 1) zeroHitsCount++

            hits.eachWithIndex { ScoreDoc d, int position ->
                allHits << d.doc

                if (otherdocIdSet.contains(d.doc)) {
                    negativeHits++;
                    negativeScoreTotal = negativeScoreTotal + d.score
                    if (position < coreClusterSize) {
                        //heavy penalty
                        //def reverseRank = coreClusterSize - position
                        //fitness.coreClusterPenalty +=reverseRank
                        coreClusterPenalty++
                    }
                } else {
                    positiveHits++
                    positiveScoreTotal = positiveScoreTotal + d.score
                }
            }

        }
//println "qmpa size " + qMap.size()


        queryMap = qMap.asImmutable()


        posScrMinusNegScrTimes2 = positiveScoreTotal - (negativeScoreTotal * 2)
        totalHits = allHits.size()
        fraction = totalHits / IndexInfo.indexReader.maxDoc()
        missedDocs = IndexInfo.indexReader.maxDoc() - allHits.size()
        scoreOnly = positiveScoreTotal - negativeScoreTotal

        //fitness must be positive for ECJ - most runs start with large negative score
        final int minScore = 1000
        scorePlus1000 = (posScrMinusNegScrTimes2 < -minScore) ? 0 : posScrMinusNegScrTimes2 + minScore

        //	scorePlus1000 = (scoreOnly < -minScore) ? 0 : scoreOnly + minScore



        int pp = 0
        if ( zeroHitsCount> 0 || qMap.size() !=3 || gpe)
        {pp =100}// else


        final int negIndicators =
                //major penalty for query returning nothing or empty query
                //	(zeroHitsCount * 100) + coreClusterPenalty + duplicateCount + lowSubqHits + 1;
                pp + coreClusterPenalty + 1;

        baseFitness =  (scorePlus1000 / negIndicators) * fraction * fraction

    }

//baseFitness =  (scorePlus1000 / negIndicators)
//baseFitness =  scorePlus1000 / (coreClusterPenalty + 1 )
//baseFitness * (1/(Math.log(missedDocs)))
//baseFitness * (1/(Math.pow(1.01,missedDocs)))
//posScrMinusNegScrTimes2;


    void generationStats(long generation) {
        println "Gereration $generation BaseFitness: ${baseFitness.round(2)} ${queryShort()}"
        println "PosHits: $positiveHits NegHits: $negativeHits PosScr: ${positiveScoreTotal.round(2)} NegScr: ${negativeScoreTotal.round(2)} PosScr-(NegScr*2): ${posScrMinusNegScrTimes2.round(2)} CoreClstPen: $coreClusterPenalty"
        println "TotalHits: $totalHits TotalDocs: ${IndexInfo.indexReader.maxDoc()} MissedDocs: $missedDocs Fraction: $fraction ZeroHits: $zeroHitsCount"
    }

@TypeChecked(TypeCheckingMode.SKIP)
    void finalQueryStats(int job, int gen, int popSize) {
        String messageOut = ""
        FileWriter resultsOut = new FileWriter("results/clusterResultsF1.txt", true)
        resultsOut << "${new Date()}  ***** Job: $job Gen: $gen PopSize: $popSize Noclusters: ${IndexInfo.NUMBER_OF_CLUSTERS}  pathToIndex: ${IndexInfo.pathToIndex}  ************************************************************* \n"

        List<Double> f1list = [], precisionList = [], recallList = []
        queryMap.keySet().eachWithIndex { q, index ->

            TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
            searcher.search(q, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;
            String qString = q.toString(IndexInfo.FIELD_CONTENTS)

            println "***********************************************************************************"
            messageOut = "ClusterQuery: $index hits: ${hits.length} Query:  $qString \n"
            println messageOut
            resultsOut << messageOut

            //map of categories (ground truth) and their frequencies
            Map<String, Integer> catsFreq = new HashMap<String, Integer>() //[:]
            hits.eachWithIndex { ScoreDoc h, int i ->
                int docId = h.doc;
                Document d = searcher.doc(docId);
                String catName = d.get(IndexInfo.FIELD_CATEGORY_NAME)
                int n = catsFreq.get((catName)) ?: 0
                catsFreq.put((catName), n + 1)
            }
            println "Gen: $gen ClusterQuery: $index catsFreq: $catsFreq for query: $qString "

            //find the category with maximimum returned docs for this query
            Map.Entry<String, Integer> catMax = catsFreq?.max { it?.value }

            println "catsFreq: $catsFreq cats max: $catMax "

            // if (catMax != 0) {
            TotalHitCountCollector totalHitCollector = new TotalHitCountCollector();
            TermQuery catQ = new TermQuery(new Term(IndexInfo.FIELD_CATEGORY_NAME,
                    catMax.key));
            searcher.search(catQ, totalHitCollector);
            int categoryTotal = totalHitCollector.getTotalHits();
            messageOut = "categoryTotal: $categoryTotal for category: $catQ \n"
            println messageOut
            resultsOut << messageOut

            double recall = catMax.value / categoryTotal;
            double precision = catMax.value / hits.size()
            double f1 = (2 * precision * recall) / (precision + recall);

            f1list << f1
            precisionList << precision
            recallList << recall
            messageOut = "f1: $f1 recall: $recall precision: $precision"
            println messageOut
            resultsOut << messageOut + "\n"
            // }
        }

        final double averageF1 = (f1list) ? (double) f1list.sum() / IndexInfo.NUMBER_OF_CLUSTERS : 0
        final double averageRecall = (recallList) ? (double) recallList.sum() / IndexInfo.NUMBER_OF_CLUSTERS : 0
        final double averagePrecision = (precisionList) ? (double) precisionList.sum() / IndexInfo.NUMBER_OF_CLUSTERS : 0
        messageOut = "***  TOTALS:   *****   f1list: $f1list averagef1: :$averageF1  ** average precision: $averagePrecision average recall: $averageRecall"
        println messageOut

        resultsOut << "TotalHits: $totalHits Total Docs:  ${IndexInfo.indexReader.maxDoc()} \n"
        resultsOut << "PosHits: $positiveHits NegHits: $negativeHits PosScore: $positiveScoreTotal NegScore: $negativeScoreTotal Fitness: ${fitness()} \n"
        resultsOut << messageOut + "\n"
        resultsOut << "************************************************ \n \n"

        resultsOut.flush()
        resultsOut.close()

        boolean appnd = job > 0
        FileWriter fcsv = new FileWriter("results/resultsCluster.csv", appnd)
        if (!appnd) {
            final String fileHead = "gen, job, popSize, baseFitness, averageF1, averagePrecision, averageRecall, query, date, indexPath" + '\n';
            fcsv << fileHead
        }
        fcsv << "$gen , $job , $popSize , $baseFitness , ${averageF1.round(2)}, ${averagePrecision.round(2)}, ${averageRecall.round(2)} , ${queryForCSV(job)}, ${new Date()}, ${IndexInfo.pathToIndex} \n"
        fcsv.flush()
        fcsv.close()
    }

    String queryShort() {
        def s = "queryMap.size ${queryMap.size()} \n"
        queryMap.keySet().eachWithIndex { Query q, int index ->
            if (index > 0) s += '\n';
            s += "ClusterQuery: $index :  ${queryMap.get(q)}  ${q.toString(IndexInfo.FIELD_CONTENTS)}"
        }
        return s
    }

    private String queryForCSV(int job) {
        def s = "Job: $job "
        queryMap.keySet().eachWithIndex { q, index ->
            s += "ClusterQuery " + index + ": " + queryMap.get(q) + " " + q.toString(IndexInfo.FIELD_CONTENTS) + " ## "
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