package clusterExtension

import index.IndexEnum
import index.Indexes
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.classification.document.SimpleNaiveBayesDocumentClassifier
import org.apache.lucene.document.Document
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopDocs

class LuceneClassifier {

    static void main(String[] args) {
        Indexes.instance.setIndex(IndexEnum.NG3N)
        BooleanQuery.Builder bqbTrain = new BooleanQuery.Builder()
        BooleanQuery.Builder bqbTest = new BooleanQuery.Builder();
        bqbTrain.add(new TermQuery(new Term(Indexes.FIELD_TEST_TRAIN, 'train')), BooleanClause.Occur.SHOULD)
        bqbTest.add(new TermQuery(new Term(Indexes.FIELD_TEST_TRAIN, 'test')), BooleanClause.Occur.SHOULD)

        Map<String, Analyzer> analyzerPerField = new HashMap<String, Analyzer>();
        analyzerPerField.put(Indexes.FIELD_TEST_TRAIN, new StandardAnalyzer());
        analyzerPerField.put(Indexes.FIELD_CONTENTS, new StandardAnalyzer());
       // analyzerPerField.put(Indexes.FIELD_ASSIGNED_CLASS, new StandardAnalyzer());

        Query trainQ = bqbTrain.build()
        Query testQ = bqbTest.build()

        TopDocs testTopDocs = Indexes.indexSearcher.search(testQ, 40)
        ScoreDoc[] testHits = testTopDocs.scoreDocs;

        SimpleNaiveBayesDocumentClassifier snbdc = new SimpleNaiveBayesDocumentClassifier(Indexes.indexReader,
                trainQ,
                Indexes.FIELD_ASSIGNED_CLASS,
                analyzerPerField,
                Indexes.FIELD_CONTENTS)

   //     snbdc.

       // snbdc.

        for (ScoreDoc testd: testHits){
            Document d = Indexes.indexSearcher.doc(testd.doc)

           def x = snbdc.assignClass(d)

            println "path " + d.get(Indexes.FIELD_PATH)
            println "Category " + d.get(Indexes.FIELD_CATEGORY_NAME)
            println "test train " + d.get(Indexes.FIELD_TEST_TRAIN)


            println "c " + d.get(Indexes.FIELD_ASSIGNED_CLASS)
            println "x $x"
            println " z " + x.getAssignedClass().utf8ToString()  //.assignedClass.getClass()

                   // ConfusionMatrixGenerator.ConfusionMatrix
       //      def ss = getConfusionMatrix(Indexes.indexReader,
            //        x,  Indexes.FIELD_ASSIGNED_CLASS, Indexes.FIELD_CONTENTS, 3000)


                     // Classifier<T> classifier,
                  //  String classFieldName,
                //    String textFieldName,
                 //   long timeoutMilliseconds)


        }
    }
}
