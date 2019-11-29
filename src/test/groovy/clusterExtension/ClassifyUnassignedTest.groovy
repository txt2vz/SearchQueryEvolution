package clusterExtension

import index.IndexEnum
import index.Indexes
import org.apache.lucene.classification.Classifier
import org.apache.lucene.document.Document
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.MatchAllDocsQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopDocs
import spock.lang.Specification

class ClassifyUnassignedTest extends Specification {

    def "CheckClassification"() {

        setup:
        Indexes.setIndex(IndexEnum.CLASSIC4TRAIN)

        TermQuery assignedTQ = new TermQuery(new Term(Indexes.FIELD_ASSIGNED_CLASS, 'unassigned'))
        BooleanQuery.Builder bqb = new BooleanQuery.Builder()
        bqb.add(new MatchAllDocsQuery(), BooleanClause.Occur.SHOULD);
        bqb.add(assignedTQ, BooleanClause.Occur.MUST_NOT)
        Query unassignedQ = bqb.build()

        TopDocs unAssignedTopDocs = Indexes.indexSearcher.search(unassignedQ, Indexes.indexReader.numDocs())
        ScoreDoc[] unAssignedHits = unAssignedTopDocs.scoreDocs

        TopDocs allTopDocs = Indexes.indexSearcher.search( new MatchAllDocsQuery(), Indexes.indexReader.numDocs())
        ScoreDoc[] allHits = allTopDocs.scoreDocs;
        println "unAssignedHits size " + unAssignedHits.size()

        int numberOfClusters = 4

        // Classifier classifier

        when:
        Classifier classifier = new ClassifyUnassigned().classifyUnassigned(IndexEnum.CLASSIC4TRAIN, LuceneClassifyMethod.KNN)
        int cnt = 0
        int error = 0
        Set <String> categories = [] as Set<String>
        for (ScoreDoc alld : allHits) {
            Document d = Indexes.indexSearcher.doc(alld.doc)
            cnt++
            def path = d.get(Indexes.FIELD_PATH)
            def categoryName = d.get(Indexes.FIELD_CATEGORY_NAME)
            categories << categoryName

            def assignedClass = classifier.assignClass(d.get(Indexes.FIELD_CONTENTS))
            def assignedClassString = assignedClass.getAssignedClass().utf8ToString()
            def assig = d.get(Indexes.FIELD_ASSIGNED_CLASS)

            if (categoryName == 'cran' && assignedClassString != categoryName) {
                error++
            }
        }

        println "cnt $cnt error $error"
        println "cateogoreis $categories"


        Effectiveness.classifierEffectiveness(classifier, IndexEnum.CLASSIC4TEST, 4)

        then:
        2 == 2
    }

}
