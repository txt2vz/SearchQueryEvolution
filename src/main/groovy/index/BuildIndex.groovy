package index

import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader

import java.nio.file.Path
import java.nio.file.Paths

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TotalHitCountCollector
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory

class BuildIndex {

    static main(args) {
        new BuildIndex()
    }

    BuildIndex() {
        String indexPath =
                //'indexes/warCrimes'
                'indexes/resistance'
        String docsPath =
      /C:\Users\aceslh\IdeaProjects\txt2vz\boaData\text\secrecy/
                ///C:\Users\aceslh\OneDrive - Sheffield Hallam University\BritishOnlineArchive\holocaust\War Crimes Text Files_Combined/
                // /C:\Users\aceslh\Dataset\r5o/

        Path path = Paths.get(indexPath)
        Directory directory = FSDirectory.open(path)
        Analyzer analyzer = //new EnglishAnalyzer();  //with stemming
                new StandardAnalyzer()
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer)

//store doc counts for each category
        def catsNameFreq = [:]

// Create a new index in the directory, removing any
// previously indexed documents:
        iwc.setOpenMode(OpenMode.CREATE)
        IndexWriter writer = new IndexWriter(directory, iwc)
        //  IndexSearcher indexSearcher = new IndexSearcher(writer.getReader())
        Date start = new Date();
        println("Indexing to directory: $indexPath  from: $docsPath ...")

        def categoryNumber = -1

        new File(docsPath).eachDir {

            categoryNumber++
            int docCount = 0
            it.eachFileRecurse { file ->

                if (!file.hidden && file.exists() && file.canRead() && !file.isDirectory() && docCount < 200) // && categoryNumber <3)

                {
                    def doc = new Document()


                    Field catNumberField = new StringField(Indexes.FIELD_CATEGORY_NUMBER, String.valueOf(categoryNumber), Field.Store.YES);
                    doc.add(catNumberField)

                    Field pathField = new StringField(Indexes.FIELD_PATH, file.getPath(), Field.Store.YES);
                    doc.add(pathField);

                    String parent = file.getParent()
                    String grandParent = file.getParentFile().getParent()

                    def catName
                 //   catName = file.name.charAt(6)
                    catName = parent.substring(parent.lastIndexOf(File.separator) + 1, parent.length())

                    Field catNameField = new StringField(Indexes.FIELD_CATEGORY_NAME, catName, Field.Store.YES);
                    doc.add(catNameField)

                    String test_train
                    //   if (file.canonicalPath.contains("test")) test_train = "test" else test_train = "train"
                    //split test train 50 /50
                    if (docCount % 2 == 0) test_train = "test" else test_train = "train"

                    Field ttField = new StringField(Indexes.FIELD_TEST_TRAIN, test_train, Field.Store.YES)
                    doc.add(ttField)

                    doc.add(new TextField(Indexes.FIELD_CONTENTS, file.text, Field.Store.YES))

                    def n = catsNameFreq.get((catName)) ?: 0
                    catsNameFreq.put((catName), n + 1)

                    writer.addDocument(doc)
                    docCount++
                }
            }
        }
        println "Total docs: " + writer.maxDoc()
        writer.close()
        IndexReader indexReader = DirectoryReader.open(directory)
        IndexSearcher indexSearcher = new IndexSearcher(indexReader)
        TotalHitCountCollector trainCollector = new TotalHitCountCollector();
        final TermQuery trainQ = new TermQuery(new Term(Indexes.FIELD_TEST_TRAIN, "train"))

        TotalHitCountCollector testCollector = new TotalHitCountCollector();
        final TermQuery testQ = new TermQuery(new Term(Indexes.FIELD_TEST_TRAIN, "test"))

        indexSearcher.search(trainQ, trainCollector);
        def trainTotal = trainCollector.getTotalHits();

        indexSearcher.search(testQ, testCollector);
        def testTotal = testCollector.getTotalHits();

        Date end = new Date();
        println(end.getTime() - start.getTime() + " total milliseconds");
        println "testTotal $testTotal trainTotal $trainTotal"
        println "catsNameFreq $catsNameFreq"


        println "End ***************************************************************"
    }
}