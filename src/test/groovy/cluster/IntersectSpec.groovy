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
        Map<Tuple2<String, String>, Double> intersectRatioMap = qti.getIntersectRatioMap(tqa.take(100))
        List<Tuple2<String, String>> intersectRatioList = qti.getIntersectList(tqa.take(100), 0.5)

        String word0 = "nasa"
        String word1 = "space"

        List <String> wordPairSorted = [word0, word1].sort()
        Tuple2<String, String> tuple2WordPairSorted = new Tuple2<String, String>(wordPairSorted[0], wordPairSorted[1])

        then:
        intersectRatioMap.containsKey(tuple2WordPairSorted)
        intersectRatioMap[tuple2WordPairSorted] > 0.8
        intersectRatioList.contains(tuple2WordPairSorted)
        intersectRatioList.size() == 137
    }
}
