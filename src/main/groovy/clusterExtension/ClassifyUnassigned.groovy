package clusterExtension

import index.IndexEnum
import index.Indexes
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.classification.BM25NBClassifier
import org.apache.lucene.classification.KNearestNeighborClassifier
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.MatchAllDocsQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopDocs
import org.apache.lucene.search.similarities.BM25Similarity

class ClassifyUnassigned {

    static void main(String[] args) {
        //Indexes.setIndex(IndexEnum.NG3)
        Indexes.setIndex(IndexEnum.R4Train)

        TermQuery assignedTQ = new TermQuery(new Term(Indexes.FIELD_ASSIGNED_CLASS, 'unassigned'))
        BooleanQuery.Builder bqb = new BooleanQuery.Builder()
        bqb.add(new MatchAllDocsQuery(), BooleanClause.Occur.SHOULD);
        bqb.add(assignedTQ, BooleanClause.Occur.MUST_NOT)
        Query unassignedQ = bqb.build()

        TopDocs unAssignedTopDocs = Indexes.indexSearcher.search(unassignedQ, Indexes.indexReader.numDocs())
        ScoreDoc[] unAssignedHits = unAssignedTopDocs.scoreDocs;

        println "unAssignedHits size " + unAssignedHits.size()

//        BM25NBClassifier bm25NBClassifier = new BM25NBClassifier(
//                Indexes.indexReader,
//                new StandardAnalyzer(),
//                unassignedQ,
//                Indexes.FIELD_ASSIGNED_CLASS,
//                Indexes.FIELD_CONTENTS
//        )

        KNearestNeighborClassifier kNearestNeighborClassifier = new KNearestNeighborClassifier(
                Indexes.indexReader,
                new BM25Similarity(),
                new StandardAnalyzer(),
                unassignedQ,
                20,
                3,
                1,
                Indexes.FIELD_ASSIGNED_CLASS,
                Indexes.FIELD_CONTENTS
        )

     //   ConfusionMatrix.checkClassifier(bm25NBClassifier, Indexes.indexEnum.R4Test )
        ConfusionMatrix.checkClassifier(kNearestNeighborClassifier, Indexes.indexEnum.R4Test )
    }
}
