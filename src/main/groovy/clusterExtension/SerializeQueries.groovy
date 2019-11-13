package clusterExtension

import index.IndexEnum
import index.Indexes
import org.apache.lucene.document.Document
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopDocs

class SerializeQueries implements Serializable {

    public static void main(String[] args) {
        Indexes.setIndex(IndexEnum.NG3)

        File queryData = new File('results/qdata.txt')

        BooleanQuery.Builder bqb = new BooleanQuery.Builder();
        bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'god')), BooleanClause.Occur.SHOULD)
        bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'jesus')), BooleanClause.Occur.SHOULD)
        Query godQ = bqb.build() as Query

        bqb = new BooleanQuery.Builder();
        bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'team')), BooleanClause.Occur.SHOULD)
        bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'play')), BooleanClause.Occur.MUST)
        Query gameQ = bqb.build()

        List queries = []
        List q2 = []

        queries.add(godQ)
        queries.add(gameQ)
        String gameS = gameQ.toString(Indexes.FIELD_CONTENTS)

        queryData.text = ''
        queries.each { q ->
            queryData << q.toString(Indexes.FIELD_CONTENTS) + '\n'
        }
        QueryParser parser = new QueryParser(Indexes.FIELD_CONTENTS, Indexes.analyzer)

        queryData.eachLine { line ->
            println "line $line"
            Query qn = parser.parse(line)
            println "qn $qn"
        }

        //Query q = new QueryParser(Version.LUCENE_42, "title", analyzer).parse(querystr);


        Query qg = parser.parse(gameS)

        println "qg $qg"

        println "queries $queries"


        TopDocs topDocs = Indexes.indexSearcher.search(qg, Integer.MAX_VALUE)
        ScoreDoc[] hits = topDocs.scoreDocs

        for (ScoreDoc sd : hits) {
            Document d = Indexes.indexSearcher.doc(sd.doc)
            // println "hits " + d.get(Indexes.FIELD_PATH)

        }

    }

}
