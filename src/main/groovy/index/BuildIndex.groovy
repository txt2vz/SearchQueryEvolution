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
                //'indexes/resistance'
                'indexes/NG3'
        //       'indexes/science4'


        String docsPath =

//                /D:\Classify20NG3/
                /C:\Users\aceslh\Dataset\20NG3SpaceHockeyChristian\train/
   //     /C:\Users\aceslh\Dataset\20NG4ScienceTrain/
      ///C:\Users\aceslh\IdeaProjects\txt2vz\boaData\text\secrecy/
                ///C:\Users\aceslh\OneDrive - Sheffield Hallam University\BritishOnlineArchive\holocaust\War Crimes Text Files_Combined/

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
        Date start = new Date();
        println("Indexing to directory: $indexPath  from: $docsPath ...")

        def categoryNumber = 0

        new File(docsPath).eachDir {

            int docCount = 0
            it.eachFileRecurse { file ->

                if (!file.hidden && file.exists() && file.canRead() && !file.isDirectory() && docCount < 100) // && categoryNumber <3)

                {
                    Document doc = new Document()

                    Field catNumberField = new StringField(Indexes.FIELD_CATEGORY_NUMBER, String.valueOf(categoryNumber), Field.Store.YES);
                    doc.add(catNumberField)

             //       String s = file.getPath()
               //    String p2 = s.replaceAll('\\\\', '-')
                   //println "p2 $p2"

                    String name = file.getName()
                 //   println "name $name"

                 //  Field pathField = new StringField(Indexes.FIELD_PATH, file.getPath(), Field.Store.YES);
                 //   Field pathField = new StringField(Indexes.FIELD_PATH, p2, Field.Store.YES)
                    Field pathField = new StringField(Indexes.FIELD_PATH, name, Field.Store.YES);
                    doc.add(pathField)

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

                    Field assignedClass = new StringField(Indexes.FIELD_ASSIGNED_CLASS, 'unAssigned', Field.Store.YES);
                    doc.add(assignedClass)

                    doc.add(new TextField(Indexes.FIELD_CONTENTS, file.text, Field.Store.YES))

                    def n = catsNameFreq.get((catName)) ?: 0
                    catsNameFreq.put((catName), n + 1)

                    writer.addDocument(doc)
               //     writer.up
                    docCount++
                }
            }
            categoryNumber++
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

        TotalHitCountCollector cryptCollector = new TotalHitCountCollector();
        final TermQuery cryptQ = new TermQuery(new Term(Indexes.FIELD_CATEGORY_NAME, "sci.crypt"))
        indexSearcher.search(cryptQ, cryptCollector);
        def cryptTotal = cryptCollector.getTotalHits()
        println "cryptTotal $cryptTotal"

        println "numDocs " + indexReader.numDocs()
        println "End ***************************************************************"
    }
}