package index

import index.IndexEnum
import index.IndexUtils
import index.Indexes
import org.apache.lucene.classification.Classifier
import org.apache.lucene.classification.utils.ConfusionMatrixGenerator
import org.apache.lucene.index.Term
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TotalHitCountCollector

class Effectiveness {

    static Tuple4 querySetEffectiveness(Set<Query> querySet) {

        List<Double> f1list = [], precisionList = [], recallList = [], fitnessList = []

        querySet.each { Query q ->

            Tuple3 tuple3 = IndexUtils.getMostFrequentCategoryForQuery(q)
            String mostFrequentCategoryName = tuple3.first
            final int mostFrequentCategoryHitsSize = tuple3.second
            final int queryHitsSize = tuple3.third

            double recall = 0
            double precision = 0
            double f1 = 0
            int categoryTotal = 0
            TermQuery mostFrequentCategoryTermQuery = new TermQuery(new Term(Indexes.FIELD_CATEGORY_NAME,
                    mostFrequentCategoryName))

            if (mostFrequentCategoryHitsSize && queryHitsSize && mostFrequentCategoryTermQuery) {
                TotalHitCountCollector totalHitCollector = new TotalHitCountCollector()
                Indexes.indexSearcher.search(mostFrequentCategoryTermQuery, totalHitCollector);
                categoryTotal = totalHitCollector.getTotalHits()

                assert categoryTotal

                recall = (double) mostFrequentCategoryHitsSize / categoryTotal
                precision = (double) mostFrequentCategoryHitsSize / queryHitsSize
                f1 = (2 * precision * recall) / (precision + recall)
            }

            f1list << f1
            precisionList << precision
            recallList << recall
        }

        final int maxCategoriesClusters = Math.max(Indexes.index.numberOfCategories, querySet.size())
        final double averageF1ForJob = (f1list) ? (double) f1list.sum() / maxCategoriesClusters : 0
        final double averageRecallForJob = (recallList) ? (double) recallList.sum() / maxCategoriesClusters : 0
        final double averagePrecisionForJob = (precisionList) ? (double) precisionList.sum() / maxCategoriesClusters : 0

        assert averageF1ForJob
        assert averageF1ForJob > 0

        return new Tuple4<Double, Double, Double, List<Double>>(averageF1ForJob, averagePrecisionForJob, averageRecallForJob, f1list)
    }


    static Tuple3<Double, Double, Double> classifierEffectiveness(Classifier classifier, IndexEnum testIndex, int k) {
        index.Indexes.setIndex(testIndex)

        ConfusionMatrixGenerator.ConfusionMatrix confusionMatrix =
                ConfusionMatrixGenerator.getConfusionMatrix(
                        Indexes.indexReader,
                        classifier,
                        Indexes.FIELD_CATEGORY_NAME,
                        Indexes.FIELD_CONTENTS,
                        -1)

        final double f1Lucene = confusionMatrix.getF1Measure()
        final double precisionLucene = confusionMatrix.getPrecision()
        final double recallLucene = confusionMatrix.getRecall()

        double f1return = f1Lucene, preturn = precisionLucene, rreturn = recallLucene
        final int numberOfEvaluatedDocs = confusionMatrix.getNumberOfEvaluatedDocs()

        println "Lucene classifier f1: $f1Lucene precisionLucene: $precisionLucene recallLucene: $recallLucene numberOfEvaluatedDocs $numberOfEvaluatedDocs"
        println "linearizedMatrix ${confusionMatrix.getLinearizedMatrix()}"

        if (testIndex.numberOfCategories != k ) {

            List<Double> pList = []
            List<Double> rList = []
            Map categoriesMap = IndexUtils.categoryFrequencies(Indexes.indexSearcher)

            categoriesMap.keySet().each { categoryName ->
                pList << confusionMatrix.getPrecision(categoryName)
                rList << confusionMatrix.getRecall(categoryName)
            }

            final int maxCats = Math.max(k, testIndex.numberOfCategories)
            final double averagePk = pList.sum() / maxCats
            final double averageRk = rList.sum() / maxCats
            final double f1k = 2 * ((averagePk * averageRk) / (averagePk + averageRk))

            assert f1k

            f1return = f1k
            preturn = averagePk
            rreturn = averageRk

            println "plist $pList rlist $rList"
            println "avaeragepk $averagePk averagrK $averageRk f1k $f1k"
        }
        return new Tuple3(f1return, preturn, rreturn)
    }
}
