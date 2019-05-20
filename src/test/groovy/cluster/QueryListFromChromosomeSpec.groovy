package cluster

import index.ImportantTerms
import index.IndexEnum
import index.Indexes
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery

class QueryListFromChromosomeSpec extends spock.lang.Specification {

    def "QueryListFromChromosome OR 20News3 tfidf"() {
        setup:
        Indexes.instance.setIndex(IndexEnum.NG3)
        Indexes.instance.setIndexFieldsAndTotals()
        ImportantTerms impTerms = new ImportantTerms()
        TermQuery[] tfidfList = impTerms.getTFIDFTermQueryList()
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
        Indexes.NUMBER_OF_CLUSTERS == 3

        tfidfList[0].getTerm().text() == 'god'
        tfidfList[1].getTerm().text() == 'space'
        tfidfList[2].getTerm().text() == 'nasa'

        bqbL.size() ==  Indexes.NUMBER_OF_CLUSTERS
        q.toString(Indexes.FIELD_CONTENTS) == 'god'

        when:
        genome = [2, 1, 4, 8, 3, 7] as int[]
        qlfc = new QueryListFromChromosome(tfidfList as List)

        bqbL = qlfc.getSimple(genome)
        Query q0 = bqbL[0].build()
        Query q1 = bqbL[1].build()
        Query q2 = bqbL[2].build()

        then:
        q0.toString(Indexes.FIELD_CONTENTS) == 'nasa christians'
        q1.toString(Indexes.FIELD_CONTENTS) == 'space jesus'
        q2.toString(Indexes.FIELD_CONTENTS) == 'hockey team'
    }
}