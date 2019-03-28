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
    static double kPenalty = 0.03d

    private final int totalDocs = Indexes.indexReader.maxDoc()

    static Map<Integer, Double> penaltyMap = [2: 1.0d, 3: 0.97d, 4: 0.94d, 5: 0.91d, 6: 0.88d, 7: 0.85d, 8: 0.82d, 9: 0.79d].asImmutable()

    Map<Query, Integer> queryMap = [:]
    double baseFitness = 0.0  //for ECJ
    double pseudo_precision = 0.0
    double pseudo_recall = 0.0
    double pseudo_f1 = 0.0

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
        missedDocs = totalDocs - totalHits

        if (totalHits > 0) {
            pseudo_precision = hitsMatchingOnlyOneQuery / totalHits
            //  pseudo_recall = totalHits / totalDocs
            pseudo_recall = hitsMatchingOnlyOneQuery / totalDocs
            pseudo_f1 = 2 * (pseudo_precision * pseudo_recall) / (pseudo_precision + pseudo_recall)
        }
        switch (fitnessMethod) {

            case fitnessMethod.UNIQUE_HITS_COUNT:
                baseFitness = hitsMatchingOnlyOneQuery
                break

            case fitnessMethod.UNIQUE_HITS_K_PENALTY:
                //  final double uniqueWithPenalty = hitsMatchingOnlyOneQuery - (0.03 * k)

                //baseFitness = hitsMatchingOnlyOneQuery * penaltyMap[k]
                // baseFitness = hitsMatchingOnlyOneQuery * (1.0 - (0.03 * k))

                baseFitness = hitsMatchingOnlyOneQuery * (1.0 - (0.03 * k))
                //      baseFitness = hitsMatchingOnlyOneQuery * Math.pow(0.97d, (double)(k)) //> 0 ? uniqueWithPenalty : 0
                break


        //      case fitnessMethod.PSEUDOF1_K_PENALTY0_3:
        //        double f1WithPenalty = pseudo_f1 - (0.03 * k)
        //      baseFitness = f1WithPenalty > 0 ? f1WithPenalty : 0
        //    break
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
        println "pseudo_precision: ${pseudo_precision.round(3)} pseudo_recall: ${pseudo_recall.round(3)} pseudo_f1: ${pseudo_f1.round(3)} baseFitness: ${baseFitness.round(3)}"
        println "totalHits: $totalHits totalDocs: $totalDocs missedDocs: $missedDocs uniqueHits: $hitsMatchingOnlyOneQuery hitsMatchingTwoOrMoreQueries: $hitsMatchingTwoOrMoreQueries  "
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