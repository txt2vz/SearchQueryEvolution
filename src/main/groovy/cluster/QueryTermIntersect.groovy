package cluster

import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopDocs

@CompileStatic
class QueryTermIntersect {

    private Map<Tuple2<String, String>, Double> getIntersectRatioMap(TermQuery[] termQueryArray) {
        final int hitsPerPage = Indexes.indexReader.maxDoc()
        Map<Tuple2<String, String>, Double> wordPairIntersectRatioMap = new HashMap<Tuple2<String, String>, Double>()

        for (int i = 0; i < termQueryArray.size(); i++) {
            for (int j = i + 1; j < termQueryArray.size(); j++) {
                TermQuery t0 = termQueryArray[i]
                TermQuery t1 = termQueryArray[j]

                Set<Integer> t0DocID_Set = [] as Set<Integer>
                Set<Integer> t1DocID_Set = [] as Set<Integer>

                TopDocs t0TopDocs = Indexes.indexSearcher.search(t0, hitsPerPage)
                ScoreDoc[] t0Hits = t0TopDocs.scoreDocs;

                for (ScoreDoc d : t0Hits) {
                    t0DocID_Set << d.doc
                }

                TopDocs t1TopDocs = Indexes.indexSearcher.search(t1, hitsPerPage)
                ScoreDoc[] t1Hits = t1TopDocs.scoreDocs;

                int intersectCount = 0

                for (ScoreDoc d : t1Hits) {
                    t1DocID_Set << d.doc
                    if (t0DocID_Set.contains(d.doc)) {

                        intersectCount++
                    }
                }

                final int both = t0DocID_Set.size() + t1DocID_Set.size()
                Set<Integer> union = t0DocID_Set.plus(t1DocID_Set)
                final int intersect = both - union.size()
                assert intersect == intersectCount

                final double intersectRatio = intersect / t1DocID_Set.size()

                List<String> sortedTermQueryPair = [t0.toString(Indexes.FIELD_CONTENTS), t1.toString(Indexes.FIELD_CONTENTS)].sort {it}
                wordPairIntersectRatioMap.put(new Tuple2(sortedTermQueryPair[0], sortedTermQueryPair[1]), intersectRatio)
            }
        }
        println "wordPairIntersectRatioMap: " + wordPairIntersectRatioMap.sort { -it.value }.take(100)
        return wordPairIntersectRatioMap
    }

    List<Tuple2<String, String>> getIntersectList(TermQuery[] termQueryArray, double minRatio) {

        return getIntersectRatioMap(termQueryArray).findAll { it.value > minRatio }.keySet() as List
    }
}