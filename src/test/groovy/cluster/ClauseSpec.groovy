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
        Indexes.instance.setIndex(IndexEnum.R4)
        Indexes.instance.setIndexFieldsAndTotals()

        when:

      //  List<BooleanQuery.Builder> bqbL = []

        def tq0 = new TermQuery(new Term(Indexes.FIELD_CONTENTS,
                'bpd'));


        def tq1 = new TermQuery(new Term(Indexes.FIELD_CONTENTS,
                'oil'));

      //  def bq = new BooleanQuery(tq, BoooleanClause.O)

        def bqb = new BooleanQuery.Builder()
                bqb.add(tq0,BooleanClause.Occur.MUST)
        bqb.add(tq1,BooleanClause.Occur.MUST)


       // BooleanQuery.Builder subbqb = new BooleanQuery.Builder();
       // BooleanQuery.Builder subbqb = new BooleanQuery.Builder();
       // subbqb.add(term0, BooleanClause.Occur.MUST)



      //  bqb.add(bq)
       // BooleanQuery bq
        def clauses = bqb.clauses

        then:
        clauses != null

        def z = clauses[0]
        !clauses.isEmpty()

        for (clause in clauses) {
            println "cluase is $clause"
            println "query " +clause.query
            println "word " + clause.query.toString(Indexes.FIELD_CONTENTS)
            println "z is $z"

        }

        when:
        def words = ['first', 'second', 'third', 'fourth' ,'fifth'] as String[]
        TermQuery[] tqa = new TermQuery[5]

        words.eachWithIndex{w, i ->
            tqa[i] = new TermQuery(new Term(t))
        }

    }

}
