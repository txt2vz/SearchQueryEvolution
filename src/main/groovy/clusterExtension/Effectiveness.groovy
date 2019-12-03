package clusterExtension

import cluster.ClusterFitness
import index.IndexEnum
import index.IndexUtils
import index.Indexes
import org.apache.lucene.classification.Classifier
import org.apache.lucene.classification.utils.ConfusionMatrixGenerator
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.Term
import org.apache.lucene.index.TermsEnum
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TotalHitCountCollector

class Effectiveness {

    static Tuple4 querySetEffectiveness (Set<Query> querySet ) {

        List<Double> f1list = [], precisionList = [], recallList = [], fitnessList = []

        querySet.each { Query q ->

           // String qString = q.toString(Indexes.FIELD_CONTENTS)

            def tuple3 = IndexUtils.getMostFrequentCategoryForQuery(q)
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


    static Tuple3<Double, Double, Double> classifierEffectiveness(Classifier classifier, IndexEnum ie, int k) {
        index.Indexes.setIndex(ie)

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

        double f1return, preturn, rreturn
        final int numberOfEvaluatedDocs = confusionMatrix.getNumberOfEvaluatedDocs()

        println "Lucene classifier f1: $f1Lucene precisionLucene: $precisionLucene recallLucene: $recallLucene numberOfEvaluatedDocs $numberOfEvaluatedDocs"
        println "linearizedMatrix ${confusionMatrix.getLinearizedMatrix()}"

        if (ie.numberOfCategories != k  || true){

            List <Double> pList = []
            List <Double> rList = []
            def cats = IndexUtils.categoryFrequencies(Indexes.indexSearcher)

            cats.keySet().each{categoryName ->
                pList << confusionMatrix.getPrecision(categoryName)
                rList <<  confusionMatrix.getRecall(categoryName)
            }

            def maxCats =  Math.max(k, ie.numberOfCategories)
            def averagePk = pList.sum() / maxCats
            def averageRk = rList.sum() / maxCats
            def f1k = 2 * ((averagePk * averageRk) / (averagePk + averageRk))

            assert f1k

            f1return = f1k
            preturn = averagePk
            rreturn = averageRk


            println "plist $pList rlist $rList"
            println "avaeragepk $averagePk averagrK $averageRk f1k $f1k"
         //    assert f1k == f1Lucene
        }


      //  Tuple3<Double, Double, Double> t3 = new Tuple3(f1Lucene, precisionLucene, recallLucene)

        Tuple3<Double, Double, Double> t3 = new Tuple3(f1return, preturn, rreturn)
        return t3
    }


}
