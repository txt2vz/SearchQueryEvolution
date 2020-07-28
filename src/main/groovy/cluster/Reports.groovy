package cluster

import classify.LuceneClassifyMethod
import index.IndexEnum
import index.Indexes
import org.apache.lucene.search.Query

class Reports {

    List<Tuple9<String, Double, Double, Integer, Integer, Double, QType, LuceneClassifyMethod, Boolean>> t9List = []

    void reports(IndexEnum ie, Tuple6<Map<Query, Integer>, Integer, Integer, Double, Double, Double> qResult, Tuple3 cResult, double fitness, QType qType, boolean setk, LuceneClassifyMethod lcm, boolean onlyDocsInOnecluster, int popSize, int numberOfSubpops, int genomeSize, int maxGene, int gen, String gaEngine, int job) {

        Map<Query, Integer> queryMap = qResult.v1
        final int uniqueHits = qResult.v2
        final int totalHits = qResult.v3
        final double qF1 = qResult.v4
        final double qP = qResult.v5
        final double qR = qResult.v6

        final double cF1 = cResult.v1
        final double cP = cResult.v2
        final double cR = cResult.v3

        final int numberOfClusters = queryMap.size();
        final int categoryCountError = ie.numberOfCategories - numberOfClusters
        final int categoryCountErrorAbs = Math.abs(categoryCountError)

        File fcsv = new File("results/results.csv")
        if (!fcsv.exists()) {
            fcsv << 'Index, QueryF1, QueryPrecision, QueryRecall, ClassifierF1,ClassifierPrecision,ClassifierRecall, UniqueHits, Fitness, QueryType, SetK, NumberofCategories, NumberOfClusters, ClusterCountError, ClassifyMethod, OnlyDocsInOneClusterForTraining, PopulationSize, NumberOfSubPops, GenomeSize, MaxGene, Gen, GA_Engine, Job, date \n'
        }
        fcsv << " ${ie.name()}, $qF1, $qP, $qR, $cF1, $cR, $cP, $uniqueHits, $fitness, $qType, $setk, $ie.numberOfCategories, $numberOfClusters, $categoryCountErrorAbs, $lcm, $onlyDocsInOnecluster, $popSize, $numberOfSubpops, $genomeSize, $maxGene, $gen, $gaEngine, $job, ${new Date()} \n"

        File queryFileOut = new File('results/Queries.txt')
        queryFileOut << "Total Docs: ${Indexes.indexReader.numDocs()} Index: ${Indexes.index} ${new Date()} \n"
        queryFileOut << "UniqueHits: ${uniqueHits}  TotalHitsAllQueries: $totalHits  QuerySetf1: $qF1 ClassifierF1: $cF1 setk: $setk CategoryCountError: $categoryCountErrorAbs  \n"
        queryFileOut << QuerySet.printQuerySet(queryMap)
        queryFileOut << "************************************************ \n \n"

        t9List << new Tuple9(ie.name(), qF1, cF1, uniqueHits, fitness, categoryCountErrorAbs, qType, lcm, setk)
    }

    void reportMaxFitness() {

        File fcsvMax = new File("results/maxFitnessReport.csv")
        if (!fcsvMax.exists()) {
            fcsvMax << 'Index, queryF1, classifierF1, uniqueHits, queryType, classifyMethod, setk,  date \n'
        }

        t9List.toUnique { it.v1 }.each { t ->
            def t9Max = t9List.findAll { t.v1 == it.v1 }.max { q -> q.v5 }
            fcsvMax << "${t9Max.v1}, ${t9Max.v2}, ${t9Max.v3}, ${t9Max.v4},${t9Max.v5},${t9Max.v6},${t9Max.v7},${t9Max.v8}, ${t9Max.v9}, ${new Date()} \n"
        }

        println "Average query f1 " + t9List.average { it.v2 } + " Classifier f1: " + t9List.average { it.v3 }
        t9List.clear();
    }
}
