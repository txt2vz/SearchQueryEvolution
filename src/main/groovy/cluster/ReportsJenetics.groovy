package cluster

import classify.LuceneClassifyMethod
import groovy.lang.MetaClassImpl.Index
import index.IndexEnum
import index.Indexes
import org.apache.lucene.classification.Classifier
import org.apache.lucene.search.Query

class ReportsJenetics {

    static void reportCSV (int jobNumber, int gen, int popSize, int genomeSize, int maxGeneValue, int uniqueHits, Set<Query>querySet,  IndexEnum ie ,LuceneClassifyMethod lcm, double queryF1, double classifierF1 ) {

        Date date = new Date();
        File fcsv = new File("results/resultsClusterByJobJenetics.csv")
        if (!fcsv.exists()) {
            fcsv << 'Index, queryF1, classifierF1, uniqueHits, popSize, genomeSize, wordListSize, gen, classifyMethod, date \n'
        }

        fcsv << "${ie.name()}, $queryF1, $classifierF1, $uniqueHits $popSize, $genomeSize, $maxGeneValue, $gen, $lcm, $date \n"
    }
}
