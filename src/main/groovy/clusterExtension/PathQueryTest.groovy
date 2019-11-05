package clusterExtension


import index.Indexes
import org.apache.lucene.document.Document
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.Term
import org.apache.lucene.search.*
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory

import java.nio.file.Path
import java.nio.file.Paths

class PathQueryTest {

    static void main(String[] args) {

        Path path = Paths.get('indexes/NG3')
        Directory directory = FSDirectory.open(path)
        IndexReader ir = DirectoryReader.open(directory)
        IndexSearcher is = new IndexSearcher(ir)

        Query qAll = new MatchAllDocsQuery()

    //    Map<String, Integer> assignedCategoryFrequencies = [:]

        Query qp = new TermQuery(new Term(Indexes.FIELD_PATH, 'D:\\Classify20NG3\\rec.sport.hockey\\53992'))

        int counter = 0

        TopDocs topDocsP = is.search(qp, Integer.MAX_VALUE)
        ScoreDoc[] hitsP = topDocsP.scoreDocs

        for (ScoreDoc sd : hitsP) {
            Document d = is.doc(sd.doc)

            println "d.path " + d.get(Indexes.FIELD_PATH)

        }

        println "maxdoc " + is.getIndexReader().maxDoc()
    }
}
