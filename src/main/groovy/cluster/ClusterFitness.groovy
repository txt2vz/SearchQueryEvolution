package cluster

import ec.simple.SimpleFitness
import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TotalHitCountCollector

@CompileStatic
enum FitnessMethod {
    UNIQUE_HITS_COUNT, UNIQUE_HITS_K_PENALTY
}

@CompileStatic
public class ClusterFitness extends SimpleFitness {

    static FitnessMethod fitnessMethod
    static double kPenalty = 0.04d

    Map<Query, Integer> queryMap = [:]
    double baseFitness = 0.0  //for ECJ

    int hitsMatchingOnlyOneQuery = 0
    int hitsMatchingTwoOrMoreQueries = 0
    int totalHits = 0
    int missedDocs = 0
    int k

    double getFitness() {
        return baseFitness;
    }

    void setClusterFitness(List<BooleanQuery.Builder> bqbList) {

        k = bqbList.size()
        baseFitness = 0.0

        Tuple3<Map<Query, Integer>, Integer, Integer> t3 = getUniqueHits(bqbList)
        queryMap = t3.first.asImmutable()
        hitsMatchingOnlyOneQuery = t3.second
        totalHits = t3.third

        hitsMatchingTwoOrMoreQueries = totalHits - hitsMatchingOnlyOneQuery
        missedDocs = Indexes.indexReader.numDocs() - totalHits

        switch (fitnessMethod) {

            case fitnessMethod.UNIQUE_HITS_COUNT:
                baseFitness = hitsMatchingOnlyOneQuery
                break

            case fitnessMethod.UNIQUE_HITS_K_PENALTY:

                double f = hitsMatchingOnlyOneQuery * (1.0 - (kPenalty * k))
                baseFitness = (f > 0) ? f : 0.0d
                //      baseFitness = hitsMatchingOnlyOneQuery * Math.pow(0.97d, (double)(k)) //> 0 ? uniqueWithPenalty : 0
                break
        }
    }

    private Tuple3<Map<Query, Integer>, Integer, Integer> getUniqueHits(List<BooleanQuery.Builder> bqbList) {
        Map<Query, Integer> qMap = new HashMap<Query, Integer>()
        BooleanQuery.Builder totalHitsBQB = new BooleanQuery.Builder()

        int totalUniqueHits = 0
        for (int i = 0; i < bqbList.size(); i++) {
            Query q = bqbList[i].build()
            totalHitsBQB.add(q, BooleanClause.Occur.SHOULD)

            BooleanQuery.Builder bqbOneCategoryOnly = new BooleanQuery.Builder()
            bqbOneCategoryOnly.add(q, BooleanClause.Occur.SHOULD)

            for (int j = 0; j < bqbList.size(); j++) {
                if (j != i) {
                    bqbOneCategoryOnly.add(bqbList[j].build(), BooleanClause.Occur.MUST_NOT)
                }
            }

            TotalHitCountCollector collector = new TotalHitCountCollector();
            Indexes.indexSearcher.search(bqbOneCategoryOnly.build(), collector);
            int qUniqueHits = collector.getTotalHits()

            qMap.put(q, qUniqueHits)
            totalUniqueHits += qUniqueHits
        }

        TotalHitCountCollector collector = new TotalHitCountCollector();
        Indexes.indexSearcher.search(totalHitsBQB.build(), collector);
        int totalHitsAllQueries = collector.getTotalHits();

        return new Tuple3(qMap, totalUniqueHits, totalHitsAllQueries)
    }

    void generationStats(long generation) {
        println "${queryShort()}"
        println "baseFitness: ${baseFitness.round(3)} uniqueHits: $hitsMatchingOnlyOneQuery    totalHits: $totalHits totalDocs: ${Indexes.indexReader.numDocs()} missedDocs: $missedDocs  hitsMatchingTwoOrMoreQueries: $hitsMatchingTwoOrMoreQueries  "
        println ""
    }

    String queryShort() {
        String s = ""
        queryMap.keySet().eachWithIndex { Query q, int index ->
            if (index > 0) s += '\n';
            s += "ClusterQuery: $index :  ${queryMap.get(q)}  ${q.toString(Indexes.FIELD_CONTENTS)}"
        }
        return s
    }

    //sent to stat file in statDump
    public String fitnessToStringForHumans() {
        return "ClusterQuery Fitness: ${this.fitness()}"
    }

    public String toString(int gen) {
        return "Gen: $gen ClusterQuery Fitness: ${this.fitness()} qMap: $queryMap}"
    }
}