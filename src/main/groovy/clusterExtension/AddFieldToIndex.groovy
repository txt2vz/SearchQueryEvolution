package clusterExtension

import groovy.transform.CompileStatic
import index.IndexEnum
import index.Indexes

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.search.*
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory

import java.nio.file.Path
import java.nio.file.Paths

@CompileStatic

class AddFieldToIndex {

    static void main(String[] args) {

        Indexes.setIndex(IndexEnum.NG3)
        String indexPath =  Indexes.indexEnum.pathString
        println "indexPath $indexPath"

        Path path = Paths.get(indexPath)
        Directory directory = FSDirectory.open(path)
        Analyzer analyzer = new StandardAnalyzer()
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer)

        iwc.setOpenMode(IndexWriterConfig.OpenMode.APPEND)
        IndexWriter writer = new IndexWriter(directory, iwc)

        //File outFile = new File('results/docsMatchingQuery.csv')

        BooleanQuery.Builder bqb = new BooleanQuery.Builder();
        bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'god')), BooleanClause.Occur.SHOULD)
        bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'jesus')), BooleanClause.Occur.SHOULD)
        Query godQ = bqb.build() as Query

        bqb = new BooleanQuery.Builder();
        bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'team')), BooleanClause.Occur.SHOULD)
        bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'play')), BooleanClause.Occur.SHOULD)
        Query gameQ = bqb.build()

        bqb = new BooleanQuery.Builder();
        bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'space')), BooleanClause.Occur.SHOULD)
        bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'orbit')), BooleanClause.Occur.SHOULD)
        Query spaceQ = bqb.build()

        Query qAll = new MatchAllDocsQuery()
    //   Map <Query, String> qMap = [(godQ): 'soc.religion.christian', (gameQ): 'rec.sport.hockey', (spaceQ): 'sci.space', (qAll): 'allD' ]
        Map <Query, String> qMap = [(godQ): 'soc.religion.christian', (gameQ): 'rec.sport.hockey', (spaceQ): 'sci.space' ]
        int counter = 0

        println "At start numdocs: " + writer.numDocs()

        //qmap - only if unique hit

        qMap.each { Query query, String name ->

            TopDocs topDocs = Indexes.indexSearcher.search(query, Integer.MAX_VALUE)
            ScoreDoc[] hits = topDocs.scoreDocs

            for (ScoreDoc sd : hits) {

                Document d = Indexes.indexSearcher.doc(sd.doc)

                d.removeField(Indexes.FIELD_ASSIGNED_CLASS)
                Field assignedClass = new StringField(Indexes.FIELD_ASSIGNED_CLASS, name, Field.Store.YES);
                d.add(assignedClass)

                Term t = new Term(Indexes.FIELD_DOCUMENT_ID, d.get(Indexes.FIELD_DOCUMENT_ID))

                writer.updateDocument(t,d)
                counter++
            }
        }

        println "$counter docs updated"
        println "Max docs: " + writer.maxDoc() + " numDocs: " + writer.numDocs()

        writer.forceMerge(1)
        println "Max docs: " + writer.maxDoc() + " numDocs: " + writer.numDocs()
        writer.commit()
        writer.close()
    }
}
