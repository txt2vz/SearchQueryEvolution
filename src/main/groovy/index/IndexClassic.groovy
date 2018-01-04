package index

import groovy.io.FileType
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.index.Term
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TotalHitCountCollector
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory

import java.nio.file.Path
import java.nio.file.Paths

/**
 * From http://www.icmc.usp.br/CMS/Arquivos/arquivos_enviados/BIBLIOTECA_113_RT_395.pdf  
 *Classic4 collection [Research, 2010] are composed by 4 distinct collections: 
 *CACM (titles and abstracts from the journal Communications of the ACM), 
 *ISI (information retrieval papers), CRANFIELD (aeronautical system papers), and 
 *MEDLINE (medical journals). 
 */


class IndexClassic {
	// Create Lucene index in this directory
	//def indexPath = 	'indexes/classic4_500L5'
    def indexPath = 	'indexes/classic4_500'
	// Index files in this directory
	def docsPath =
	// /C:\Users\Laurie\Dataset\classic/
	/C:\Users\Laurie\Dataset\classic4_500/

	Path path = Paths.get(indexPath)
	Directory directory = FSDirectory.open(path)
	Analyzer analyzer = //new EnglishAnalyzer();  //with stemming
	new StandardAnalyzer();
	def catFreq=[:]

	static main(args) {
		def i = new IndexClassic()
		i.buildIndex()
	}

	def buildIndex() {
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

		// Create a new index in the directory, removing any
		// previously indexed documents:
		iwc.setOpenMode(OpenMode.CREATE);
		IndexWriter writer = new IndexWriter(directory, iwc);

		Date start = new Date();
		println("Indexing to directory '" + indexPath + "'...");
		def catNumber=0;

		new File(docsPath).eachDir {

			it.eachFileRecurse(FileType.FILES) { file ->
				//if ( !file.name.contains("cacm")) // for classic 3 exclude cacm
					indexDocs(writer,file, catNumber)
			}
			catNumber++;
		}

		Date end = new Date();
		println (end.getTime() - start.getTime() + " total milliseconds");
		println "Total docs: " + writer.maxDoc()

		IndexSearcher searcher = new IndexSearcher(writer.getReader());

		TotalHitCountCollector thcollector  = new TotalHitCountCollector();
		final TermQuery catQ = new TermQuery(new Term(IndexInfo.FIELD_CATEGORY_NAME,	"med."))
		searcher.search(catQ, thcollector);
		def categoryTotal = thcollector.getTotalHits();
		println "med. total: $categoryTotal"
		println "category frequencies: $catFreq"

		writer.close()
		IndexReader indexReader = DirectoryReader.open(directory)
		IndexSearcher indexSearcher = new IndexSearcher(indexReader)
		TotalHitCountCollector trainCollector = new TotalHitCountCollector();
		final TermQuery trainQ = new TermQuery(new Term(IndexInfo.FIELD_TEST_TRAIN, "train"))

		TotalHitCountCollector testCollector = new TotalHitCountCollector();
		final TermQuery testQ = new TermQuery(new Term(IndexInfo.FIELD_TEST_TRAIN, "test"))

		indexSearcher.search(trainQ, trainCollector);
		def trainTotal = trainCollector.getTotalHits();

		indexSearcher.search(testQ, testCollector);
		def testTotal = testCollector.getTotalHits();

		println(end.getTime() - start.getTime() + " total milliseconds");
		println "testTotal $testTotal trainTotal $trainTotal"


		println "End ***************************************************************"
	}

	//index the doc adding fields for path, category, test/train and contents
	def indexDocs(IndexWriter writer, File f, categoryNumber)
	throws IOException {

		def doc = new Document()
		Field pathField = new StringField(IndexInfo.FIELD_PATH, f.getPath(), Field.Store.YES);
		doc.add(pathField);

		//for classic dataset
		def catName = f.getName().substring(0,4)
		def n = catFreq.get((catName)) ?: 0
		if (n<500){
			catFreq.put((catName), n + 1)

			Field catNameField = new StringField(IndexInfo.FIELD_CATEGORY_NAME, catName, Field.Store.YES);
			doc.add(catNameField)
			doc.add(new TextField(IndexInfo.FIELD_CONTENTS, f.text,  Field.Store.YES)) ;

			Field catNumberField = new StringField(IndexInfo.FIELD_CATEGORY_NUMBER, String.valueOf(categoryNumber), Field.Store.YES);
			doc.add(catNumberField)

			String test_train
			if (n%2==0) test_train = 'test' else test_train = 'train'
			Field ttField = new StringField(IndexInfo.FIELD_TEST_TRAIN, test_train, Field.Store.YES)

			doc.add(ttField)
			writer.addDocument(doc);
		}
	}
}