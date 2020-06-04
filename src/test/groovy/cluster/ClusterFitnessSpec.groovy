package cluster

import index.IndexEnum
import index.Indexes
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TotalHitCountCollector
import spock.lang.Specification

class ClusterFitnessSpec extends Specification {

    def "check psuedo f1 precision recall "(){
        setup:
        Indexes.setIndex(IndexEnum.NG3TEST)
        def cf = new ECJclusterFitness()
        ECJclusterFitness.FITNESS_METHOD = FitnessMethodECJ.UNIQUE_HITS_COUNT//FitnessMethod.PSEUDOF1
        BooleanQuery.Builder[] bqbL = new BooleanQuery.Builder[2]

        TermQuery spaceQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'space'))
        TermQuery orbitQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'orbit'))

        TermQuery hockeyQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'hockey'))
        TermQuery gameQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'game'))

        TermQuery emptyQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, '?jkXX'))

        when:
        bqbL[0] = new BooleanQuery.Builder().add(spaceQuery, BooleanClause.Occur.SHOULD)
        bqbL[1] = new BooleanQuery.Builder().add(orbitQuery, BooleanClause.Occur.SHOULD)

        TotalHitCountCollector collector = new TotalHitCountCollector();
        Indexes.indexSearcher.search(bqbL[0].build(), collector);
        int spaceHits = collector.getTotalHits()

        collector = new TotalHitCountCollector();
        Indexes.indexSearcher.search(bqbL[1].build(), collector);
        int orbitHits = collector.getTotalHits()

        collector = new TotalHitCountCollector();
        def spaceORorbit = new BooleanQuery.Builder().add(spaceQuery, BooleanClause.Occur.SHOULD)
        spaceORorbit.add(orbitQuery, BooleanClause.Occur.SHOULD)

        Indexes.indexSearcher.search(spaceORorbit.build(), collector);
        int spaceORorbitHits = collector.getTotalHits()

        def spaceANDorbit = new BooleanQuery.Builder().add(spaceQuery, BooleanClause.Occur.MUST)
        spaceANDorbit.add(orbitQuery, BooleanClause.Occur.MUST)
        collector = new TotalHitCountCollector();
        Indexes.indexSearcher.search(spaceANDorbit.build(), collector);
        int spaceANDorbitHits = collector.getTotalHits()

        cf = new ECJclusterFitness()
      //  Set<BooleanQuery.Builder> bqbSet0 = bqbL as Set<BooleanQuery.Builder>
        cf.setClusterFitness(Arrays.asList(bqbL).asImmutable())

        then:

        println " "
        println "spaceHits: $spaceHits orbitHits: $orbitHits spaceORorbit: $spaceORorbitHits spaceANDorbit: $spaceANDorbitHits"
        println " "

        cf.totalHits == spaceORorbitHits
        cf.hitsMatchingOnlyOneQuery == spaceORorbitHits - spaceANDorbitHits

    }
}
