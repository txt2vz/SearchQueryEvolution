package clusterExtension

import index.IndexEnum
import index.Indexes
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.search.*
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory

import java.nio.file.Path
import java.nio.file.Paths

class AddFieldToIndex {

     static void main(String[] args) {

         String indexPath = 'indexes/science4'

         Path path = Paths.get(indexPath)
         Directory directory = FSDirectory.open(path)
         Analyzer analyzer =  new StandardAnalyzer()
         IndexWriterConfig iwc = new IndexWriterConfig(analyzer)


// Create a new index in the directory, removing any
// previously indexed documents:
         iwc.setOpenMode(IndexWriterConfig.OpenMode.APPEND)
         IndexWriter writer = new IndexWriter(directory, iwc)

         File outFile = new File ('results/docsMatchingQuery.csv')
         Indexes.instance.setIndex(IndexEnum.NG3)

         //create query 'nasa' OR 'space'
         BooleanQuery.Builder bqb = new BooleanQuery.Builder();
         bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS,'space')), BooleanClause.Occur.SHOULD)
         bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS,'nasa')), BooleanClause.Occur.SHOULD)
         Query q = bqb.build()

         String queryString = q.toString(Indexes.FIELD_CONTENTS)

         TopDocs topDocs = Indexes.indexSearcher.search(q, Integer.MAX_VALUE)
         ScoreDoc[] hits = topDocs.scoreDocs

         outFile.write 'documentPath, category, testTrain, query \n'

         for (ScoreDoc sd: hits){
             Document d = Indexes.indexSearcher.doc(sd.doc)

             Field assignedClass = new StringField(Indexes.FIELD_ASSIGNED_CLASS, 'om', Field.Store.YES);
             d.add(assignedClass)

            def p =  d.getField(Indexes.FIELD_PATH)

             println "p " + p.stringValue()

           //  def a = d.getField(Indexes.FIELD_ASSIGNED_CLASS)
           //  println "a $a"

          //   d.add(a)

             writer.updateDocument(new Term(Indexes.FIELD_PATH, p.stringValue()),d)
//writer.commit()
        //     writer.close()
          //   String pathN = d.get(Indexes.FIELD_PATH)
          //   String category = d.get(Indexes.FIELD_CATEGORY_NAME)
          //   String testTrain =  d.get(Indexes.FIELD_TEST_TRAIN)

           //  println "pathN $pathN category: $category testTrain: $testTrain query: $queryString"

             //outFile << "$path, $category, $testTrain, $queryString \n"

         }
         writer.close()
    }



}
