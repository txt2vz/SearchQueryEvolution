package clusterExtension

import index.IndexEnum
import index.Indexes
import org.apache.lucene.document.Document
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.MatchAllDocsQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopDocs
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory

import java.nio.file.Path
import java.nio.file.Paths

class FileNamesFromQuery {

    static void main(String[] args) {

        Indexes.setIndex(IndexEnum.NG3)

     //   File outFile = new File('results/docsMatchingQuery.csv')
        //Indexes.instance.setIndex(IndexEnum.NG3)

        //create query 'nasa' OR 'space'
        //    BooleanQuery.Builder bqb = new BooleanQuery.Builder();
        //  bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS,'god')), BooleanClause.Occur.SHOULD)
        // bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS,'jesus')), BooleanClause.Occur.SHOULD)
        //  Query q = bqb.build()

//        Path path = Paths.get('indexes/NG3')
//        Directory directory = FSDirectory.open(path)
//        IndexReader ir = DirectoryReader.open(directory)
//        IndexSearcher is = new IndexSearcher(ir)

        IndexReader ir = Indexes.indexReader
        IndexSearcher is = new IndexSearcher(ir)
       // IndexSearcher is =



        Query qAll = new MatchAllDocsQuery()

        //String queryString = q.toString(Indexes.FIELD_CONTENTS)

        TopDocs topDocs = is.search(qAll, Integer.MAX_VALUE)
        ScoreDoc[] hits = topDocs.scoreDocs

        //  outFile.write 'documentPath, category, testTrain, query \n'

        Map<String, Integer> assignedCategoryFrequencies = [:]

        int counter = 0
        String p
        for (ScoreDoc sd : hits) {
            Document d = is.doc(sd.doc)

            String pathN = d.get(Indexes.FIELD_PATH)
            String category = d.get(Indexes.FIELD_CATEGORY_NAME)
            String testTrain = d.get(Indexes.FIELD_TEST_TRAIN)
            String assignedCat = d.get(Indexes.FIELD_ASSIGNED_CLASS)

            if (counter < 120)
                println "pathN $pathN category: $category testTrain: $testTrain  asssignedCat $assignedCat"

            int n = assignedCategoryFrequencies.get(assignedCat) ?: 0
            assignedCategoryFrequencies.put((assignedCat), n + 1)

            p = d.get(Indexes.FIELD_PATH)

            counter++
            // outFile << "$path, $category, $testTrain, $queryString \n"
        }

        println "assingedCatFreques $assignedCategoryFrequencies"
        println "maxdoc " + is.getIndexReader().maxDoc()
        println "numdoc " + is.getIndexReader().numDocs()
        println "p $p"


        String pt = 'C:\\Users\\aceslh\\Dataset\\20NG3SpaceHockeyChristian\\train\\soc.religion.christian\\20581'
        String pt2 = '52569' //                 /C:\Users\aceslh\Dataset\20NG3SpaceHockeyChristian\train\soc.religion.christian\20581/
        def x = new TermQuery(new Term(Indexes.FIELD_PATH, pt2))

        TopDocs topDocsP = is.search(x, Integer.MAX_VALUE)
        ScoreDoc[] hitsP = topDocsP.scoreDocs

        for (ScoreDoc sd : hitsP) {
            Document d = is.doc(sd.doc)

            def z = d.get(Indexes.FIELD_PATH)
            println "z $z"

        }

        Query qall = new MatchAllDocsQuery()

        TopDocs topDocsA = is.search(qAll, Integer.MAX_VALUE)
        ScoreDoc[] hitsA = topDocsA.scoreDocs

        def cnt = 0
        for (ScoreDoc sd : hitsA) {
            cnt++
        }
        println "cnt $cnt"
    }
}
