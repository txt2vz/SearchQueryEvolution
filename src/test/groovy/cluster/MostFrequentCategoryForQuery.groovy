package cluster

import index.IndexEnum
import index.Indexes
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.TermQuery
import spock.lang.Specification

class MostFrequentCategoryForQuery extends Specification {
    def "GetMostFrequentCategoryForQuery"() {

        setup:
        Indexes.setIndex(IndexEnum.NG3)

        TermQuery spaceQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'space'))
        TermQuery orbitQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'orbit'))

        TermQuery hockeyQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'hockey'))
        TermQuery gameQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'game'))

        TermQuery emptyQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, '?jkXX'))

        when:
        BooleanQuery.Builder spaceBQB = new BooleanQuery.Builder().add(spaceQuery, BooleanClause.Occur.SHOULD)
        spaceBQB.add(orbitQuery, BooleanClause.Occur.SHOULD)
        BooleanQuery.Builder hockeyBQB = new BooleanQuery.Builder().add(hockeyQuery, BooleanClause.Occur.SHOULD)
        hockeyBQB.add(gameQuery, BooleanClause.Occur.SHOULD)

        then:
        'sci.space' == Analysis.getMostFrequentCategoryForQuery(spaceBQB.build()).first
        'rec.sport.hockey'  == Analysis.getMostFrequentCategoryForQuery(hockeyBQB.build()).first
    }
}
