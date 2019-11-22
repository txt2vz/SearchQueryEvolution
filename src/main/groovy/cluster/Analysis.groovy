package cluster


import index.Indexes
import org.apache.lucene.document.Document
import org.apache.lucene.index.Term
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopScoreDocCollector
import org.apache.lucene.search.TotalHitCountCollector

class Analysis {
    def resultsF1 = [:]
    def categoryAccuracy = [:]
    def resultsFitnessWithF1 = [:]
    def resultsDir = new File(/results/).mkdir()
    File queryFileOut = new File('results/Queries.txt')
    File overallResults = new File("results/overallResultsCluster.txt")

    Analysis() {
    }

    static Tuple3<String, Integer, Integer> getMostFrequentCategoryForQuery(Query q) {
        Map<String, Integer> categoryFrequencyMap = [:]
        TopScoreDocCollector collector = TopScoreDocCollector.create(Indexes.indexReader.numDocs());
        Indexes.indexSearcher.search(q, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;

        hits.each { ScoreDoc sd ->
            final int docId = sd.doc;
            Document d = Indexes.indexSearcher.doc(docId)
            String catName = d.get(Indexes.FIELD_CATEGORY_NAME)
            final int n = categoryFrequencyMap.get((catName)) ?: 0
            categoryFrequencyMap.put((catName), n + 1)
        }

        Map.Entry<String, Integer> mostFrequentCategory = categoryFrequencyMap?.max { it?.value }
        assert mostFrequentCategory

        String maxCategoryName = mostFrequentCategory?.key
        final int maxCategoryHits = mostFrequentCategory?.value
        assert maxCategoryName
        assert maxCategoryHits > 0

        println "CategoryFrequencyMap: $categoryFrequencyMap for query: ${q.toString(Indexes.FIELD_CONTENTS)} mostFrequentCategory: $mostFrequentCategory totalHist ${hits.size()} "

        return new Tuple3<String, Integer, Integer>(maxCategoryName, maxCategoryHits, hits.size())
    }

    void jobSummary() {

        def indexAverages = resultsF1.groupBy({ k, v -> k.first }).values().collectEntries { Map m -> [m.keySet()[0].first, m.values().sum() / m.values().size()] }
        indexAverages.each { print it.key + ' Average: ' + it.value.round(5) + ' ' }
        indexAverages.each { overallResults.append(it.key + ' Average: ' + it.value.round(5) + " ") }

        double overallAverage = resultsF1.values().sum() / resultsF1.size()
        println "\nOverall Averages:  ${overallAverage.round(5)} ${new Date()} resultsF1: $resultsF1 "
        println "CategoryCountError : $categoryAccuracy"

        def categoryErrorTotal = categoryAccuracy.groupBy({ k, v -> k.first }).values().collectEntries { Map m -> [m.keySet()[0].first, m.values().sum()] }
        double errorPerJob = (double) categoryErrorTotal.values().sum() / ClusterMainECJ.NUMBER_OF_JOBS

        println "Category Error $categoryErrorTotal"
        println "Catergory Error Average: $errorPerJob Category Error total: " + categoryErrorTotal.values().sum()

        overallResults << " ${new Date()}"
        overallResults << "\nOverall Average: ${overallAverage.round(5)} resultsF1: $resultsF1 \n"
        overallResults << "Catergory Error Average: $errorPerJob Category Error tototal: " + categoryErrorTotal.values().sum() + " Category Error by cateogry: $categoryErrorTotal \n"
    }

    void reportsOut(int jobNumber, int gen, int popSize, int numberOfSubpops, int genomeSizePop0, int maxGenePop0, ClusterFitness cfit) {

        def (ArrayList<Double> f1list, double averageF1forJob, double averagePrecision, double averageRecall) = calculate_F1_p_r(cfit, true)

        println "Queries Report qmap: ${cfit.queryMap}"

        int numberOfClusters = cfit.queryMap.size()
        int numberOfOriginalClasses = Indexes.indexEnum.numberOfCategories
        int categoryCountError = numberOfOriginalClasses - numberOfClusters
        int categoryCountErrorAbs = Math.abs(categoryCountError)

        queryFileOut << "${new Date()}  ***** Job: $jobNumber Query Type: ${ClusterQueryECJ.queryType}  Fitness Method: ${ClusterFitness.fitnessMethod}  Gen: $gen PopSize: $popSize Index: ${Indexes.indexEnum} Intersect Method: ${QueryListFromChromosome.intersectMethod.intersectRatio} ************************************************************* \n"

        String messageOut = "***  TOTALS:   *****   f1list: $f1list averagef1: :$averageF1forJob  ** average precision: $averagePrecision average recall: $averageRecall"
        println messageOut

        queryFileOut << "TotalHits: ${cfit.totalHits} Total Docs:  ${Indexes.indexReader.numDocs()} "
        queryFileOut << "PosHits: ${cfit.hitsMatchingOnlyOneQuery} NegHits: ${cfit.hitsMatchingTwoOrMoreQueries}  Fitness: ${cfit.getFitness().round(5)}  \n"
        queryFileOut << messageOut + "\n"
        queryFileOut << "************************************************ \n \n"

        File fcsv = new File("results/resultsClusterByJob.csv")
        if (!fcsv.exists()) {
            fcsv << 'aveargeF1, averagePrecision, averageRecall, fitness, indexName, fitnessMethod, kPenalty, sub-populations, popSize, genomeSize, wordListSize, queryType, intersectMethod, #clusters, #categories, #categoryCountError, #categoryCountErrorAbs, gen, jobNumber, date \n'
        }

        fcsv << "${averageF1forJob.round(5)}, ${averagePrecision.round(5)}, ${averageRecall.round(5)}, ${cfit.getFitness().round(5)}, ${Indexes.indexEnum.name()}, ${cfit.fitnessMethod}, ${ClusterFitness.kPenalty}, $numberOfSubpops, $popSize, $genomeSizePop0, $maxGenePop0, " +
                "${ClusterQueryECJ.queryType}, ${QueryListFromChromosome.intersectMethod.intersectRatio}, $numberOfClusters, $numberOfOriginalClasses, $categoryCountError, $categoryCountErrorAbs, $gen, $jobNumber , ${new Date()} \n"

        Tuple5 indexAndParams = new Tuple5(Indexes.indexEnum.name(), ClusterFitness.fitnessMethod, ClusterQueryECJ.queryType, QueryListFromChromosome.intersectMethod, jobNumber)
        resultsF1 << [(indexAndParams): averageF1forJob]
        categoryAccuracy << [(indexAndParams): categoryCountErrorAbs]

        resultsFitnessWithF1 << [(indexAndParams): new Tuple2<Double, Double>(cfit.baseFitness, averageF1forJob)]
    }

    List calculate_F1_p_r(ClusterFitness cfit, boolean queryReport) {
        List<Double> f1list = [], precisionList = [], recallList = [], fitnessList = []

        cfit.queryMap.keySet().eachWithIndex { Query q, index ->

            String qString = q.toString(Indexes.FIELD_CONTENTS)

            def tuple3 = getMostFrequentCategoryForQuery(q)
            String maxCatName = tuple3.first
            final int maxCatHits = tuple3.second
            final int totalHits = tuple3.third

            assert maxCatHits
            assert totalHits

            TotalHitCountCollector totalHitCollector = new TotalHitCountCollector();
            TermQuery catQ = new TermQuery(new Term(Indexes.FIELD_CATEGORY_NAME,
                    maxCatName));
            Indexes.indexSearcher.search(catQ, totalHitCollector);
            final int categoryTotal = totalHitCollector.getTotalHits();

            assert categoryTotal

            final double recall = (double) maxCatHits / categoryTotal;
            final double precision = (double) maxCatHits / totalHits
            final double f1 = (2 * precision * recall) / (precision + recall)

            assert f1
            assert f1 > 0

            f1list << f1
            precisionList << precision
            recallList << recall

            if (queryReport) {

                String out = "Query $index :  $qString ## f1: $f1 recall: $recall precision: $precision categoryTotal: $categoryTotal for category: $catQ hitsInCategory: $maxCatHits "
                println out
                queryFileOut << out + "\n"
            }
        }

        final int numClusters = Math.max(Indexes.NUMBER_OF_CLUSTERS, cfit.queryMap.size())
        final double averageF1forJob = (f1list) ? (double) f1list.sum() / numClusters : 0
        final double averageRecall = (recallList) ? (double) recallList.sum() / numClusters : 0
        final double averagePrecision = (precisionList) ? (double) precisionList.sum() / numClusters : 0

        assert averageF1forJob
        assert averageF1forJob > 0

        [f1list, averageF1forJob, averagePrecision, averageRecall]
    }
}
