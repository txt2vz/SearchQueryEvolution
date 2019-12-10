package cluster


import index.Effectiveness
import clusterExtension.LuceneClassifyMethod
import index.Indexes

class Analysis {
    def resultsF1 = [:]
    def categoryAccuracy = [:]
    def resultsFitnessWithF1 = [:]
    // def resultsDir = new File(/results/).mkdir()

    File queryFileOut = new File('results/Queries.txt')
    File overallResults = new File("results/overallResultsCluster.txt")

    Analysis() {
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

    void reportsOut(int jobNumber, int gen, int popSize, int numberOfSubpops, int genomeSizePop0, int maxGenePop0, ClusterFitness cfit, Tuple3 t3ClassiferResult = null, LuceneClassifyMethod classifyMethod = null) {

      //  Tuple4 t4 = calculate_F1_p_r(cfit, true)
        Tuple4 tuple4 = Effectiveness.querySetEffectiveness(cfit.queryMap.keySet())
        final double averageF1ForJob = tuple4.first
        final double averagePrecisionForJob = tuple4.second
        final double averageRecallForJoab = tuple4.third
        List<Double> f1list = tuple4.fourth

     //   println "Queries Report qmap: ${cfit.queryMap}"

        final int numberOfClusters = cfit.queryMap.size()
        final int numberOfOriginalClasses = Indexes.index.numberOfCategories
        final int categoryCountError = numberOfOriginalClasses - numberOfClusters
        final int categoryCountErrorAbs = Math.abs(categoryCountError)

        if (t3ClassiferResult) {
            println "GA F1 $averageF1ForJob GA p $averagePrecisionForJob  GA r  $averageRecallForJoab Classifer F1 ${t3ClassiferResult.first}  Classifer p ${t3ClassiferResult.second}  Classifer r ${t3ClassiferResult.third}"

            File GAplusLucene = new File("results/GAplusLucene.csv")
            if (!GAplusLucene.exists()) {
                GAplusLucene << 'GAaveargeF1, GAaveragePrecision, GAaverageRecall, GAfitness, ClassifierF1, ClassifierP, ClassifierR, ClassifierMethod, indexName, queryType, #clusters, #categories, #categoryCountError, #categoryCountErrorAbs, gen, jobNumber, date \n'
            }
            GAplusLucene << "${averageF1ForJob.round(5)}, ${averagePrecisionForJob.round(5)}, ${averageRecallForJoab.round(5)}, ${cfit.getFitness().round(5)}, ${t3ClassiferResult.first}, ${t3ClassiferResult.second}, ${t3ClassiferResult.third},  " +
                    " ${classifyMethod.name()},  ${Indexes.index.name()}, ${ClusterQueryECJ.queryType},  $numberOfClusters, $numberOfOriginalClasses, $categoryCountError, $categoryCountErrorAbs, $gen, $jobNumber , ${new Date()} \n"
        }

        String messageOut = "***  TOTALS:   *****   f1list: $f1list averagef1: :$averageF1ForJob  ** average precision: $averagePrecisionForJob average recall: $averageRecallForJoab"
        println messageOut

        queryFileOut << "TotalHits: ${cfit.totalHits} Total Docs:  ${Indexes.indexReader.numDocs()} "
        queryFileOut << "PosHits: ${cfit.hitsMatchingOnlyOneQuery} NegHits: ${cfit.hitsMatchingTwoOrMoreQueries}  Fitness: ${cfit.getFitness().round(5)}  \n"
        queryFileOut << messageOut + "\n"
        queryFileOut << "************************************************ \n \n"

        File fcsv = new File("results/resultsClusterByJob.csv")
        if (!fcsv.exists()) {
            fcsv << 'aveargeF1, averagePrecision, averageRecall, fitness, indexName, fitnessMethod, kPenalty, sub-populations, popSize, genomeSize, wordListSize, queryType, intersectMethod, #clusters, #categories, #categoryCountError, #categoryCountErrorAbs, gen, jobNumber, date \n'
        }

        fcsv << "${averageF1ForJob.round(5)}, ${averagePrecisionForJob.round(5)}, ${averageRecallForJoab.round(5)}, ${cfit.getFitness().round(5)}, ${Indexes.index.name()}, ${cfit.fitnessMethod}, ${ClusterFitness.kPenalty}, $numberOfSubpops, $popSize, $genomeSizePop0, $maxGenePop0, " +
                "${ClusterQueryECJ.queryType}, ${QueryListFromChromosome.intersectMethod.intersectRatio}, $numberOfClusters, $numberOfOriginalClasses, $categoryCountError, $categoryCountErrorAbs, $gen, $jobNumber , ${new Date()} \n"

        Tuple5 indexAndParams = new Tuple5(Indexes.index.name(), ClusterFitness.fitnessMethod, ClusterQueryECJ.queryType, QueryListFromChromosome.intersectMethod, jobNumber)
        resultsF1 << [(indexAndParams): averageF1ForJob]
        categoryAccuracy << [(indexAndParams): categoryCountErrorAbs]

        resultsFitnessWithF1 << [(indexAndParams): new Tuple2<Double, Double>(cfit.baseFitness, averageF1ForJob)]
    }

//    static Tuple4 calculate_F1_p_r(ClusterFitness cfit, boolean queryReport) {
//
//        List<Double> f1list = [], precisionList = [], recallList = [], fitnessList = []
//
//        cfit.queryMap.keySet().eachWithIndex { Query q, index ->
//
//            String qString = q.toString(Indexes.FIELD_CONTENTS)
//
//            def tuple3 = IndexUtils.getMostFrequentCategoryForQuery(q)
//            String maxCatName = tuple3.first
//            final int maxCatHits = tuple3.second
//            final int totalHits = tuple3.third
//
//            double recall = 0
//            double precision = 0
//            double f1 = 0
//            int categoryTotal = 0
//            TermQuery catQ = new TermQuery(new Term(Indexes.FIELD_CATEGORY_NAME,
//                    maxCatName));
//
//            if (maxCatHits && totalHits && catQ) {
//                TotalHitCountCollector totalHitCollector = new TotalHitCountCollector()
//                Indexes.indexSearcher.search(catQ, totalHitCollector);
//                categoryTotal = totalHitCollector.getTotalHits()
//
//                assert categoryTotal
//
//                recall = (double) maxCatHits / categoryTotal
//                precision = (double) maxCatHits / totalHits
//                f1 = (2 * precision * recall) / (precision + recall)
//            }
//
//            f1list << f1
//            precisionList << precision
//            recallList << recall
//
////            if (queryReport) {
////                String out = "Query $index :  $qString ## f1: $f1 recall: $recall precision: $precision categoryTotal: $categoryTotal for category: $catQ hitsInCategory: $maxCatHits "
////                println out
////                queryFileOut << out + "\n"
////            }
//        }
//
//        final int numClusters = Math.max(Indexes.NUMBER_OF_CLUSTERS, cfit.queryMap.size())
//        final double averageF1ForJob = (f1list) ? (double) f1list.sum() / numClusters : 0
//        final double averageRecallForJob = (recallList) ? (double) recallList.sum() / numClusters : 0
//        final double averagePrecisionForJob = (precisionList) ? (double) precisionList.sum() / numClusters : 0
//
//        assert averageF1ForJob
//        assert averageF1ForJob > 0
//
//        return new Tuple4<Double, Double, Double, List<Double>>(averageF1ForJob, averagePrecisionForJob, averageRecallForJob, f1list)
//    }
}
