package clusterExtension

import index.IndexEnum
import index.Indexes
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.Term
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TotalHitCountCollector

class CategoryFreq {

    static main(args) {

        Indexes.setIndex(IndexEnum.NG3)
        // IndexReader indexReader = DirectoryReader.open(directory)
        IndexSearcher indexSearcher = new IndexSearcher(Indexes.indexReader)
        TotalHitCountCollector trainCollector = new TotalHitCountCollector();
        final TermQuery trainQ = new TermQuery(new Term(Indexes.FIELD_TEST_TRAIN, "train"))

        TotalHitCountCollector testCollector = new TotalHitCountCollector();
        final TermQuery testQ = new TermQuery(new Term(Indexes.FIELD_TEST_TRAIN, "test"))

        indexSearcher.search(trainQ, trainCollector);
        def trainTotal = trainCollector.getTotalHits();

        indexSearcher.search(testQ, testCollector);
        def testTotal = testCollector.getTotalHits();

        Date end = new Date();
        // println(end.getTime() - start.getTime() + " total milliseconds");
        println "testTotal $testTotal trainTotal $trainTotal"
        // println "catsNameFreq $catsNameFreq"

        TotalHitCountCollector cryptCollector = new TotalHitCountCollector();
        final TermQuery spaceQ = new TermQuery(new Term(Indexes.FIELD_CATEGORY_NAME, "sci.space"))
        indexSearcher.search(spaceQ, cryptCollector);
        def spaceTotal = cryptCollector.getTotalHits()
        println "spaceTotal $spaceTotal"

        println "numDocs " + Indexes.indexReader.numDocs()

     //   TotalHitCountCollector collector = new TotalHitCountCollector();
       // Indexes.indexSearcher.search(spaceQ, collector);
        //int qUniqueHits = collector.getTotalHits()
    }
}
