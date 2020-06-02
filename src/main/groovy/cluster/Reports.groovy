package cluster

import classify.LuceneClassifyMethod
import index.IndexEnum

class Reports {

     List < Tuple7<IndexEnum,Double,Double, Integer, QType, LuceneClassifyMethod, Boolean>> t7List = []

     void reportCSV (int jobNumber, IndexEnum ie, double queryF1, double classifierF1, int uniqueHits, QType qType, LuceneClassifyMethod lcm, int popSize, int genomeSize, int maxGene, int gen, boolean setk, boolean onlyDocsInOneQueryForClassification ) {

        t7List << new Tuple7(ie,queryF1,classifierF1,uniqueHits,qType,lcm, setk)
        Date date = new Date();
        File fcsv = new File("results/results.csv")
        if (!fcsv.exists()) {
            fcsv << 'Job, Index, queryF1, classifierF1, uniqueHits, queryType, classifyMethod, setk, onlyDocsInOneQueryForClassification, popSize, genomeSize, maxGene, gen, date \n'
        }

        fcsv << "$jobNumber, ${ie.name()}, $queryF1, $classifierF1, $uniqueHits, $qType, $lcm, $setk, $onlyDocsInOneQueryForClassification, $popSize, $genomeSize, $maxGene, $gen, $date \n"
    }

     void reportMaxFitness(){

        File fcsvMax = new File("results/t7max.csv")
        if (!fcsvMax.exists()) {
            fcsvMax << 'Index, queryF1, classifierF1, uniqueHits, queryType, classifyMethod, setk,  date \n'
        }

        Date date = new Date();
        t7List.toUnique{it.v1}.each {t ->
            def t7Max = t7List.findAll{t.v1 == it.v1}.max { q -> q.v4 }

            fcsvMax << "${t7Max.v1.name()}, ${t7Max.v2}, ${t7Max.v3}, ${t7Max.v4},${t7Max.v5},${t7Max.v6},${t7Max.v7}, $date \n"
        }
    }
}
