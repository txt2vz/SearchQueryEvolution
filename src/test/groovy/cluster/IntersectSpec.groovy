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
        int hitsPerPage = Indexes.indexReader.maxDoc()
        TermQuery[] tqa = new ImportantTerms().getTFIDFTermQueryList()
        QueryTermIntersect qti = new QueryTermIntersect()

        when:
        Map intersectCountMap = qti.getIntersectCountMap(tqa.take(20))
        intersectCountMap.sort { -it.value }

        TermQuery t0 = new TermQuery(new Term(Indexes.FIELD_CONTENTS, "system"))
        TermQuery t1 = new TermQuery(new Term(Indexes.FIELD_CONTENTS, "key"))

        List termQueryList = [t0, t1]
        Tuple2<TermQuery, TermQuery> tuple2Unsorted = new Tuple2<TermQuery, TermQuery>(termQueryList[0], termQueryList[1])

        termQueryList.sort { it.term }
        Tuple2<TermQuery, TermQuery> tuple2Sorted = new Tuple2<TermQuery, TermQuery>(termQueryList[0], termQueryList[1])

        List termTuple2List = qti.getIntersectList(tqa.take(20), 10)

        then:
        intersectCountMap.containsKey(tuple2Sorted)
        !intersectCountMap.containsKey(tuple2Unsorted)
        intersectCountMap[tuple2Sorted]==40
        termTuple2List.contains(tuple2Sorted)
        termTuple2List.size() == 129
    }
}
