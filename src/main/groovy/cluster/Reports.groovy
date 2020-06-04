package cluster

import classify.LuceneClassifyMethod
import index.IndexEnum
import org.apache.lucene.search.Query

class Reports {

    List<Tuple7<IndexEnum, Double, Double, Integer, QType, LuceneClassifyMethod, Boolean>> t7List = []

    void reportCSV(IndexEnum ie, Tuple5 <Set<Query>, Integer, Double, Double, Double> qResult, Tuple3 cResult, QType qType, boolean setk, LuceneClassifyMethod lcm, boolean onlyDocsInOnecluster, int popSize, int genomeSize, int maxGene, int gen, String gaEngine, int job) {

        t7List << new Tuple7(ie, qResult.v3, cResult.v1, qResult.v4, qType, lcm, setk)

        final int numberOfClusters =   qResult.v1.size()
        final int categoryCountError = ie.numberOfCategories - numberOfClusters
        final int categoryCountErrorAbs = Math.abs(categoryCountError)

        File fcsv = new File("results/results.csv")
        if (!fcsv.exists()) {
            fcsv << 'Index, QueryF1, QueryPrecision, QueryRecall, ClassifierF1,ClassifierPrecision,ClassifierRecall, UniqueHits, QueryType, SetK, ClusterCountError, ClassifyMethod, OnlyDocsInOneClusterForTraining, PopulationSize, GenomeSize, MaxGene, Gen, GA_Engine, Job, date \n'
        }

        fcsv << " ${ie.name()}, ${qResult.v3}, ${qResult.v4}, ${qResult.v5}, ${cResult.v1},${cResult.v2},${cResult.v3}, ${qResult.v2}, $qType, $setk, $categoryCountErrorAbs, $lcm, $onlyDocsInOnecluster, $popSize, $genomeSize, $maxGene, $gen, $gaEngine, $job, ${new Date()} \n"
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
