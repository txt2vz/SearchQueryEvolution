package cluster

import index.IndexEnum
import index.Indexes
import org.apache.lucene.index.Term
import org.apache.lucene.search.TermQuery

class EvaluateClustersSpec extends spock.lang.Specification {

    def "find most frequent category r4"() {
        setup:
        Indexes.instance.setIndex(IndexEnum.R4)
        Indexes.instance.setIndexFieldsAndTotals()
        String maxCatName
        int maxCatHits
        int totalHits
        TermQuery catQ

        def jr = new JobReport()

        when:
        catQ = new TermQuery(new Term(Indexes.FIELD_CATEGORY_NAME,
                'grain'));
        (maxCatName, maxCatHits, totalHits) = jr.findMostFrequentCategoryForQuery(catQ, 0)

        then:
        maxCatName == 'grain'
        maxCatHits == 100

        when:
        catQ = new TermQuery(new Term(Indexes.FIELD_CONTENTS,
                'bpd'));  //barrels per day
        (maxCatName, maxCatHits, totalHits) = jr.findMostFrequentCategoryForQuery(catQ, 0)

        then:
        maxCatName == 'crude'
        maxCatHits == 28
    }
}
