package index

import groovy.transform.CompileStatic
import org.apache.lucene.index.*
import org.apache.lucene.search.DocIdSetIterator
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.similarities.ClassicSimilarity
import org.apache.lucene.search.similarities.TFIDFSimilarity
import org.apache.lucene.util.BytesRef

@CompileStatic
class ImportantTermsForClustering {

    private static Set<String> stopSet = StopSet.getStopSetFromFile()
    private final static int MAX_TERMQUERYLIST_SIZE = 200;

    static List<TermQuery> getTFIDFTermQueryList(IndexReader indexReader) {

        TermsEnum termsEnum = MultiFields.getTerms(indexReader, Indexes.FIELD_CONTENTS).iterator()

        println "ImportantTermsForClustering Index: " + Indexes.indexEnum

        Map <TermQuery, Double> termQueryMap = [:]
        BytesRef termbr;
        TFIDFSimilarity tfidfSim = new ClassicSimilarity()
        int docCount = indexReader.numDocs()

        while ((termbr = termsEnum.next()) != null) {

            Term t = new Term(Indexes.FIELD_CONTENTS, termbr);
            int df = indexReader.docFreq(t)
            String word = t.text()

            if (
                    df > 10
                    && !stopSet.contains(word)
                    && !word.contains("'")
                    && word.length() > 1
                    && word.charAt(0).isLetter()
            ) {

                long docFreq = indexReader.docFreq(t);
                double tfidfTotal = 0

                PostingsEnum docsEnum = termsEnum.postings(MultiFields.getTermDocsEnum(indexReader, Indexes.FIELD_CONTENTS, termbr))
                if (docsEnum != null) {
                    while (docsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {

                        double tfidf = tfidfSim.tf(docsEnum.freq()) * tfidfSim.idf(docCount, docFreq)
                        tfidfTotal += tfidf
                    }
                }
                termQueryMap += [(new TermQuery(t)): tfidfTotal]
            }
        }

        termQueryMap = termQueryMap.sort { a, b -> a.value <=> b.value }
        List<TermQuery> tql = new ArrayList<TermQuery>(termQueryMap.keySet().take(MAX_TERMQUERYLIST_SIZE))

        println "termQueryMap size: ${termQueryMap.size()}  termQuerylist size: ${tql.size()}  termQuerylist: $tql"
        println "termQueryMap ${termQueryMap.take(50)}"
        return tql.asImmutable()
    }
}