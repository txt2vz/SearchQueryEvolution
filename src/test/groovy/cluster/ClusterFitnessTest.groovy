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
        BooleanQuery.Builder[] bqbL = new BooleanQuery.Builder[2]

        TermQuery spaceQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'space'))
        TermQuery orbitQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'orbit'))

        TermQuery hockeyQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'hockey'))
        TermQuery gameQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'game'))

        TermQuery emptyQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, '?jkXX'))

        when:
        bqbL[0] = new BooleanQuery.Builder().add(spaceQuery, BooleanClause.Occur.SHOULD)
        bqbL[0].add(orbitQuery, BooleanClause.Occur.SHOULD)

        bqbL[1] = new BooleanQuery.Builder().add(hockeyQuery, BooleanClause.Occur.SHOULD)

        Set<BooleanQuery.Builder> bqbSet = bqbL as Set<BooleanQuery.Builder>
        cf.setClusterFitness(bqbSet)

        then:
        println "base "  + cf.getBaseFitness() + "  " + cf.k  + " pos " + cf.positiveHits + " pseudof1 " + cf.pseudo_f1
        cf.k == 2
        println "cf.totalHitsFromQ " + cf.totalHitsFromQ + " posfromQ " + cf.postiveHitsFromQ
        cf.totalHits== cf.totalHitsFromQ
        cf.positiveHits == cf.postiveHitsFromQ

       // def q = cf.totalHitsBQB.build()

     //   println "q " + q.toString()

        when:
        bqbL[0] = new BooleanQuery.Builder().add(emptyQuery, BooleanClause.Occur.MUST)
        bqbL[0].add(hockeyQuery, BooleanClause.Occur.MUST)

        bqbL[1] = new BooleanQuery.Builder().add(emptyQuery, BooleanClause.Occur.MUST)
        bqbL[0].add(orbitQuery, BooleanClause.Occur.MUST)
        Set<BooleanQuery.Builder> bqbSet2 = bqbL as Set<BooleanQuery.Builder>
        cf.setClusterFitness(bqbSet2)

        then:
        println "2base "  + cf.getBaseFitness() + "  " + cf.k  + " pos " + cf.positiveHits + " pseudof1 " + cf.pseudo_f1
        println "cf.totalHitsFromQ " + cf.totalHitsFromQ
        cf.pseudo_f1 == 0
        cf.totalHits== cf.totalHitsFromQ

        when:
        bqbL[0] = new BooleanQuery.Builder().add(hockeyQuery, BooleanClause.Occur.SHOULD)
        bqbL[1] = new BooleanQuery.Builder().add(gameQuery, BooleanClause.Occur.SHOULD)
        Set<BooleanQuery.Builder> bqbSet3 = bqbL as Set<BooleanQuery.Builder>
        cf.setClusterFitness(bqbSet3)

        then:
        println "cf.totalHitsFromQ three " + cf.totalHitsFromQ + " posfromQ " + cf.postiveHitsFromQ
        cf.totalHits== cf.totalHitsFromQ
        cf.positiveHits == cf.postiveHitsFromQ
        cf.negativeHits == cf.totalHitsFromQ - cf.postiveHitsFromQ


    }
}
