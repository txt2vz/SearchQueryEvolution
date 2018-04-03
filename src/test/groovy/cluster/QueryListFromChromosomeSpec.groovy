package cluster

import index.ImportantTerms
import index.IndexEnum
import index.Indexes
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query

class QueryListFromChromosomeSpec extends spock.lang.Specification {

    def "QueryListFromChromosome OR 20News3 tfidf"() {
        setup:
        Indexes.instance.setIndex(IndexEnum.NG3)
        Indexes.instance.setIndexFieldsAndTotals()
        ImportantTerms impTerms = new ImportantTerms()
        def tfidfList = impTerms.getTFIDFTermQueryList()

        when:
        int[] intArray = [0, 1, 2]
        List<BooleanQuery.Builder> bqbL = QueryListFromChromosome.getORQueryList(intArray, tfidfList, Indexes.NUMBER_OF_CLUSTERS)
        Query q = bqbL[0].build()

        then:
        //   tfidfList[0].getTerm().text() == 'space'
        //   tfidfList[1].getTerm().text() == 'god'
        //   tfidfList[3].getTerm().text() == 'game'
        bqbL.size() ==  Indexes.NUMBER_OF_CLUSTERS//numberOfClusters
        q.toString(Indexes.FIELD_CONTENTS) == 'god'

        when:
        intArray = [0, 1, 2, 3, 4, 5]
        bqbL = QueryListFromChromosome.getORQueryList(intArray, tfidfList, Indexes.NUMBER_OF_CLUSTERS)
        q = bqbL[0].build()

        then:
        q.toString(Indexes.FIELD_CONTENTS) == 'god game'
    }
}