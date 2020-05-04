package cluster

import classify.ClassifyUnassigned
import classify.LuceneClassifyMethod
import classify.UpdateAssignedFieldInIndex
import index.IndexEnum
import index.Indexes
import io.jenetics.IntegerChromosome
import org.apache.lucene.classification.Classifier
import org.apache.lucene.index.IndexReader
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery

class JeneticsHelper {

   static Map<Query, Integer> getQueries(IndexReader ir, List <BooleanQuery.Builder> bqbList){
      def t3 = ClusterFitnessJ.getUniqueHits(bqbList)//   Collections.unmodifiableList(Arrays.asList(bqbList)))
      def queryMap =  t3.v1
      return  queryMap
  }

    static List<Query> getBest(IndexReader ir, int[] intChromosome, List<TermQuery> termQueryList, final int k, boolean getF1 = false){

       List <BooleanQuery.Builder> bqbList =  QueryListFromChromosomeJ.getOneWordQueryPerCluster(ir,intChromosome,termQueryList, k)   ;

        def t3 = ClusterFitnessJ.getUniqueHits(bqbList)//   Collections.unmodifiableList(Arrays.asList(bqbList)))
        def queryMap =  t3.v1
        def f = t3.v2;

        StringBuilder sb = new StringBuilder()
        queryMap.keySet().eachWithIndex { Query q, int index ->
            sb << "ClusterQuery: $index :  ${queryMap.get(q)}  ${q.toString(Indexes.FIELD_CONTENTS)} \n"
        }

        println "fitness: $f"
        println sb

        if (getF1) {
            def e = Effectiveness.querySetEffectiveness(queryMap.keySet())

            println "e f1 " + e.v1
        }
        return queryMap.keySet().toList()
    }

    static void classify (IndexEnum ie, List<Query> queryList, k){

        UpdateAssignedFieldInIndex.updateAssignedField(ie,  queryList )

        Classifier classifier = ClassifyUnassigned.classifyUnassigned(ie, LuceneClassifyMethod.KNN)
        Effectiveness.classifierEffectiveness(classifier, ie, k)

    }
}
