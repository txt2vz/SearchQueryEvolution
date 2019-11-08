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

        Indexes.setIndex(IndexEnum.NG3)

        String indexPath =  Indexes.indexEnum.pathString

        println "indexPath $indexPath"
                //'indexes/NG3'
                //'indexes/science4'

        Path path = Paths.get(indexPath)
        Directory directory = FSDirectory.open(path)
        Analyzer analyzer = new StandardAnalyzer()
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer)

// Create a new index in the directory, removing any
// previously indexed documents:
        iwc.setOpenMode(IndexWriterConfig.OpenMode.APPEND)
        IndexWriter writer = new IndexWriter(directory, iwc)

        println "at start max doc " + writer.numDocs()

        //File outFile = new File('results/docsMatchingQuery.csv')


        BooleanQuery.Builder bqb = new BooleanQuery.Builder();
        bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'god')), BooleanClause.Occur.SHOULD)
        bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'jesus')), BooleanClause.Occur.SHOULD)
        Query godQ = bqb.build()

        bqb = new BooleanQuery.Builder();
        bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'game')), BooleanClause.Occur.SHOULD)
        bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'hockey')), BooleanClause.Occur.SHOULD)
        Query gameQ = bqb.build()

        bqb = new BooleanQuery.Builder();
        bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'space')), BooleanClause.Occur.SHOULD)
        bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'orbit')), BooleanClause.Occur.SHOULD)
        Query spaceQ = bqb.build()

        Query qAll = new MatchAllDocsQuery()
       Map <Query, String> qMap = [(godQ): 'soc.religion.christian', (gameQ): 'rec.sport.hockey', (spaceQ): 'sci.space', (qAll): 'allD' ]
     //   Map <Query, String> qMap = [(godQ): 'soc.religion.christian', (gameQ): 'rec.sport.hockey', (spaceQ): 'sci.space' ]
        int counter = 0

        println "at start numdocs " + writer.numDocs()

        Query aAll = new MatchAllDocsQuery()

        qMap.each { query, name ->

            TopDocs topDocs = Indexes.indexSearcher.search(query, Integer.MAX_VALUE)
            ScoreDoc[] hits = topDocs.scoreDocs

            for (ScoreDoc sd : hits) {

                Document d = Indexes.indexSearcher.doc(sd.doc)
                d.removeField(Indexes.FIELD_ASSIGNED_CLASS)

                Field assignedClass = new StringField(Indexes.FIELD_ASSIGNED_CLASS, name, Field.Store.YES);
                d.add(assignedClass)

              //  String p = d.get(Indexes.FIELD_PATH)
            //    String p2 = p

              //  println "p $p"
                Term t = new Term(Indexes.FIELD_PATH, d.get(Indexes.FIELD_PATH))
                writer.updateDocument(t,d)

             //   writer.updateDocument(new Term(Indexes.FIELD_PATH, p2), doc)
                counter++
            }
        }

        writer.commit()
        println "$counter docs updated"
        println "Max docs: " + writer.maxDoc() + " numDocs " + writer.numDocs()
        writer.close()
    }
}
