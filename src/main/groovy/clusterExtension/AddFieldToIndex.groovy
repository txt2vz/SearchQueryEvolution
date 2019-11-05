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

        String indexPath = 'indexes/NG3'
                //'indexes/science4'

        Path path = Paths.get(indexPath)
        Directory directory = FSDirectory.open(path)
        Analyzer analyzer = new StandardAnalyzer()
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer)

// Create a new index in the directory, removing any
// previously indexed documents:
        iwc.setOpenMode(IndexWriterConfig.OpenMode.APPEND)
        IndexWriter writer = new IndexWriter(directory, iwc)

        File outFile = new File('results/docsMatchingQuery.csv')
        Indexes.instance.setIndex(IndexEnum.NG3)

        //create query 'nasa' OR 'space'
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

       // String queryString = godQ.toString(Indexes.FIELD_CONTENTS)

        Query qAll = new MatchAllDocsQuery()

        Map <Query, String> qMap = [(godQ): 'god', (gameQ): 'game', (spaceQ): 'space']
        int counter = 0

        qMap.each { query, name ->

            TopDocs topDocs = Indexes.indexSearcher.search(query, Integer.MAX_VALUE)
            ScoreDoc[] hits = topDocs.scoreDocs

            outFile.write 'documentPath, category, testTrain, query \n'

            for (ScoreDoc sd : hits) {
                Document d = Indexes.indexSearcher.doc(sd.doc)

                Field assignedClass = new StringField(Indexes.FIELD_ASSIGNED_CLASS, name, Field.Store.YES);
                d.add(assignedClass)

                def p = d.getField(Indexes.FIELD_PATH)

                writer.updateDocument(new Term(Indexes.FIELD_PATH, p.stringValue()), d)
                counter++
            }
        }
        println "$counter docs updated"

        writer.close()
    }
}
