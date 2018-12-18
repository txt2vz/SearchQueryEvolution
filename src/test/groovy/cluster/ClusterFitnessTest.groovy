package cluster

import index.IndexEnum
import index.Indexes
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import spock.lang.Specification

class ClusterFitnessTest extends Specification {

    def "check psuedo f1 precision recall "(){
        setup:
        Indexes.instance.setIndex(IndexEnum.NG3)
        def cf = new ClusterFitness()
        ClusterFitness.fitnessMethod = FitnessMethod.PSEUDOF1

        TermQuery spaceQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'space'))
        TermQuery orbitQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'orbit'))

        TermQuery hockeyQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'hockey'))

        when:
        def totalHits = 10
        def totalDocs = 20
        def negativeHits = 4
        def positiveHits = 5
        def missedDocs = totalDocs - totalHits

        def precision = positiveHits/totalHits
        def recall = positiveHits / totalDocs
        def f1 = (2 * (precision * recall) ) / (precision + recall)
        def pseudoF1 = 2 * positiveHits / (2 * positiveHits + negativeHits + missedDocs)

        then:
        precision ==0.5
        recall == 0.25

        println "preciionst $precision  pseudo_recall $recall"
        println "f1 $f1 pseudoF1 $pseudoF1 missed docs $missedDocs"

        when:

        BooleanQuery.Builder[] bqbL = new BooleanQuery.Builder[2]
        bqbL[0] = new BooleanQuery.Builder().add(spaceQuery, BooleanClause.Occur.SHOULD)
        bqbL[0].add(orbitQuery, BooleanClause.Occur.SHOULD)

        bqbL[1] = new BooleanQuery.Builder().add(hockeyQuery, BooleanClause.Occur.SHOULD)

        Set<BooleanQuery.Builder> bqbSet = bqbL as Set<BooleanQuery.Builder>

        println "bqbSet $bqbSet"

        cf.setClusterFitness(bqbSet)

        then:
        println "base "  + cf.getBaseFitness() + "  " + cf.k  + " pos " + cf.positiveHits + " pseudof1 " + cf.pseudo_f1


    }
}
