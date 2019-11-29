package clusterExtension

import index.IndexEnum
import index.IndexUtils
import index.Indexes
import org.apache.lucene.classification.Classifier
import org.apache.lucene.classification.utils.ConfusionMatrixGenerator
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.Term
import org.apache.lucene.index.TermsEnum
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TotalHitCountCollector

class Effectiveness {

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
        final int numberOfEvaluatedDocs = confusionMatrix.getNumberOfEvaluatedDocs()

        println "ConfusionMatrix f1: $f1Lucene precisionLucene: $precisionLucene recallLucene: $recallLucene numberOfEvaluatedDocs $numberOfEvaluatedDocs"

        Map<String, Map<String, Long>> linearizedMatrix = confusionMatrix.getLinearizedMatrix()
        println "linearizedMatrix $linearizedMatrix"


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

            println "plist $pList rlist $rList"
            println "avaeragepk $averagePk averagrK $averageRk f1k $f1k"
             assert f1k == f1Lucene
        }







//        def f1cran = confusionMatrix.getF1Measure('cran')
//        def f1med = confusionMatrix.getF1Measure('med')
//        def f1cacm = confusionMatrix.getF1Measure('cacm')
//        def f1cisi = confusionMatrix.getF1Measure('cisi')
//
//
//        def pcran = confusionMatrix.getPrecision('cran')
//        def pmed = confusionMatrix.getPrecision('med')
//        def pcacm = confusionMatrix.getPrecision('cacm')
//        def pcisi = confusionMatrix.getPrecision('cisi')
//
//        def rcran = confusionMatrix.getRecall('cran')
//        def rmed = confusionMatrix.getRecall('med')
//        def rcacm = confusionMatrix.getRecall('cacm')
//        def rcisi = confusionMatrix.getRecall('cisi')
//
//
//        //   def e = confusionMatrix.getF1Measure('earn')
//        def cp = confusionMatrix.getPrecision('cran')
//        def cr = confusionMatrix.getRecall('cran')
//
//        //   println "e $e f1cran $f1cran cp $cp cr $cr"
//        println "f1 cran $f1cran  p $cp r $cr"
//        println "f1cran $f1cran f1med $f1med f1cacm $f1cacm f1cisi $f1cisi"
//        println "pcran $pcran pmed $pmed pcacm $pcacm pcisi $pcisi"
//        println "rcran $rcran rmed $rmed rcacm $rcacm rcisi $rcisi"
//
//        def numberOfClasses = 5
//
//        def f1s = [f1cran, f1med, f1cacm, f1cisi]
//        def ps = [pcacm, pcran, pcisi, pmed]
//        def rs = [rcran, rmed, rcacm, rcisi]
//
//        double rs1 = rs.sum() /rs.size()
//
//        println "average recall $rs1"
//
//        double f1a = (f1cran + f1med + f1cacm + f1cisi) / 4
//        def av = f1s.sum() / numberOfClasses
//        def avps = ps.sum() / numberOfClasses
//        def avrs = rs.sum() /numberOfClasses
//
//        println "f1a $f1a  av $av"
//        println "ps $ps avps $avps"
//
//        println "rs $rs  avrs $avrs"
//        def f1b = 2 * ((avps * avrs) / (avps + avrs))
//
//                //2 * ((precision * recall) / (precision + recall))
//        println "f1b $f1b"



        Tuple3<Double, Double, Double> t3 = new Tuple3(f1Lucene, precisionLucene, recallLucene)
        return t3
    }
}
