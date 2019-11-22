package clusterExtension

import cluster.Analysis
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
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.*
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory

import java.nio.file.Path
import java.nio.file.Paths

@CompileStatic
class AddFieldToIndex {

    static void main(String[] args) {

        Indexes.setIndex(IndexEnum.R4Train)
        String indexPath = Indexes.indexEnum.pathString
        println "indexPath $indexPath"

        Path path = Paths.get(indexPath)
        Directory directory = FSDirectory.open(path)
        Analyzer analyzer = new StandardAnalyzer()
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer)

        iwc.setOpenMode(IndexWriterConfig.OpenMode.APPEND)
        IndexWriter writer = new IndexWriter(directory, iwc)

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
        TopDocs topDocsAll = Indexes.indexSearcher.search(qAll, Integer.MAX_VALUE)
        ScoreDoc[] hitsAll = topDocsAll.scoreDocs

        for (ScoreDoc sd : hitsAll) {

            Document d = Indexes.indexSearcher.doc(sd.doc)

            d.removeField(Indexes.FIELD_ASSIGNED_CLASS)
            Field assignedClass = new StringField(Indexes.FIELD_ASSIGNED_CLASS, 'unassigned', Field.Store.YES);
            d.add(assignedClass)

            Term t = new Term(Indexes.FIELD_DOCUMENT_ID, d.get(Indexes.FIELD_DOCUMENT_ID))
            writer.updateDocument(t, d)
        }

        writer.forceMerge(1)
        writer.commit()


        Map<Query, String> qMap = [:]
        // [(godQ): 'soc.religion.christian', (gameQ): 'rec.sport.hockey', (spaceQ): 'sci.space', (qAll): 'allD' ]
        // Map<Query, String> qMap = [:]
        //   [(godQ): 'soc.religion.christian', (gameQ): 'rec.sport.hockey', (spaceQ): 'sci.space']
        int counter = 0

        println "At start numdocs: " + writer.numDocs()

        File queryData = new File('results/qdata.txt')
        List<Query> queries = []

        QueryParser parser = new QueryParser(Indexes.FIELD_CONTENTS, Indexes.analyzer)

        queryData.eachLine { String line ->

            println "line $line"
            //  String word0 = line.substring(0, line.indexOf(' ')).trim();
            //    println "word0 $word0"
            Query qn = parser.parse(line)
            //queries << qn
            //   qMap.put(qn, word0)
            queries << qn
            println "qn $qn"
        }

        List<Query> uniqueQueries = []
        for (int i = 0; i < queries.size(); i++) {
            Query q = queries[i]

            BooleanQuery.Builder bqbOneCategoryOnly = new BooleanQuery.Builder()
            bqbOneCategoryOnly.add(q, BooleanClause.Occur.SHOULD)

            for (int j = 0; j < queries.size(); j++) {
                if (j != i) {
                    bqbOneCategoryOnly.add(queries[j], BooleanClause.Occur.MUST_NOT)
                }
            }
            uniqueQueries << bqbOneCategoryOnly.build()
        }

        println "qmap before $qMap"
        println "queries $queries"
        println "uniqueries $uniqueQueries"

        uniqueQueries.each { q ->
     //   queries.each { q ->
            def t3 = Analysis.getMostFrequentCategoryForQuery(q)
            String cname = t3.first
            println "cname $cname"
            qMap.put(q, cname)
        }

        println "qmap after $qMap"


        //qmap - only if unique hit
        //  if (false)
        qMap.each { Query query, String name ->

            TopDocs topDocs = Indexes.indexSearcher.search(query, Integer.MAX_VALUE)
            ScoreDoc[] hits = topDocs.scoreDocs

            for (ScoreDoc sd : hits) {

                Document d = Indexes.indexSearcher.doc(sd.doc)

                d.removeField(Indexes.FIELD_ASSIGNED_CLASS)
                Field assignedClass = new StringField(Indexes.FIELD_ASSIGNED_CLASS, name, Field.Store.YES);
                d.add(assignedClass)

                Term t = new Term(Indexes.FIELD_DOCUMENT_ID, d.get(Indexes.FIELD_DOCUMENT_ID))

                writer.updateDocument(t, d)
                counter++
            }
        }

        println "$counter docs updated"
        println "Max docs: " + writer.maxDoc() + " numDocs: " + writer.numDocs()

        Indexes.showCategoryFrequenies()

        writer.forceMerge(1)
        writer.commit()
        println "Max docs: " + writer.maxDoc() + " numDocs: " + writer.numDocs()


        writer.close()
        Indexes.showCategoryFrequenies()
    }
}
