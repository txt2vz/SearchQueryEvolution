package cluster

import com.sun.javafx.UnmodifiableArrayList
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import index.Indexes
import org.apache.lucene.index.IndexReader
import org.apache.lucene.search.*
import org.apache.lucene.search.spans.SpanFirstQuery
import org.apache.lucene.search.spans.SpanTermQuery

@CompileStatic
enum IntersectMethodJ {

    NONE(0.0d),
    RATIO_POINT_1(0.1d),
    RATIO_POINT_2(0.2d),
    RATIO_POINT_3(0.3d),
    RATIO_POINT_4(0.4d),
    RATIO_POINT_5(0.5d),
    RATIO_POINT_6(0.6d),
    RATIO_POINT_7(0.7d),
    RATIO_POINT_8(0.8d),
    RATIO_POINT_9(0.9d)

    IntersectMethodJ(double minVal) {
        intersectRatio = minVal
    }
    double intersectRatio
}

//see https://www.tutorialspoint.com/genetic_algorithms/genetic_algorithms_fundamentals.htm

@CompileStatic
class QueryListFromChromosomeJ {

    static List<BooleanQuery.Builder>getOneWordQueryPerClusterSetK(IndexReader ir, int[] intChromosome, List<TermQuery> termQueryList) {
        final int k = intChromosome[0]
        int[] intChromeN = intChromosome[1..intChromosome.length]
        return getOneWordQueryPerCluster(ir, intChromeN, termQueryList, k)
    }

    static List<BooleanQuery.Builder> getOneWordQueryPerCluster(IndexReader ir, int[] intChromosome, List<TermQuery> termQueryList, final int k) {

        Set<Integer> alleles = [] as Set<Integer>
        List<BooleanQuery.Builder> bqbL = []//new BooleanQuery.Builder[k]

        int index = 0;
        int clusterNumber = 0
        while (clusterNumber < k && index < intChromosome.size()) {

            final int allele = intChromosome[index]
            assert allele < termQueryList.size() && allele >= 0

            if (alleles.add(allele)) {
                bqbL[clusterNumber] = new BooleanQuery.Builder().add(termQueryList[allele], BooleanClause.Occur.SHOULD)
                clusterNumber++
            }
            index++
        }
        return bqbL.asImmutable() //as UnmodifiableArrayList <BooleanQuery.Builder>
        // return new Tuple4(bqbL, k, index, alleles)
    }
}