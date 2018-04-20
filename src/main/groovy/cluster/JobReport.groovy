package cluster

import groovy.time.TimeDuration
import index.Indexes
import org.apache.lucene.document.Document
import org.apache.lucene.index.Term
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopScoreDocCollector
import org.apache.lucene.search.TotalHitCountCollector

class JobReport {
    def resultsF1 = [:]

    void overallSummary(TimeDuration duration) {

        File overallResults = new File("results/overallResultsCluster.txt")

        def categoryAverages = resultsF1.groupBy({ k, v -> k.first }).values().collectEntries { Map q -> [q.keySet()[0].first, q.values().sum() / q.values().size()] }
        println "categoryAverages: $categoryAverages"
        categoryAverages.each { print it.key + ' Average: ' + it.value.round(3) + ' ' }
        categoryAverages.each { overallResults.append(it.key + ' Average: ' + it.value.round(3) + " ") }

        double overallAverage = resultsF1.values().sum() / resultsF1.size()
        println "\nOverall Average:  ${overallAverage.round(3)} Fitness Method:  ${ClusterFitness.fitnessMethod}  ${new Date()} resultsF1: $resultsF1 \n"

        overallResults << " Duration $duration Fitness Method: ${ClusterFitness.fitnessMethod} ${new Date()}"
        overallResults << "\nOverall Average: ${overallAverage.round(3)} resultsF1: $resultsF1 \n"
    }

    void queriesReport(int job, int gen, int popSize, ClusterFitness cfit) {

        println "Queries Report qmap: ${cfit.queryMap}"
        int hitsPerPage = Indexes.indexReader.maxDoc()

        String messageOut = ""
        File jobResultsQuery = new File("results/jobResultsClusterQuery.txt")
        jobResultsQuery << "${new Date()}  ***** Job: $job Fitness Method: ${ClusterFitness.fitnessMethod}  Gen: $gen PopSize: $popSize Index: ${Indexes.indexEnum}  ************************************************************* \n"

        List<Double> f1list = [], precisionList = [], recallList = []

        cfit.queryMap.keySet().eachWithIndex { q, index ->

            TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
            Indexes.indexSearcher.search(q, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;
            String qString = q.toString(Indexes.FIELD_CONTENTS)

            println '***********************************************************************************'

            //map of categories (ground truth) and their frequencies
            Map<String, Integer> catsFreq = new HashMap<String, Integer>()
            hits.eachWithIndex { ScoreDoc h, int i ->
                int docId = h.doc;
                Document d = Indexes.indexSearcher.doc(docId);
                String catName = d.get(Indexes.FIELD_CATEGORY_NAME)
                int n = catsFreq.get((catName)) ?: 0
                catsFreq.put((catName), n + 1)
            }
            println "Gen: $gen ClusterQuery: $index catsFreq: $catsFreq for query: $qString "

            //find the category with maximimum returned docs for this query
            Map.Entry<String, Integer> catMax = catsFreq?.max { it?.value }

            println "catsFreq: $catsFreq cats max: $catMax "

            if (catMax != null) {
                TotalHitCountCollector totalHitCollector = new TotalHitCountCollector();
                TermQuery catQ = new TermQuery(new Term(Indexes.FIELD_CATEGORY_NAME,
                        catMax.key));
                Indexes.indexSearcher.search(catQ, totalHitCollector);
                int categoryTotal = totalHitCollector.getTotalHits();

                double recall = catMax.value / categoryTotal;
                double precision = catMax.value / hits.size()
                double f1 = (2 * precision * recall) / (precision + recall);

                f1list << f1
                precisionList << precision
                recallList << recall
                messageOut = "Query $index :  $qString ## f1: $f1 recall: $recall precision: $precision categoryTotal: $categoryTotal for category: $catQ"
                println messageOut
                jobResultsQuery << messageOut + "\n"
            }
        }

        double averageF1forJob = (f1list) ? (double) f1list.sum() / Indexes.NUMBER_OF_CLUSTERS : 0
        final double averageRecall = (recallList) ? (double) recallList.sum() / Indexes.NUMBER_OF_CLUSTERS : 0
        final double averagePrecision = (precisionList) ? (double) precisionList.sum() / Indexes.NUMBER_OF_CLUSTERS : 0
        messageOut = "***  TOTALS:   *****   f1list: $f1list averagef1: :$averageF1forJob  ** average precision: $averagePrecision average recall: $averageRecall"
        println messageOut

        jobResultsQuery << "TotalHits: ${cfit.totalHits} Total Docs:  ${Indexes.indexReader.maxDoc()} "
        jobResultsQuery << "PosHits: ${cfit.positiveHits} NegHits: ${cfit.negativeHits} PosScore: ${cfit.positiveScoreTotal} NegScore: ${cfit.negativeScoreTotal} Fitness: ${cfit.getFitness()} \n"
        jobResultsQuery << messageOut + "\n"
        jobResultsQuery << "************************************************ \n \n"

        File fcsv = new File("results/resultsClusterByJob.csv")
        if (!fcsv.exists()) {
            fcsv << 'aveargeF1, averagePrecision, averageRecall, fitness, indexName, fitnessMethod, date, gen, job, popSize" \n'
        }
        fcsv << "${averageF1forJob.round(2)}, ${averagePrecision.round(2)}, ${averageRecall.round(2)} , ${cfit.getFitness()}, ${Indexes.indexEnum.name()}, ${cfit.fitnessMethod}, ${new Date()},  $gen , $job , $popSize \n"

        Tuple2 indexAndJob = new Tuple2(Indexes.indexEnum.name(), job)
        resultsF1 << [(indexAndJob): averageF1forJob]
    }
}
