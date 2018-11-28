package cluster

import index.ImportantTerms
import index.IndexEnum
import index.Indexes
import org.apache.lucene.search.TermQuery

class IntersectSpec extends spock.lang.Specification {
    def "clusterIntersect on 20NG6"() {
        setup:
        Indexes.instance.setIndex(IndexEnum.NG6)
        TermQuery[] tqa = new ImportantTerms().getTFIDFTermQueryList()
        QueryTermIntersect qti = new QueryTermIntersect()

        when:
        Map<Tuple2<String, String>, Integer> intersectCountMap = qti.getIntersectCountMap(tqa.take(100))
        List<Tuple2<String, String>> intersectCountList = qti.getIntersectList(tqa.take(100), 10)

        String word0 = "nasa"
        String word1 = "space"

        List <String> wordPairSorted = [word0, word1].sort()
        Tuple2<String, String> tuple2WordPairUnSorted = new Tuple2<String, String>(word0, word1)
        Tuple2<String, String> tuple2WordPairSorted = new Tuple2<String, String>(wordPairSorted[0], wordPairSorted[1])

        then:
        intersectCountMap.containsKey(tuple2WordPairSorted)
      //  !intersectCountMap.containsKey(tuple2WordPairUnSorted)
        intersectCountMap[tuple2WordPairSorted]==40
        intersectCountList.contains(tuple2WordPairSorted)
        intersectCountList.size() == 129
    }
}
