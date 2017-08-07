package index

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.search.similarities.ClassicSimilarity
import org.apache.lucene.search.similarities.Similarity
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory

import java.nio.file.Path
import java.nio.file.Paths

//import org.apache.lucene.queryparser.classic.QueryParser
class IndexCrisisClusterFromCSV {

	// Create Lucene index in this directory
	Path indexPath = Paths.get('indexes/crisis3FireBombFloodL6.6Classic')
	Path docsPath = Paths.get('Datasets/crisisData')
	Directory directory = FSDirectory.open(indexPath)
	Analyzer analyzer = //new EnglishAnalyzer();  //with stemming
	              new StandardAnalyzer();
	def catsFreq=[:]

	static main(args) {
		def i = new IndexCrisisClusterFromCSV()
		i.buildIndex()
	}

	def buildIndex() {
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		Similarity tfidf = new ClassicSimilarity()
		iwc.setSimilarity(tfidf)

		// Create a new index in the directory, removing any
		// previously indexed documents:
		iwc.setOpenMode(OpenMode.CREATE);

		IndexWriter writer = new IndexWriter(directory, iwc);

		Date start = new Date();
		println("Indexing to directory $indexPath ...");

		//	println "docsPath $docsPath parent" + docsPath.getParent()

		docsPath.toFile().eachFileRecurse {file ->

			def catName = file.getName().take(10)
			println "File: $file  CatName: $catName"

			file.splitEachLine(',') {fields ->

				def n = catsFreq.get((catName)) ?: 0
				if (n < 1000) {
					catsFreq.put((catName), n + 1)

					def textBody = fields[1]
					//def tweetID = fields[0]
					def doc = new Document()
					if (textBody!=" ")
						doc.add(new TextField(IndexInfo.FIELD_CONTENTS, textBody,  Field.Store.YES))

					Field catNameField = new StringField(IndexInfo.FIELD_CATEGORY_NAME, catName, Field.Store.YES);
					doc.add(catNameField)
					writer.addDocument(doc);
				}
			}
		}
		println "catsFreq $catsFreq"
		println "Total docs in index: ${writer.maxDoc()}"
		writer.close()
		println 'done...'
	}
}