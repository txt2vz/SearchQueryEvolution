package clusterExtension

import index.IndexEnum
import index.Indexes
import org.apache.lucene.document.Document
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopDocs


class FileNamesFromQuery {

     static void main(String[] args) {

         Indexes.instance.setIndex(IndexEnum.NG3)
         IndexSearcher searcher = Indexes.indexSearcher
         BooleanQuery.Builder bqb = new BooleanQuery.Builder();
         bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS,'nasa')), BooleanClause.Occur.SHOULD)
         Query q =bqb.build()

         TopDocs t0TopDocs = Indexes.indexSearcher.search(q, 10000)
         ScoreDoc[] t0Hits = t0TopDocs.scoreDocs;

         for (ScoreDoc sd: t0Hits){
             Document d = searcher.doc(sd.doc)
             println "path " + d.get(Indexes.FIELD_PATH)
             println "Category " + d.get(Indexes.FIELD_CATEGORY_NAME)
             println "test train " + d.get(Indexes.FIELD_TEST_TRAIN)
             println ""
         }
    }
}
