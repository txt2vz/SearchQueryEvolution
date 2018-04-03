 package index

 import org.apache.lucene.document.Document
 import org.apache.lucene.index.DirectoryReader
 import org.apache.lucene.index.Term
 import org.apache.lucene.search.*
 import org.apache.lucene.store.Directory
 import org.apache.lucene.store.FSDirectory

 import java.nio.file.Path
 import java.nio.file.Paths

 class TestR10 extends spock.lang.Specification {
     Path path = Paths.get('indexes/R10')
     Directory directory = FSDirectory.open(path)
     DirectoryReader ireader = DirectoryReader.open(directory);
     IndexSearcher isearcher = new IndexSearcher(ireader);

     def 'categoryName R10' (){

         def Document d
         def categoryNumber= '7'

         setup:
         TermQuery catQ 	= new TermQuery(new Term(Indexes.FIELD_CATEGORY_NUMBER,
                 categoryNumber))

         when:
         TopScoreDocCollector collector = TopScoreDocCollector.create(1)
         isearcher.search(catQ, collector);
         ScoreDoc[] hits = collector.topDocs().scoreDocs

         hits.each {h ->
             d = isearcher.doc(h.doc)
         }

         then:
         d.get(Indexes.FIELD_CATEGORY_NAME)== 'ship'
     }

     def 'total r10 docs in category'() {
         setup:

         final TermQuery catQgrain = new TermQuery(new Term(Indexes.FIELD_CATEGORY_NAME, 'grain'))
         final TermQuery catQcrudeName = new TermQuery(new Term(Indexes.FIELD_CATEGORY_NAME, 'crude'))
         final TermQuery catQcrudeNumber = new TermQuery(new Term(Indexes.FIELD_CATEGORY_NUMBER, '2'))

         when:
         TotalHitCountCollector thcollector  = new TotalHitCountCollector();
         isearcher.search(catQgrain, thcollector)
         def grainTotal = thcollector.getTotalHits()

         thcollector  = new TotalHitCountCollector();
         isearcher.search(catQcrudeName, thcollector)
         def crudeNameTotal = thcollector.getTotalHits()

         thcollector  = new TotalHitCountCollector();
         isearcher.search(catQcrudeNumber, thcollector)
         def crudeNumberTotal = thcollector.getTotalHits()

         then:
         grainTotal == 582
         crudeNameTotal == 578
         crudeNumberTotal == crudeNameTotal

         cleanup:
         ireader.close()
     }

     def "total docs for test and train"() {
         setup:

         TotalHitCountCollector trainCollector  = new TotalHitCountCollector();
         final TermQuery trainQ = new TermQuery(new Term(Indexes.FIELD_TEST_TRAIN,	"train"))

         TotalHitCountCollector testCollector  = new TotalHitCountCollector();
         final TermQuery testQ = new TermQuery(new Term(Indexes.FIELD_TEST_TRAIN,	"test"))

         when:
         isearcher.search(trainQ, trainCollector);
         def trainTotal = trainCollector.getTotalHits();

         isearcher.search(testQ, testCollector);
         def testTotal = testCollector.getTotalHits();

         def totalDocs = ireader.maxDoc()

         then:
         trainTotal == 7193
         testTotal == 2787
         totalDocs == 9980
         totalDocs == trainTotal + testTotal

         cleanup:
         ireader.close()
     }
 }