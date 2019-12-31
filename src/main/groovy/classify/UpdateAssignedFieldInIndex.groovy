package classify

import groovy.transform.CompileStatic
import index.IndexEnum
import index.IndexUtils
import index.Indexes
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
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
class UpdateAssignedFieldInIndex {

    static void main(String[] args) {
        File queryData = new File('results/qFile.txt')
        updateAssignedField(IndexEnum.CRISIS3, queryData)
    }

    static void updateAssignedField(IndexEnum trainIndex, File queryFile, boolean onlyDocsInOneCluster=false){

        Indexes.setIndex(trainIndex)
        String indexPath = Indexes.index.pathString
        Path path = Paths.get(indexPath)
        Directory directory = FSDirectory.open(path)
        Analyzer analyzer = new StandardAnalyzer()
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer)
        iwc.setOpenMode(IndexWriterConfig.OpenMode.APPEND)
        IndexWriter indexWriter = new IndexWriter(directory, iwc)

        TopDocs topDocsAll = Indexes.indexSearcher.search(new MatchAllDocsQuery(), Integer.MAX_VALUE)
        ScoreDoc[] hitsAll = topDocsAll.scoreDocs

        for (ScoreDoc sd : hitsAll) {

            Document d = Indexes.indexSearcher.doc(sd.doc)

            d.removeField(Indexes.FIELD_ASSIGNED_CLASS)
            Field assignedClass = new StringField(Indexes.FIELD_ASSIGNED_CLASS, 'unassigned', Field.Store.YES);
            d.add(assignedClass)

            Term t = new Term(Indexes.FIELD_DOCUMENT_ID, d.get(Indexes.FIELD_DOCUMENT_ID))
            indexWriter.updateDocument(t, d)
        }

        indexWriter.forceMerge(1)
        indexWriter.commit()

        Map<Query, String> qMap = [:]

        List<Query> queries = []
        QueryParser parser = new QueryParser(Indexes.FIELD_CONTENTS, Indexes.analyzer)

        queryFile.eachLine { String line ->
            Query qn = parser.parse(line)
            queries << qn
        }

        if (onlyDocsInOneCluster) {
            List<Query> docInOneClusterQueries = []
            for (int i = 0; i < queries.size(); i++) {
                Query q = queries[i]

                BooleanQuery.Builder bqbOneCategoryOnly = new BooleanQuery.Builder()
                bqbOneCategoryOnly.add(q, BooleanClause.Occur.SHOULD)

                for (int j = 0; j < queries.size(); j++) {
                    if (j != i) {
                        bqbOneCategoryOnly.add(queries[j], BooleanClause.Occur.MUST_NOT)
                    }
                }
                docInOneClusterQueries << bqbOneCategoryOnly.build()
            }
            println "uniqueries $docInOneClusterQueries"
            queries = docInOneClusterQueries
        }

        queries.each { q ->
            String categoryName = IndexUtils.getMostFrequentCategoryForQuery(q).first
            qMap.put(q, categoryName)
        }

        int counter = 0
        qMap.each { Query query, String name ->

            TopDocs topDocs = Indexes.indexSearcher.search(query, Integer.MAX_VALUE)
            ScoreDoc[] hits = topDocs.scoreDocs

            for (ScoreDoc sd : hits) {

                Document d = Indexes.indexSearcher.doc(sd.doc)

                d.removeField(Indexes.FIELD_ASSIGNED_CLASS)
                Field assignedClass = new StringField(Indexes.FIELD_ASSIGNED_CLASS, name, Field.Store.YES);
                d.add(assignedClass)

                Term t = new Term(Indexes.FIELD_DOCUMENT_ID, d.get(Indexes.FIELD_DOCUMENT_ID))
                indexWriter.updateDocument(t, d)

                counter++
            }
        }

        println "$counter docs updated in UpdateAssignedFieldIndex"

        indexWriter.forceMerge(1)
        indexWriter.commit()
        println "Max docs: " + indexWriter.maxDoc() + " numDocs: " + indexWriter.numDocs()

        indexWriter.close()
        Indexes.setIndex(trainIndex)
        IndexUtils.categoryFrequencies(Indexes.indexSearcher)
    }
}
