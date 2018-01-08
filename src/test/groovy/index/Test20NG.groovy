 package index

 import org.apache.lucene.document.Document
 import org.apache.lucene.index.DirectoryReader
 import org.apache.lucene.index.Term
 import org.apache.lucene.search.*
 import org.apache.lucene.store.Directory
 import org.apache.lucene.store.FSDirectory

 import java.nio.file.Path
 import java.nio.file.Paths

 class Test20NG extends spock.lang.Specification {
     Path path = Paths.get('indexes/20NG')
     Directory directory = FSDirectory.open(path)
     DirectoryReader ireader = DirectoryReader.open(directory);
     IndexSearcher isearcher = new IndexSearcher(ireader)

     def 'categoryNameNG20' (){
         def Document d
         def categoryNumber= '3'
         setup:
         TermQuery catQ 	= new TermQuery(new Term(IndexInfo.FIELD_CATEGORY_NUMBER,
                 categoryNumber))

         when:
         TopScoreDocCollector collector = TopScoreDocCollector.create(1)
         isearcher.search(catQ, collector);
         ScoreDoc[] hits = collector.topDocs().scoreDocs

         hits.each {h ->
             d = isearcher.doc(h.doc)
         }

         then:
         d.get(IndexInfo.FIELD_CATEGORY_NAME) == 'comp.sys.ibm.pc.hardware'
     }

     def 'total NG20 docs in comp.graphics categroy'() {
         setup:

         TotalHitCountCollector thcollector  = new TotalHitCountCollector();
         final TermQuery graphicsNameQ = new TermQuery(new Term(IndexInfo.FIELD_CATEGORY_NAME, 'comp.graphics'))
         final TermQuery graphicsNumberQ = new TermQuery(new Term(IndexInfo.FIELD_CATEGORY_NUMBER, '1'))

         when:
         isearcher.search(graphicsNameQ, thcollector)
         def graphicsNameTotal = thcollector.getTotalHits()

         thcollector  = new TotalHitCountCollector();
         isearcher.search(graphicsNumberQ, thcollector)
         def graphicsNumberTotal = thcollector.getTotalHits();

         then:
         graphicsNameTotal == 973
         graphicsNumberTotal == graphicsNameTotal

         cleanup:
         ireader.close()
     }

     def "total docs for test and train"() {
         setup:

         TotalHitCountCollector trainCollector  = new TotalHitCountCollector();
         final TermQuery trainQ = new TermQuery(new Term(IndexInfo.FIELD_TEST_TRAIN,	"train"))

         TotalHitCountCollector testCollector  = new TotalHitCountCollector();
         final TermQuery testQ = new TermQuery(new Term(IndexInfo.FIELD_TEST_TRAIN,	"test"))

         when:
         isearcher.search(trainQ, trainCollector);
         def trainTotal = trainCollector.getTotalHits();

         isearcher.search(testQ, testCollector);
         def testTotal = testCollector.getTotalHits();

         def totalDocs = ireader.maxDoc()

         then:
         trainTotal == 11314
         testTotal == 7532
         totalDocs == 18846
         totalDocs == trainTotal + testTotal

         cleanup:
         ireader.close()
     }
 }