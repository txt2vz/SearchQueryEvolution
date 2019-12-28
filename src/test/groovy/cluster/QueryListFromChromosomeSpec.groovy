package cluster

import index.ImportantTermQueries
import index.IndexEnum
import index.Indexes
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery

class QueryListFromChromosomeSpec extends spock.lang.Specification {

    def "QueryListFromChromosome OR 20News3 tfidf"() {
        setup:
        Indexes.setIndex(IndexEnum.NG3TEST)
     //   Indexes.setIndexFieldsAndTotals()
      //  ImportantTermsOld impTermQueries = new ImportantTermsOld()
        ImportantTermQueries impTermQueries = new ImportantTermQueries()
        TermQuery[] tfidfList = impTermQueries.getTFIDFTermQueryList(Indexes.indexReader)
        QueryListFromChromosome.intersectMethod = IntersectMethod.RATIO_POINT_5

        when:
        int[] genome = [0, 1, 2] as int[]
        QueryListFromChromosome qlfc = new QueryListFromChromosome(tfidfList as List)

        List<BooleanQuery.Builder> bqbL = qlfc.getSimple(genome)
        Query q = bqbL[0].build()
        String s = q.toString(Indexes.FIELD_CONTENTS)
        println "s $s"
        TermQuery tq = new TermQuery (new Term(Indexes.FIELD_CONTENTS,s))
        println "tq $tq"

        then:
        Indexes.index.numberOfCategories == 3

        tfidfList[0].getTerm().text() == 'nasa'
        tfidfList[1].getTerm().text() == 'space'
        tfidfList[2].getTerm().text() == 'god'

        bqbL.size() ==  Indexes.index.numberOfCategories
        q.toString(Indexes.FIELD_CONTENTS) == 'nasa'

        when:
        genome = [2, 1, 4, 8, 3, 7] as int[]
        qlfc = new QueryListFromChromosome(tfidfList as List)

        bqbL = qlfc.getSimple(genome)
        Query q0 = bqbL[0].build()
        Query q1 = bqbL[1].build()
        Query q2 = bqbL[2].build()

        then:
        q0.toString(Indexes.FIELD_CONTENTS) == 'god team'
        q1.toString(Indexes.FIELD_CONTENTS) == 'space hockey'
        q2.toString(Indexes.FIELD_CONTENTS) == 'orbit game'
    }
}