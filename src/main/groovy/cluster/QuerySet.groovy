package cluster

import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.search.*

//see https://www.tutorialspoint.com/genetic_algorithms/genetic_algorithms_fundamentals.htm


enum QType {
    OR1, OR_INTERSECT
}

@CompileStatic
class QuerySet {

    static List<BooleanQuery.Builder> getQueryBuilderList(int[] intChromosome, List<TermQuery> termQueryList, final int k, QType qType) {

        switch (qType) {
            case QType.OR1: return getOneWordQueryPerCluster(intChromosome, termQueryList, k)
                break

            case QType.OR_INTERSECT: return getORIntersect(intChromosome, termQueryList, k)
                break
        }
    }

    private static List<BooleanQuery.Builder> getOneWordQueryPerCluster(int[] intChromosome, List<TermQuery> termQueryList, final int k) {

        //   Set<Integer> alleles = [] as Set<Integer>
        List<BooleanQuery.Builder> bqbL = []

        int index = 0;
        int clusterNumber = 0
        while (clusterNumber < k && index < intChromosome.size()) {

            final int allele = intChromosome[index]
            assert allele < termQueryList.size() && allele >= 0

            //   if (alleles.add(allele)) {
            bqbL[clusterNumber] = new BooleanQuery.Builder().add(termQueryList[allele], BooleanClause.Occur.SHOULD)
            clusterNumber++
            //   }
            index++
        }
        return bqbL.asImmutable()
    }

    private static List<BooleanQuery.Builder> getORIntersect(int[] intChromosome, List<TermQuery> termQueryList, final int k) {

        List<BooleanQuery.Builder> bqbL = []
        Set<Integer> alleles = [] as Set<Integer>
        int clusterNumber = 0
        int index = 0

        while (clusterNumber < k && index < intChromosome.size()) {
            final int allele = intChromosome[index]

            if (alleles.add(allele)) {
                bqbL[clusterNumber] = new BooleanQuery.Builder().add(termQueryList[allele], BooleanClause.Occur.SHOULD)
                clusterNumber++
            }
            index++
        }

        for (int i = index; i < intChromosome.size(); i++) {

            final int allele = intChromosome[i]
            clusterNumber = i % k

            BooleanQuery rootq = bqbL[clusterNumber].build()
            Query tq0 = rootq.clauses().first().getQuery()
            TermQuery tqNew = termQueryList[allele]

            if (alleles.add(allele) && (QueryTermIntersect.isValidIntersect(tq0, tqNew))) {
                bqbL[clusterNumber].add(tqNew, BooleanClause.Occur.SHOULD)
            }
        }

        assert bqbL.size() == k
        return bqbL.asImmutable()
    }

    // static Tuple3<Set<Query>, Integer, Double> querySetInfo(int[] intChromosome, List<TermQuery> termQueryList, final int k, QType queryType, boolean printQueries = false, boolean queriesToFile = false) {
    static Tuple3<Set<Query>, Integer, Double> querySetInfo(List<BooleanQuery.Builder> bqbList, boolean printQueries = false, boolean queriesToFile = false) {

        //List<BooleanQuery.Builder> bqbList = getQueryList(intChromosome, termQueryList, k, queryType)
        Tuple3<Map<Query, Integer>, Integer, Integer> t3 = UniqueHits.getUniqueHits(bqbList);

        Map<Query, Integer> queryMap = t3.v1
        final int uniqueHits = t3.v2
        final int totalHitsAllQueries = t3.v3

        Tuple4<Double, Double, Double, List<Double>> e = Effectiveness.querySetEffectiveness(queryMap.keySet());
        final f1 = e.v1

        if (printQueries) {
            println printQuerySet(queryMap);
        }

        if (queriesToFile) {

            File queryFileOut = new File('results/Queries.txt')
            queryFileOut << "TotalHits: ${t3.v3} Total Docs:  ${Indexes.indexReader.numDocs()} Index: ${Indexes.index} ${new Date()} \n"
            queryFileOut << "hitsMatchingOnly1Query: ${uniqueHits}  TotalHitsAllQueries : $totalHitsAllQueries  f1: $f1  \n"
            queryFileOut << printQuerySet(queryMap)
            queryFileOut << "************************************************ \n \n"
        }

        return new Tuple3(queryMap.keySet(), uniqueHits, f1)
    }

    static String printQuerySet(Map<Query, Integer> queryIntegerMap) {
        StringBuilder sb = new StringBuilder()
        queryIntegerMap.keySet().eachWithIndex { Query q, int index ->
            sb << "ClusterQuery: $index :  ${queryIntegerMap.get(q)}  ${q.toString(Indexes.FIELD_CONTENTS)}  \n"
        }
        return sb.toString()
    }
}