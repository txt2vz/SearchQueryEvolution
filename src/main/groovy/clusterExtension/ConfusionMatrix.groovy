package clusterExtension

import index.IndexEnum
import index.Indexes
import org.apache.lucene.classification.Classifier
import org.apache.lucene.classification.utils.ConfusionMatrixGenerator
import org.apache.lucene.index.Term
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TotalHitCountCollector

class ConfusionMatrix {

    static void checkClassifier(Classifier classifier, IndexEnum ie) {
        index.Indexes.setIndex(ie)

        ConfusionMatrixGenerator.ConfusionMatrix confusionMatrix =
                ConfusionMatrixGenerator.getConfusionMatrix(
                        Indexes.indexReader,
                        classifier,
                        Indexes.FIELD_CATEGORY_NAME,
                        Indexes.FIELD_CONTENTS,
                        -1)

        final double f1 = confusionMatrix.getF1Measure()
        final double precisiion = confusionMatrix.getPrecision()
        final double recall = confusionMatrix.getRecall()
        final int numberOfEvaluatedDocs = confusionMatrix.getNumberOfEvaluatedDocs()

        println "f1: $f1 precision: $precisiion recall: $recall numberOfEvaluatedDocs $numberOfEvaluatedDocs"

        Map<String,Map<String,Long>> linearizedMatrix = confusionMatrix.getLinearizedMatrix()

        println "linearizedMatrix $linearizedMatrix"
    }
}
