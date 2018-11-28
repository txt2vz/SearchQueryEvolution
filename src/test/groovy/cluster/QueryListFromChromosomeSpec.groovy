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
        def tfidfList = impTerms.getTFIDFTermQueryList()

        when:
        int[] genome = [0, 1, 2] as int[]
        QueryListFromChromosome qlfc = new QueryListFromChromosome(tfidfList)
       // qlfc.intChromosome = genome

        List<BooleanQuery.Builder> bqbL = qlfc.getSimple(genome, false)
        Query q = bqbL[0].build()
        String s = q.toString(Indexes.FIELD_CONTENTS)
        println "s $s"
        TermQuery tq = new TermQuery (new Term(Indexes.FIELD_CONTENTS,s))
        println "tq $tq"

        then:
        //   tfidfList[0].getTerm().text() == 'space'
        //   tfidfList[1].getTerm().text() == 'god'
        //   tfidfList[3].getTerm().text() == 'game'
        bqbL.size() ==  Indexes.NUMBER_OF_CLUSTERS//numberOfClusters
        q.toString(Indexes.FIELD_CONTENTS) == 'god'

        when:
        genome = [0, 1, 2, 3, 4, 5] as int[]
        qlfc = new QueryListFromChromosome(tfidfList)
        //qlfc.intChromosome = genome as int[]
        bqbL = qlfc.getSimple(genome, false)
        q = bqbL[0].build()

        then:
        q.toString(Indexes.FIELD_CONTENTS) == 'god church'
    }
}