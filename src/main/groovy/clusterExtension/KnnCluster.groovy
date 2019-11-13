package clusterExtension

import index.IndexEnum
import index.Indexes
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.classification.document.KNearestNeighborDocumentClassifier
import org.apache.lucene.classification.utils.ConfusionMatrixGenerator
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
        Indexes.setIndex(IndexEnum.NG3)

        Map<String, Analyzer> analyzerPerField = new HashMap<String, Analyzer>();

        analyzerPerField.put(Indexes.FIELD_CATEGORY_NAME, new StandardAnalyzer());
        analyzerPerField.put(Indexes.FIELD_TEST_TRAIN, new StandardAnalyzer());
        analyzerPerField.put(Indexes.FIELD_CONTENTS, new StandardAnalyzer());

        TopDocs testTopDocs = Indexes.indexSearcher.search(Indexes.testQ, 440)
        ScoreDoc[] testHits = testTopDocs.scoreDocs;

        TermQuery assignedTQ = new TermQuery(new Term(Indexes.FIELD_ASSIGNED_CLASS, 'unAssigned'))
        BooleanQuery.Builder bqb = new BooleanQuery.Builder()
        bqb.add(new MatchAllDocsQuery(), BooleanClause.Occur.SHOULD);
        //  bqb.add(new BooleanClause(new MatchAllDocsQuery(), BooleanClause.Occur.SHOULD));
        bqb.add(assignedTQ, BooleanClause.Occur.MUST_NOT)
        Query assignedQ = bqb.build()

        KNearestNeighborDocumentClassifier knnClassifier = new KNearestNeighborDocumentClassifier(
                Indexes.indexReader,
                new BM25Similarity(),
                //  new ClassicSimilarity(),
                assignedQ,//   Indexes.trainQ,  //matchAll
                3,//Indexes.indexEnum.getNumberOfCategories(),  //k from cluster
                3,
                1,
                // Indexes.FIELD_CATEGORY_NAME,
                Indexes.FIELD_ASSIGNED_CLASS,
                analyzerPerField,
                Indexes.FIELD_CONTENTS)

        for (ScoreDoc testd : testHits) {
            Document d = Indexes.indexSearcher.doc(testd.doc)

            def path = d.get(Indexes.FIELD_PATH)
            def categoryName = d.get(Indexes.FIELD_CATEGORY_NAME)
            def assignedClass = knnClassifier.assignClass(d)
            def assignedClassString = assignedClass.getAssignedClass().utf8ToString()
            def assig = d.get(Indexes.FIELD_ASSIGNED_CLASS)

            if (assignedClassString != categoryName) {
                println "classsification error ++++++++++++++++++++ path $path categoryName $categoryName assigned to: $assignedClassString asssig $assig"
            }
        }

        assert knnClassifier

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

        println "f1: $f1 precision: $precisiion recall: $recall"

        def p = confusionMatrix.getLinearizedMatrix()

        println "p $p"
    }
}
