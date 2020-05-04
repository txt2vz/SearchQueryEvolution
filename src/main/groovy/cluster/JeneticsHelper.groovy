package cluster

import io.jenetics.IntegerChromosome
import org.apache.lucene.search.BooleanQuery

class JeneticsHelper {

    static double getF(QueryListFromChromosome qlc, def a, boolean end = false) {

        // BooleanQuery.Builder[] bqbArray = qlc.getSimple(((IntegerChromosome) gt.get(0)).toArray());
        BooleanQuery.Builder[] bqbArray = qlc.getSimple(a);
        //QueryListFromChromosome
        // .getOR_List(((IntegerChromosome) gt.getChromosome(0)).toArray(), termQueryArray, Indexes.NUMBER_OF_CLUSTERS, BooleanClause.Occur.SHOULD, 1);

        ClusterFitness clusterFitness = new ClusterFitness();
        clusterFitness.setClusterFitness(Collections.unmodifiableList(Arrays.asList(bqbArray)));
        System.out.println(clusterFitness.queryShort());
        if (end) {
            println "DD" + clusterFitness.queryShort()
            println "FF " + clusterFitness.fitness
        }

        return clusterFitness.fitness
    }
}
