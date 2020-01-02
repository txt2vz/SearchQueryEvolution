package cluster

import index.IndexEnum
import index.Indexes
import index.IndexUtils
import org.apache.lucene.index.Term
import org.apache.lucene.search.TermQuery

class MostFrequentCategorySpec extends spock.lang.Specification {

    def "find most frequent category R4TEST"() {
        setup:
        Indexes.setIndex(IndexEnum.R4TEST)
        TermQuery catQ

        when:
        catQ = new TermQuery(new Term(Indexes.FIELD_CATEGORY_NAME,'grain'))
        Tuple3 t3 = IndexUtils.getMostFrequentCategoryForQuery(catQ)

        then:
        t3.first == 'grain'
        t3.second == 70
        t3.third == 70

        when:
        catQ = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'bpd'));  //barrels per day
        t3 = IndexUtils.getMostFrequentCategoryForQuery(catQ)

        then:
        t3.first == 'crude'
        t3.second == 15
        t3.third == 15

        when:
        catQ = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'oil'))
        t3 = IndexUtils.getMostFrequentCategoryForQuery(catQ)

        then:
        t3.first  == 'crude'
        t3.second == 62
        t3.third == 75
    }
}
