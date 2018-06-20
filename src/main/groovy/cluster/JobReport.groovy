package cluster

import groovy.time.TimeDuration
import index.Indexes
import org.apache.lucene.document.Document
import org.apache.lucene.index.Term
import org.apache.lucene.search.Query
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

        overallResults << " Duration $duration Fitness Method: ${ClusterFitness.fitnessMethod}  ${new Date()}"
        overallResults << "\nOverall Average: ${overallAverage.round(3)} resultsF1: $resultsF1 \n"
    }

    void queriesReport(int job, int gen, int popSize, int numberOfSubpops, int genomeSizePop0, int maxGenePop0, ClusterFitness cfit) {

        println "Queries Report qmap: ${cfit.queryMap}"

        String messageOut = ""
        File jobResultsQueryFileOut = new File("results/jobResultsClusterQuery.txt")
        jobResultsQueryFileOut << "${new Date()}  ***** Job: $job Fitness Method: ${ClusterFitness.fitnessMethod}  Gen: $gen PopSize: $popSize Index: ${Indexes.indexEnum}  ************************************************************* \n"

        def (ArrayList<Double> f1list, ArrayList<Double> recallList, ArrayList<Double> precisionList) = evaluateClusters(cfit.queryMap, jobResultsQueryFileOut)

        int numClusters = Math.max(Indexes.NUMBER_OF_CLUSTERS, cfit.numberOfClusters)

        double averageF1forJob = (f1list) ? (double) f1list.sum() / numClusters:0 // Indexes.NUMBER_OF_CLUSTERS : 0
        final double averageRecall = (recallList) ? (double) recallList.sum() /  numClusters:0  // Indexes.NUMBER_OF_CLUSTERS : 0
        final double averagePrecision = (precisionList) ? (double) precisionList.sum() / numClusters:0  //Indexes.NUMBER_OF_CLUSTERS : 0
        messageOut = "***  TOTALS:   *****   f1list: $f1list averagef1: :$averageF1forJob  ** average precision: $averagePrecision average recall: $averageRecall"
        println messageOut

        jobResultsQueryFileOut << "TotalHits: ${cfit.totalHits} Total Docs:  ${Indexes.indexReader.maxDoc()} "
        jobResultsQueryFileOut << "PosHits: ${cfit.positiveHits} NegHits: ${cfit.negativeHits} PosScore: ${cfit.positiveScoreTotal} NegScore: ${cfit.negativeScoreTotal} Fitness: ${cfit.getFitness().round(2)} \n"
        jobResultsQueryFileOut << messageOut + "\n"
        jobResultsQueryFileOut << "************************************************ \n \n"

        File fcsv = new File("results/resultsClusterByJob.csv")
        if (!fcsv.exists()) {
            fcsv << 'aveargeF1, averagePrecision, averageRecall, fitness, indexName, fitnessMethod, sub-populations, popSize, genomeSize, wordListSize, queryType, gen, job, date \n'
        }
	   
        fcsv << "${averageF1forJob.round(2)}, ${averagePrecision.round(2)}, ${averageRecall.round(2)}, ${cfit.getFitness().round(2)}, ${Indexes.indexEnum.name()}, ${cfit.fitnessMethod}, $numberOfSubpops, $popSize, $genomeSizePop0, $maxGenePop0,${ ClusterQueryECJ.queryType}, $gen, $job, ${new Date()} \n"

        Tuple2 indexAndJob = new Tuple2(Indexes.indexEnum.name(), job)
        resultsF1 << [(indexAndJob): averageF1forJob]
    }

    List evaluateClusters(Map queryMap, File jobResultsQuery) {
        List<Double> f1list = [], precisionList = [], recallList = []

        queryMap.keySet().eachWithIndex { Query q, index ->

            String qString = q.toString(Indexes.FIELD_CONTENTS)
            def (String maxCatName, int maxCatHits, int totalHits) = findMostFrequentCategoryForQuery(q, index)
            println "maxCatName: $maxCatName maxCatHits: $maxCatHits totalHits: $totalHits"

            if (maxCatName != 'Not_Found') {
                TotalHitCountCollector totalHitCollector = new TotalHitCountCollector();
                TermQuery catQ = new TermQuery(new Term(Indexes.FIELD_CATEGORY_NAME,
                        maxCatName));
                Indexes.indexSearcher.search(catQ, totalHitCollector);
                int categoryTotal = totalHitCollector.getTotalHits();

                double recall = (double) maxCatHits / categoryTotal;
                double precision = (double) maxCatHits / totalHits
                double f1 = (2 * precision * recall) / (precision + recall)

                f1list << f1
                precisionList << precision
                recallList << recall

                def out = "Query $index :  $qString ## f1: $f1 recall: $recall precision: $precision categoryTotal: $categoryTotal for category: $catQ"
                println out
                jobResultsQuery << out + "\n"
            }  else{
                f1list << 0
                precisionList << 0
                recallList << 0
            }
        }
        [f1list, recallList, precisionList]
    }

    List findMostFrequentCategoryForQuery(Query q, int index) {
        Map<String, Integer> catsFreq = new HashMap<String, Integer>()
        String qString = q.toString(Indexes.FIELD_CONTENTS)

        TopScoreDocCollector collector = TopScoreDocCollector.create(Indexes.indexReader.maxDoc());
        Indexes.indexSearcher.search(q, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;

        hits.eachWithIndex { ScoreDoc h, int i ->
            int docId = h.doc;
            Document d = Indexes.indexSearcher.doc(docId);
            String catName = d.get(Indexes.FIELD_CATEGORY_NAME)
            int n = catsFreq.get((catName)) ?: 0
            catsFreq.put((catName), n + 1)
        }

        Map.Entry<String, Integer> catMax = catsFreq?.max { it?.value }
        println '***********************************************************************************'
        println "ClusterQuery: $index catsFreq: $catsFreq for query: $qString "
        println "catsFreq: $catsFreq cats max: $catMax "

        String maxCategoryName = catMax?.key ?: 'Not_Found'
        int maxCategoryHits = catMax?.value ?: -1

        [maxCategoryName, maxCategoryHits, hits.size()]
    }
}
