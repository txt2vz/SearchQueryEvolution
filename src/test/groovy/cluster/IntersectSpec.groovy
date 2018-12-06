package cluster

import index.ImportantTerms
import index.IndexEnum
import index.Indexes
import org.apache.lucene.index.Term
import org.apache.lucene.search.TermQuery

class IntersectSpec extends spock.lang.Specification {
    def "clusterIntersect on 20NG6"() {
        setup:
        Indexes.instance.setIndex(IndexEnum.NG6)
        TermQuery[] tqa = new ImportantTerms().getTFIDFTermQueryList()
        QueryTermIntersect qti = new QueryTermIntersect()

        when:
        Map<Tuple2<String, String>, Double> intersectRatioMap = qti.getIntersectRatioMap(tqa)
        List<Tuple2<String, String>> intersectRatioList = qti.getIntersectList(tqa, 0.5)

        String shuttleString = "shuttle"
        String spaceString = "space"
        TermQuery shuttleTermQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, shuttleString))
        TermQuery spaceTermQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, spaceString))

        Tuple2<String, String> tuple2WordShuttleSpace = new Tuple2<String, String>(shuttleString, spaceString)
        Tuple2<String, String> tuple2WordSpaceShuttle = new Tuple2<String, String>(spaceString, shuttleString)

        then:
        intersectRatioMap.containsKey(tuple2WordShuttleSpace)
        intersectRatioMap.containsKey(tuple2WordSpaceShuttle)
        intersectRatioMap[tuple2WordSpaceShuttle] > 0.5
        intersectRatioMap[tuple2WordShuttleSpace] < 0.5

        intersectRatioList.size() == 592
        QueryTermIntersect.getIntersectRatio(spaceTermQuery, shuttleTermQuery) > 0.5

        intersectRatioMap[tuple2WordShuttleSpace] ==
                QueryTermIntersect.getIntersectRatio(shuttleTermQuery, spaceTermQuery)

    }
}
