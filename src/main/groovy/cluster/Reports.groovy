package cluster

import classify.LuceneClassifyMethod
import index.IndexEnum
import index.Indexes
import org.apache.lucene.search.Query

class Reports {

    List<Tuple7<IndexEnum, Double, Double, Integer, QType, LuceneClassifyMethod, Boolean>> t7List = []

    void reportCSV(IndexEnum ie, Tuple6 <Map<Query, Integer>, Integer, Integer, Double, Double, Double> qResult, Tuple3 cResult, QType qType, boolean setk, LuceneClassifyMethod lcm, boolean onlyDocsInOnecluster, int popSize, int genomeSize, int maxGene, int gen, String gaEngine, int job) {

        Map<Query, Integer> queryMap = qResult.v1
        final int uniqueHits  = qResult.v2
        final int totalHits = qResult.v3
        final double qF1 = qResult.v4
        final double qP = qResult.v5
        final double qR = qResult.v6

        final double cF1 = cResult.v1
        final double cP = cResult.v2
        final double cR = cResult.v3

        final int categoryCountError = ie.numberOfCategories - queryMap.size()
        final int categoryCountErrorAbs = Math.abs(categoryCountError)

        File fcsv = new File("results/results.csv")
        if (!fcsv.exists()) {
            fcsv << 'Index, QueryF1, QueryPrecision, QueryRecall, ClassifierF1,ClassifierPrecision,ClassifierRecall, UniqueHits, QueryType, SetK, ClusterCountError, ClassifyMethod, OnlyDocsInOneClusterForTraining, PopulationSize, GenomeSize, MaxGene, Gen, GA_Engine, Job, date \n'
        }
        fcsv << " ${ie.name()}, $qF1, $qP, $qR, $cF1, $cR, $cP, $uniqueHits, $qType, $setk, $categoryCountErrorAbs, $lcm, $onlyDocsInOnecluster, $popSize, $genomeSize, $maxGene, $gen, $gaEngine, $job, ${new Date()} \n"

        File queryFileOut = new File('results/Queries.txt')
        queryFileOut << "Total Docs: ${Indexes.indexReader.numDocs()} Index: ${Indexes.index} ${new Date()} \n"
        queryFileOut << "UniqueHits: ${uniqueHits}  TotalHitsAllQueries: $totalHits  QuerySetf1: $qF1 ClassifierF1: $cF1 setk: $setk CategoryCountError: $categoryCountErrorAbs  \n"
        queryFileOut << QuerySet.printQuerySet(queryMap)
        queryFileOut << "************************************************ \n \n"

        t7List << new Tuple7(ie, qF1, cF1, uniqueHits, qType, lcm, setk)
    }

    void reportMaxFitness() {

        File fcsvMax = new File("results/maxFitnessReport.csv")
        if (!fcsvMax.exists()) {
            fcsvMax << 'Index, queryF1, classifierF1, uniqueHits, queryType, classifyMethod, setk,  date \n'
        }

        Date date = new Date();
        t7List.toUnique { it.v1 }.each { t ->
            def t7Max = t7List.findAll { t.v1 == it.v1 }.max { q -> q.v4 }

            fcsvMax << "${t7Max.v1.name()}, ${t7Max.v2}, ${t7Max.v3}, ${t7Max.v4},${t7Max.v5},${t7Max.v6},${t7Max.v7}, $date \n"
        }
    }
}
