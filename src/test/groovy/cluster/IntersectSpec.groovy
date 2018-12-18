package cluster

import index.ImportantTerms
import index.IndexEnum
import index.Indexes
import org.apache.lucene.index.Term
import org.apache.lucene.search.Query
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

        String shuttleString = 'shuttle'
        String spaceString = 'space'
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

        intersectRatioMap[tuple2WordShuttleSpace] ==
                QueryTermIntersect.getTermIntersectRatioUsingAND(shuttleTermQuery, spaceTermQuery)

        QueryTermIntersect.getTermIntersectRatioUsingAND(shuttleTermQuery, spaceTermQuery) ==  QueryTermIntersect.getIntersectRatio(shuttleTermQuery, spaceTermQuery)
    }

    def "clusterIntersect on 20NG5"() {
        setup:
        Indexes.instance.setIndex(IndexEnum.NG5)
        TermQuery[] tqa = new ImportantTerms().getTFIDFTermQueryList()
        QueryTermIntersect qti = new QueryTermIntersect()

        when:
        Query hockeyQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'hockey'))
        Query nasaQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'nasa'))
        Query playQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'play'))
        Query spaceQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'space'))
        Query orbitQuery = new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'orbit'))

        double ratioHockeyNasa = QueryTermIntersect.getIntersectRatio(hockeyQuery, nasaQuery)
        double ratioNasaSpace = QueryTermIntersect.getIntersectRatio(nasaQuery, spaceQuery)
        double ratioSpaceNasa = QueryTermIntersect.getIntersectRatio(spaceQuery, nasaQuery)
        double ratioHockeyPlay = QueryTermIntersect.getIntersectRatio(hockeyQuery, playQuery)
        double ratioPlayHockey = QueryTermIntersect.getIntersectRatio(playQuery, hockeyQuery)
        double ratioOrbitNasa = QueryTermIntersect.getIntersectRatio(orbitQuery, nasaQuery)
        double ratioNasaOrbit = QueryTermIntersect.getIntersectRatio(nasaQuery, orbitQuery)
        double ratioSpaceHockey = QueryTermIntersect.getIntersectRatio(spaceQuery, hockeyQuery)
        double ratioSpaceOrbit = QueryTermIntersect.getIntersectRatio(spaceQuery, orbitQuery)
        double ratiOrbitSpace = QueryTermIntersect.getIntersectRatio(orbitQuery, spaceQuery)

        println "intersect ratioHockeyPlay: $ratioHockeyPlay  ratioPlayHockey $ratioPlayHockey  ratioNasaSapce $ratioNasaSpace  ratioSpaceNasa: $ratioSpaceNasa ratioOrbitNasa $ratioOrbitNasa ratioNasaOrbit $ratioNasaOrbit"
        println "spaceHockey $ratioSpaceHockey spaceOrbit: $ratioSpaceOrbit ratioOrbitSpace $ratiOrbitSpace"

        then:
        ratioHockeyNasa == 0
        ratioHockeyPlay > 0.6
        ratioPlayHockey > 0.5
    }
}