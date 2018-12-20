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
    SCORE, HITS, PSEUDOF1, PSEUDOF1_K_PENALTY0_3
}

@CompileStatic
public class ClusterFitness extends SimpleFitness {

    static FitnessMethod fitnessMethod

    private final int totalDocs = Indexes.indexReader.maxDoc()

    Map<Query, Integer> queryMap = [:]
    double baseFitness = 0.0  //for ECJ
    double pseudo_precision = 0.0
    double pseudo_recall = 0.0
    double pseudo_f1 = 0.0

    int positiveHits = 0
    int negativeHits = 0
    int totalHits = 0
    int missedDocs = 0
    int k

    double getFitness() {
        return baseFitness;
    }

    void setClusterFitness(Set<BooleanQuery.Builder> bqbSet) {

        k = bqbSet.size()

        baseFitness = 0.0
        positiveHits = 0
        negativeHits = 0
        totalHits = 0
        missedDocs = 0
        pseudo_precision = 0.0
        pseudo_recall = 0.0

        Map<Query, Integer> qMap = new HashMap<Query, Integer>()
        BooleanQuery.Builder totalHitsBQB = new BooleanQuery.Builder()

        for (BooleanQuery.Builder bqb : bqbSet) {

            Query q = bqb.build()
            totalHitsBQB.add(q, BooleanClause.Occur.SHOULD)

            Set<BooleanQuery.Builder> otherQueries = bqbSet - bqb

            // otherQueries should not be null
            assert otherQueries

            BooleanQuery.Builder bqbOthers = new BooleanQuery.Builder();

            for (BooleanQuery.Builder obqb : otherQueries) {

                //  assert obqb - unexplained rare problem where assert fails on R5?
                if (obqb) {
                    bqbOthers.add(obqb.build(), BooleanClause.Occur.SHOULD)
                }
            }
            Query otherBQ = bqbOthers.build()

            TotalHitCountCollector collector = new TotalHitCountCollector();
            BooleanQuery.Builder bqbPos = new BooleanQuery.Builder();
            bqbPos.add(bqb.build(), BooleanClause.Occur.SHOULD)  //positiveHit SHOULD match query
            bqbPos.add(otherBQ, BooleanClause.Occur.MUST_NOT)  //positiveHits MUST NOT match other queries
            Indexes.indexSearcher.search(bqbPos.build(), collector);
            int qPositiveHits = collector.getTotalHits()
            qMap.put(q, qPositiveHits)

            positiveHits += qPositiveHits
        }

        queryMap = qMap.asImmutable()

        TotalHitCountCollector collector = new TotalHitCountCollector();
        Indexes.indexSearcher.search(totalHitsBQB.build(), collector);
        totalHits = collector.getTotalHits();
        negativeHits = totalHits - positiveHits

        assert totalHits > 0

        missedDocs = totalDocs - totalHits

        pseudo_precision = positiveHits / totalHits
        pseudo_recall = totalHits / totalDocs
        pseudo_f1 = 2 * (pseudo_precision * pseudo_recall) / (pseudo_precision + pseudo_recall)

        switch (fitnessMethod) {

            case fitnessMethod.PSEUDOF1:
                baseFitness = pseudo_f1
                break

            case fitnessMethod.PSEUDOF1_K_PENALTY0_3:
                double f1WithPenalty = pseudo_f1 - (0.03 * k)
                baseFitness = f1WithPenalty > 0 ? f1WithPenalty : 0
                break
        }
    }

    void generationStats(long generation) {
        println "${queryShort()}"
        println "pseudo_precision: ${pseudo_precision.round(3)} pseudo_recall: ${pseudo_recall.round(3)} pseudo_f1: ${pseudo_f1.round(3)} baseFitness: ${baseFitness.round(3)}"
        println "totalHits: $totalHits totalDocs: $totalDocs missedDocs: $missedDocs posHits: $positiveHits negHits: $negativeHits  "
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