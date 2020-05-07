package cluster

import classify.ClassifyUnassigned
import classify.LuceneClassifyMethod
import classify.UpdateAssignedFieldInIndex
import groovy.transform.CompileStatic
import index.IndexEnum
import index.Indexes
import org.apache.lucene.classification.Classifier
import org.apache.lucene.index.IndexReader
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery


@CompileStatic
class JeneticsHelper {

    static Tuple3<Set<Query>,Integer, Double> getDataForReporting(IndexReader ir, int[] intChromosome, List<TermQuery> termQueryList, final int k, boolean printQueries = false ){
        List <BooleanQuery.Builder> bqbList =  QueryListFromChromosomeJ.getOneWordQueryPerCluster(ir, intChromosome,termQueryList, k)   ;
        Tuple3<Map<Query, Integer>, Integer, Integer> t3 = ClusterFitnessJ.getUniqueHits(bqbList) ;

        Map<Query, Integer> queryMap =  t3.getV1();
        final int uniqueHits = t3.getV2();

        if (printQueries) {
            JeneticsHelper.printQueries(queryMap);
        }
        Tuple4<Double, Double, Double, List<Double>> e = Effectiveness.querySetEffectiveness(queryMap.keySet());
        final f1 = e.v1

        return new Tuple3 (queryMap.keySet(), uniqueHits, f1)
    }

//    static Set<Query> getQueryList(IndexReader ir, int[] intChromosome, List<TermQuery> termQueryList, final int k, boolean getF1 = false){
//
//       List <BooleanQuery.Builder> bqbList =  QueryListFromChromosomeJ.getOneWordQueryPerCluster(ir,intChromosome,termQueryList, k)   ;
//
//        Tuple3<Map<Query, Integer>, Integer, Integer> t3 = ClusterFitnessJ.getUniqueHits(bqbList)//   Collections.unmodifiableList(Arrays.asList(bqbList)))
//        Map<Query, Integer> queryMap =  t3.v1
//        int uniqueHits = t3.v2;
//        printQueries(queryMap)
//
//
//        println "fitness: $uniqueHits"
//
//
//        if (getF1) {
//            Tuple4<Double, Double, Double, List<Double>> e = Effectiveness.querySetEffectiveness(queryMap.keySet())
//
//            println "e f1 " + e.v1
//        }
//        return queryMap.keySet().asImmutable()
//    }

    static void printQueries(Map<Query, Integer> queryIntegerMap){
        StringBuilder sb = new StringBuilder()
        queryIntegerMap.keySet().eachWithIndex { Query q, int index ->
            sb << "ClusterQuery: $index :  ${queryIntegerMap.get(q)}  ${q.toString(Indexes.FIELD_CONTENTS)} \n"
        }
        println sb
    }

    static Tuple3<Double, Double, Double>  classify (IndexEnum ie, Set<Query> querySet, final int k){

        UpdateAssignedFieldInIndex.updateAssignedField(ie,  querySet )

        Classifier classifier = ClassifyUnassigned.getClassifierForUnassignedDocuments(ie, LuceneClassifyMethod.KNN)
        Tuple3<Double, Double, Double>  eff = Effectiveness.classifierEffectiveness(classifier, ie, k)
        return  eff

    }
}
