package cluster

import index.IndexEnum
import index.Indexes
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.TermQuery

class ClauseSpec extends spock.lang.Specification {

    def "check number of cluases"() {
        setup:
        Indexes.setIndex(IndexEnum.R4)
        Indexes.setIndexFieldsAndTotals()

        when:

        def tq0 = new TermQuery(new Term(Indexes.FIELD_CONTENTS,
                'bpd'));

        def tq1 = new TermQuery(new Term(Indexes.FIELD_CONTENTS,
                'oil'));

        def bqb = new BooleanQuery.Builder()
        bqb.add(tq0, BooleanClause.Occur.MUST)
        bqb.add(tq1, BooleanClause.Occur.MUST)

        def clauses = bqb.clauses

        then:
        clauses != null
        clauses[0].toString() == '+contents:bpd'
        clauses[1].toString() == '+contents:oil'

        for (clause in clauses) {
            println "clause:  $clause"
            println "clause.query " + clause.query
            println "word " + clause.query.toString(Indexes.FIELD_CONTENTS)
            println "clause.occur: " + clause.occur
        }
    }
}
