package cluster

import index.ImportantTerms
import index.IndexInfo
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query

class QueryListFromChromosomeSpec extends spock.lang.Specification {

    def "QueryListFromChromosome OR 20News3 tfidf"() {
        setup:
        IndexInfo.pathToIndex = 'indexes/NG20SpaceHockeyChristian'
        int numberOfClusters = 3
        IndexInfo.setIndex()
        IndexInfo.instance.setIndexFieldsAndTotals()
        ImportantTerms impTerms = new ImportantTerms()
        def tfidfList = impTerms.getTFIDFTermQueryList()

        when:
        int[] intArray = [0, 1, 2]
        List<BooleanQuery.Builder> bqbL = QueryListFromChromosome.getORQueryList(intArray, tfidfList, numberOfClusters)
        Query q = bqbL[0].build()

        then:
        //   tfidfList[0].getTerm().text() == 'space'
        //   tfidfList[1].getTerm().text() == 'god'
        //   tfidfList[3].getTerm().text() == 'game'
        bqbL.size() == numberOfClusters
        q.toString(IndexInfo.FIELD_CONTENTS) == 'space'

        when:
        intArray = [0, 1, 2, 3, 4, 5]
        bqbL = QueryListFromChromosome.getORQueryList(intArray, tfidfList, numberOfClusters)
        q = bqbL[0].build()

        then:
        q.toString(IndexInfo.FIELD_CONTENTS) == 'space game'
    }
}