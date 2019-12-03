package clusterExtension

import index.IndexEnum
import index.Indexes
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.classification.document.KNearestNeighborDocumentClassifier
import org.apache.lucene.document.Document
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.MatchAllDocsQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopDocs
import org.apache.lucene.search.similarities.BM25Similarity

class KnnCluster {
    //  http://lucene.apache.org/core/7_4_0/classification/index.html

    static void main(String[] args) {
        //Indexes.setIndex(IndexEnum.NG3)
        Indexes.setIndex(IndexEnum.R4Train)

        Map<String, Analyzer> analyzerPerField = new HashMap<String, Analyzer>();

        analyzerPerField.put(Indexes.FIELD_CATEGORY_NAME, new StandardAnalyzer());
        analyzerPerField.put(Indexes.FIELD_TEST_TRAIN, new StandardAnalyzer());
        analyzerPerField.put(Indexes.FIELD_CONTENTS, new StandardAnalyzer());

        Query qAll = new MatchAllDocsQuery()

        TopDocs allTopDocs = Indexes.indexSearcher.search(qAll, Indexes.indexReader.numDocs())
        ScoreDoc[] allHits = allTopDocs.scoreDocs;

        TopDocs testTopDocs = Indexes.indexSearcher.search(Indexes.testQ, Indexes.indexReader.numDocs())
        ScoreDoc[] testHits = testTopDocs.scoreDocs;

        TermQuery assignedTQ = new TermQuery(new Term(Indexes.FIELD_ASSIGNED_CLASS, 'unassigned'))
        BooleanQuery.Builder bqb = new BooleanQuery.Builder()
        bqb.add(new MatchAllDocsQuery(), BooleanClause.Occur.SHOULD);
        bqb.add(assignedTQ, BooleanClause.Occur.MUST_NOT)
      //  bqb.add(Indexes.trainQ, BooleanClause.Occur.MUST )
        Query assignedQ = bqb.build()

        TopDocs assigTopDocs = Indexes.indexSearcher.search(assignedQ, Indexes.indexReader.numDocs())
        ScoreDoc[] assigHits = assigTopDocs.scoreDocs;

        println "assignhits  size " + assigHits.size()

        KNearestNeighborDocumentClassifier knnClassifier = new KNearestNeighborDocumentClassifier(
                Indexes.indexReader,
                new BM25Similarity(),
                //  new ClassicSimilarity(),
                assignedQ,//   Indexes.trainQ,  //matchAll
                5,//Indexes.indexEnum.getNumberOfCategories(),  //k from cluster
                3,
                1,
                // Indexes.FIELD_CATEGORY_NAME,
                Indexes.FIELD_ASSIGNED_CLASS,
                analyzerPerField,
                Indexes.FIELD_CONTENTS)

        int cnt = 0

    //    for (ScoreDoc testd : testHits) {
            for (ScoreDoc alld : allHits) {
            Document d = Indexes.indexSearcher.doc(alld.doc)
            cnt++
            def path = d.get(Indexes.FIELD_PATH)
            def categoryName = d.get(Indexes.FIELD_CATEGORY_NAME)
            def assignedClass = knnClassifier.assignClass(d)
            def assignedClassString = assignedClass.getAssignedClass().utf8ToString()
            def assig = d.get(Indexes.FIELD_ASSIGNED_CLASS)
            def testTrain = d.get(Indexes.FIELD_TEST_TRAIN)

            if (assignedClassString != categoryName ) {
          //  if (testTrain != 'testz'){
                println "classsification error ++++++++++++++++++++ path testTrain $testTrain $path categoryName $categoryName assigned to: $assignedClassString"

                //   println "classsification error ++++++++++++++++++++ path $path categoryName $categoryName assigned to: $assignedClassString asssig $assig"
            }
        }

        println "cnt $cnt"
        assert knnClassifier

        Effectiveness.classifierEffectiveness(knnClassifier, Indexes.index.R4Test )

      /*  Indexes.setIndex(Indexes.indexEnum.R4Test)

        ConfusionMatrixGenerator.ConfusionMatrix confusionMatrix =
                ConfusionMatrixGenerator.getConfusionMatrix(
                        Indexes.indexReader,
                        knnClassifier,
                        Indexes.FIELD_CATEGORY_NAME,
                        // Indexes.FIELD_ASSIGNED_CLASS,
                        Indexes.FIELD_CONTENTS,
                        -1)

        def f1 = confusionMatrix.getF1Measure()
        def precisiion = confusionMatrix.getPrecision()
        def recall = confusionMatrix.getRecall()

        def x = confusionMatrix.getNumberOfEvaluatedDocs()


        println "f1: $f1 precision: $precisiion recall: $recall x $x"

        def p = confusionMatrix.getLinearizedMatrix()

        println "p $p"

        TotalHitCountCollector trainCollector = new TotalHitCountCollector();
        final TermQuery trainQ = new TermQuery(new Term(Indexes.FIELD_TEST_TRAIN, "train"))

        TotalHitCountCollector testCollector = new TotalHitCountCollector();
        final TermQuery testQ = new TermQuery(new Term(Indexes.FIELD_TEST_TRAIN, "test"))

        Indexes.indexSearcher.search(Indexes.trainQ, trainCollector);
        def trainTotal = trainCollector.getTotalHits();

        Indexes.indexSearcher.search(testQ, testCollector);
        def testTotal = testCollector.getTotalHits();

        println "testTotal $testTotal trainTotal $trainTotal"

       */
    }
}
