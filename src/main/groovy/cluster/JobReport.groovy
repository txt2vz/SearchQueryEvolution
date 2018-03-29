package cluster

import index.IndexInfo
import org.apache.lucene.document.Document
import org.apache.lucene.index.Term
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopScoreDocCollector
import org.apache.lucene.search.TotalHitCountCollector

class JobReport {
    def finalReportF1List =[]
    double averageF1forJob

    void updateF1list(){
        finalReportF1List<< averageF1forJob
    }

    private writeOverallToFile(){
        boolean appnd = true
        FileWriter fw = new FileWriter("results/overallResultsCluster.txt", appnd)

        double overallAverage = finalReportF1List.sum()/finalReportF1List.size()
        println "Overal Avearge:  ${overallAverage.round(3)}"
        fw << "Overall Average: ${overallAverage.round(3)}  ${new Date()} F1 List: $finalReportF1List \n"
        fw.flush()
        fw.close()
    }

    // @TypeChecked(TypeCheckingMode.SKIP)
    void queriesReport(int job, int gen, int popSize, ClusterFitness cfit)  {
        println "Queries Report qmap: ${cfit.queryMap}"

        int hitsPerPage = IndexInfo.indexReader.maxDoc()

        String messageOut = ""
        FileWriter resultsOut = new FileWriter("results/clusterResultsF1.txt", true)
        resultsOut << "${new Date()}  ***** Job: $job Gen: $gen PopSize: $popSize Index: ${IndexInfo.indexEnum}  ************************************************************* \n"

        List<Double> f1list = [], precisionList = [], recallList = []
        cfit.queryMap.keySet().eachWithIndex { q, index ->

            TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
            IndexInfo.indexSearcher.search(q, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;
            String qString = q.toString(IndexInfo.FIELD_CONTENTS)

            println "***********************************************************************************"

            //map of categories (ground truth) and their frequencies
            Map<String, Integer> catsFreq = new HashMap<String, Integer>() //[:]
            hits.eachWithIndex { ScoreDoc h, int i ->
                int docId = h.doc;
                Document d = IndexInfo.indexSearcher.doc(docId);
                String catName = d.get(IndexInfo.FIELD_CATEGORY_NAME)
                int n = catsFreq.get((catName)) ?: 0
                catsFreq.put((catName), n + 1)
            }
            println "Gen: $gen ClusterQuery: $index catsFreq: $catsFreq for query: $qString "

            //find the category with maximimum returned docs for this query
            Map.Entry<String, Integer> catMax = catsFreq?.max { it?.value }

            println "catsFreq: $catsFreq cats max: $catMax "

            if (catMax != null) {
                TotalHitCountCollector totalHitCollector = new TotalHitCountCollector();
                TermQuery catQ = new TermQuery(new Term(IndexInfo.FIELD_CATEGORY_NAME,
                        catMax.key));
                IndexInfo.indexSearcher.search(catQ, totalHitCollector);
                int categoryTotal = totalHitCollector.getTotalHits();

                double recall = catMax.value / categoryTotal;
                double precision = catMax.value / hits.size()
                double f1 = (2 * precision * recall) / (precision + recall);

                f1list << f1
                precisionList << precision
                recallList << recall
                messageOut = "Query $index :  $qString ## f1: $f1 recall: $recall precision: $precision categoryTotal: $categoryTotal for category: $catQ"
                println messageOut
                resultsOut << messageOut + "\n"
            }
        }

        averageF1forJob = (f1list) ? (double) f1list.sum() / IndexInfo.NUMBER_OF_CLUSTERS : 0
        final double averageRecall = (recallList) ? (double) recallList.sum() / IndexInfo.NUMBER_OF_CLUSTERS : 0
        final double averagePrecision = (precisionList) ? (double) precisionList.sum() / IndexInfo.NUMBER_OF_CLUSTERS : 0
        messageOut = "***  TOTALS:   *****   f1list: $f1list averagef1: :$averageF1forJob  ** average precision: $averagePrecision average recall: $averageRecall"
        println messageOut

        resultsOut << "TotalHits: ${cfit.totalHits} Total Docs:  ${IndexInfo.indexReader.maxDoc()} "
        resultsOut << "PosHits: ${cfit.positiveHits} NegHits: ${cfit.negativeHits} PosScore: ${cfit.positiveScoreTotal} NegScore: ${cfit.negativeScoreTotal} Fitness: ${cfit.getFitness()} \n"
        resultsOut << messageOut + "\n"
        resultsOut << "************************************************ \n \n"

        resultsOut.flush()
        resultsOut.close()

        boolean appnd = true //job > 0
        FileWriter fcsv = new FileWriter("results/resultsCluster.csv", appnd)
        if (!appnd) {
            final String fileHead = "gen, job, popSize, baseFitness, aveargeF1, averagePrecision, averageRecall, query, date, indexPath" + '\n';
            fcsv << fileHead
        }
        fcsv << "$gen , $job , $popSize , ${cfit.getFitness()} , ${averageF1forJob.round(2)}, ${averagePrecision.round(2)}, ${averageRecall.round(2)} , ${cfit.queryShort()}, ${new Date()}, ${IndexInfo.indexEnum.getPathString()} \n"
        fcsv.flush()
        fcsv.close()

        updateF1list()
    }
}
