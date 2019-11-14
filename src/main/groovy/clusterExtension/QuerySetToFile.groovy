package clusterExtension

import cluster.ClusterFitness
import index.Indexes
import org.apache.lucene.search.Query

class QuerySetToFile {

    static void main(String[] args) {
    }

    static void qToF(ClusterFitness cfit) {
        File queryData = new File('results/qdata.txt')
        queryData.text = ''

        cfit.queryMap.keySet().each { Query q ->
         //   String qString = q.toString(Indexes.FIELD_CONTENTS)
            queryData << q.toString(Indexes.FIELD_CONTENTS) + '\n'
        }
    }
}
