package cluster

import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopDocs

@CompileStatic
class QueryTermIntersect {

    private Map<Tuple2<String, String>, Integer> getIntersectCountMap(TermQuery[] termQueryArray) {
        int hitsPerPage = Indexes.indexReader.maxDoc()
        Map<Tuple2<String, String>, Integer> wordPairInteresectCountMap = new HashMap<Tuple2<String, String>, Integer>()

        for (int i = 0; i < termQueryArray.size(); i++) {
            for (int j = i + 1; j < termQueryArray.size(); j++) {
                TermQuery t0 = termQueryArray[i]
                TermQuery t1 = termQueryArray[j]

                Set<Integer> t0DocID_Set = [] as Set<Integer>

                TopDocs t0TopDocs = Indexes.indexSearcher.search(t0, hitsPerPage)
                ScoreDoc[] t0Hits = t0TopDocs.scoreDocs;

                for (ScoreDoc d : t0Hits) {
                    t0DocID_Set << d.doc
                }

                TopDocs t1TopDocs = Indexes.indexSearcher.search(t1, hitsPerPage)
                ScoreDoc[] t1Hits = t1TopDocs.scoreDocs;

                int intersectCount = 0

                for (ScoreDoc d : t1Hits) {
                    if (t0DocID_Set.contains(d.doc)) {
                        intersectCount++
                    }
                }

                List<String> sortedTermQueryPair = [t0.toString(Indexes.FIELD_CONTENTS), t1.toString(Indexes.FIELD_CONTENTS)].sort { it }
                wordPairInteresectCountMap.put(new Tuple2(sortedTermQueryPair[0], sortedTermQueryPair[1]), intersectCount)
            }
        }
        println "wordPairIntersectCountMap: " + wordPairInteresectCountMap.sort{-it.value}.take(20)
        return wordPairInteresectCountMap
    }

    List<Tuple2<String, String>> getIntersectList(TermQuery[] termQueryArray, int minDocCount) {

        return getIntersectCountMap(termQueryArray).findAll { it.value > minDocCount }.keySet() as List
    }
}