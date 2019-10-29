package clusterExtension

import index.IndexEnum
import index.Indexes
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.classification.*
import org.apache.lucene.classification.document.SimpleNaiveBayesDocumentClassifier
import org.apache.lucene.classification.utils.ConfusionMatrixGenerator
import org.apache.lucene.classification.utils.*
import org.apache.lucene.document.Document
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopDocs
import org.apache.lucene.util.BytesRef

class LuceneClassifier {

    static void main(String[] args) {
        Indexes.instance.setIndex(IndexEnum.NG3N)

        Map<String, Analyzer> analyzerPerField = new HashMap<String, Analyzer>();

        analyzerPerField.put(Indexes.FIELD_CATEGORY_NAME, new StandardAnalyzer());
        analyzerPerField.put(Indexes.FIELD_TEST_TRAIN, new StandardAnalyzer());
        analyzerPerField.put(Indexes.FIELD_CONTENTS, new StandardAnalyzer());

        TopDocs testTopDocs = Indexes.indexSearcher.search(Indexes.testQ, 400)
        ScoreDoc[] testHits = testTopDocs.scoreDocs;

     //   SimpleNaiveBayesDocumentClassifier  snbdc =
        org.apache.lucene.classification.Classifier<BytesRef> snbdc =
                new SimpleNaiveBayesDocumentClassifier(Indexes.indexReader,
                        Indexes.trainQ,
                        Indexes.FIELD_CATEGORY_NAME,  //.FIELD_ASSIGNED_CLASS,
                        analyzerPerField,
                        Indexes.FIELD_CONTENTS)

        println snbdc

        for (ScoreDoc testd : testHits) {
            Document d = Indexes.indexSearcher.doc(testd.doc)

            def assignedClass = snbdc.assignClass(d)

            def path = d.get(Indexes.FIELD_PATH)
            def cat = d.get(Indexes.FIELD_CATEGORY_NAME)

            def assignedClassString= assignedClass.getAssignedClass().utf8ToString()

            if (assignedClassString != cat) {
                println "classsification error ++++++++++++++++++++ path $path cat $cat assig $assignedClassString"

            }
        }


        assert snbdc
        println "snbdc class " + snbdc.class

       ConfusionMatrixGenerator.ConfusionMatrix  confusionMatrix =
                ConfusionMatrixGenerator.getConfusionMatrix(Indexes.indexReader,
                        snbdc ,
                        Indexes.FIELD_CATEGORY_NAME,
                        Indexes.FIELD_CONTENTS,
                        100000)

        //     double f1Measure = confusionMatrix.getF1Measure();

        //  println "f1 $f1Measure"

    }
}
