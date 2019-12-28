package cluster

import index.Effectiveness
import classify.LuceneClassifyMethod
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

    void reportsOut(int jobNumber, int gen, int popSize, int numberOfSubpops, int genomeSizePop0, int maxGenePop0, ClusterFitness cfit, Tuple3 t3ClassiferResult = null, LuceneClassifyMethod classifyMethod = null, boolean onlyDocsInOneCluster) {

        Tuple4 tuple4 = Effectiveness.querySetEffectiveness(cfit.queryMap.keySet())
        final double averageF1ForJob = tuple4.first
        final double averagePrecisionForJob = tuple4.second
        final double averageRecallForJoab = tuple4.third
        List<Double> f1list = tuple4.fourth

        final int numberOfClusters = cfit.queryMap.size()
        final int numberOfOriginalClasses = Indexes.index.numberOfCategories
        final int categoryCountError = numberOfOriginalClasses - numberOfClusters
        final int categoryCountErrorAbs = Math.abs(categoryCountError)

        if (t3ClassiferResult) {
            println "GA F1 $averageF1ForJob GA p $averagePrecisionForJob  GA r  $averageRecallForJoab Classifer F1 ${t3ClassiferResult.first}  Classifer p ${t3ClassiferResult.second}  Classifer r ${t3ClassiferResult.third}"

            File GAplusLucene = new File("results/GAplusLucene.csv")
            if (!GAplusLucene.exists()) {
                GAplusLucene << 'GAaveargeF1, GAaveragePrecision, GAaverageRecall, GAfitness, ClassifierF1, ClassifierP, ClassifierR, ClassifierMethod, indexName, queryType, kpenalty, onlyDocsInOneCluster, #clusters, #categories, #categoryCountError, #categoryCountErrorAbs, gen, jobNumber, date \n'
            }
            GAplusLucene << "${averageF1ForJob.round(5)}, ${averagePrecisionForJob.round(5)}, ${averageRecallForJoab.round(5)}, ${cfit.getFitness().round(5)}, ${t3ClassiferResult.first}, ${t3ClassiferResult.second}, ${t3ClassiferResult.third},  " +
                    " ${classifyMethod.name()},  ${Indexes.index.name()}, ${ClusterQueryECJ.queryType}, ${ClusterFitness.kPenalty}, $onlyDocsInOneCluster, $numberOfClusters, $numberOfOriginalClasses, $categoryCountError, $categoryCountErrorAbs, $gen, $jobNumber , ${new Date()} \n"
        }

        String messageOut = "***  TOTALS:   *****   f1list: $f1list averagef1: :$averageF1ForJob  ** average precision: $averagePrecisionForJob average recall: $averageRecallForJoab"
        println messageOut

        queryFileOut << "TotalHits: ${cfit.totalHits} Total Docs:  ${Indexes.indexReader.numDocs()} Index: ${Indexes.index}"
        queryFileOut << "hitsMatchingOnly1Query: ${cfit.hitsMatchingOnlyOneQuery} HitsMatchingTwoOrMoreQueries: ${cfit.hitsMatchingTwoOrMoreQueries}  Fitness: ${cfit.getFitness().round(5)}  \n"
        queryFileOut << messageOut + '\n'
        queryFileOut << cfit.queryShort() + '\n'
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
}
