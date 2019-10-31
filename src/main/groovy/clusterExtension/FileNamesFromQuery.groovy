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

         File outFile = new File ('results/docsMatchingQuery.csv')
         Indexes.instance.setIndex(IndexEnum.NG3)
         BooleanQuery.Builder bqb = new BooleanQuery.Builder();
         bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS,'nasa')), BooleanClause.Occur.SHOULD)
         Query q = bqb.build()

         String queryString = q.toString(Indexes.FIELD_CONTENTS)

         TopDocs topDocs = Indexes.indexSearcher.search(q, Integer.MAX_VALUE)
         ScoreDoc[] hits = topDocs.scoreDocs

         outFile.write 'documentPath, category, testTrain, query \n'

         for (ScoreDoc sd: hits){
             Document d = Indexes.indexSearcher.doc(sd.doc)

             String path = d.get(Indexes.FIELD_PATH)
             String category = d.get(Indexes.FIELD_CATEGORY_NAME)
             String testTrain =  d.get(Indexes.FIELD_TEST_TRAIN)

             println "path $path category: $category testTrain: $testTrain"

             outFile << "$path, $category, $testTrain, $queryString \n"
         }
    }
}
