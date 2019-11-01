package clusterExtension

import index.IndexEnum
import index.Indexes
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.classification.document.KNearestNeighborDocumentClassifier
import org.apache.lucene.classification.utils.ConfusionMatrixGenerator
import org.apache.lucene.document.Document
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TopDocs
import org.apache.lucene.search.similarities.BM25Similarity
import org.apache.lucene.search.similarities.ClassicSimilarity

class Knn {
    //  http://lucene.apache.org/core/7_4_0/classification/index.html

    static void main(String[] args) {
        Indexes.instance.setIndex(IndexEnum.NG20)

        Map<String, Analyzer> analyzerPerField = new HashMap<String, Analyzer>();

        analyzerPerField.put(Indexes.FIELD_CATEGORY_NAME, new StandardAnalyzer());
        analyzerPerField.put(Indexes.FIELD_TEST_TRAIN, new StandardAnalyzer());
        analyzerPerField.put(Indexes.FIELD_CONTENTS, new StandardAnalyzer());

        TopDocs testTopDocs = Indexes.indexSearcher.search(Indexes.testQ, 40)
        ScoreDoc[] testHits = testTopDocs.scoreDocs;
        KNearestNeighborDocumentClassifier knnClassifier = new KNearestNeighborDocumentClassifier(
                Indexes.indexReader,
                new BM25Similarity(),
              //  new ClassicSimilarity(),
                Indexes.trainQ,
                Indexes.indexEnum.getNumberOfCategories(),
                4,
                4,
                Indexes.FIELD_CATEGORY_NAME,
                analyzerPerField,
                Indexes.FIELD_CONTENTS)

        for (ScoreDoc testd : testHits) {
            Document d = Indexes.indexSearcher.doc(testd.doc)

            def path = d.get(Indexes.FIELD_PATH)
            def categoryName = d.get(Indexes.FIELD_CATEGORY_NAME)
            def assignedClass = knnClassifier.assignClass(d)
            def assignedClassString= assignedClass.getAssignedClass().utf8ToString()

            if (assignedClassString != categoryName) {
                println "classsification error ++++++++++++++++++++ path $path categoryName $categoryName assigned to: $assignedClassString"
            }
        }

        assert knnClassifier

        ConfusionMatrixGenerator.ConfusionMatrix  confusionMatrix =
                ConfusionMatrixGenerator.getConfusionMatrix(Indexes.indexReader,
                        knnClassifier ,
                        Indexes.FIELD_CATEGORY_NAME,
                        Indexes.FIELD_CONTENTS,
                        -1)

        def f1 = confusionMatrix.getF1Measure()
        def precisiion = confusionMatrix.getPrecision()
        def recall = confusionMatrix.getRecall()

        println "f1: $f1 precision: $precisiion recall: $recall"
    }
}
