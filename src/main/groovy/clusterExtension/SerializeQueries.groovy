package clusterExtension

import index.Indexes
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery

class SerializeQueries implements Serializable  {

    public static void main(String[] args){
        BooleanQuery.Builder bqb = new BooleanQuery.Builder();
        bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'god')), BooleanClause.Occur.SHOULD)
        bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'jesus')), BooleanClause.Occur.SHOULD)
        Query godQ = bqb.build() as Query

        bqb = new BooleanQuery.Builder();
        bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'team')), BooleanClause.Occur.SHOULD)
        bqb.add(new TermQuery(new Term(Indexes.FIELD_CONTENTS, 'play')), BooleanClause.Occur.MUST)
        Query gameQ = bqb.build()

        List queries  = []
        List q2 =[]

        queries.add(godQ)
        queries.add(gameQ)

        println "queries $queries"
        File f = new File('q')
        f.withObjectOutputStream {q1 ->
            q1.writeObject(queries)
        }

        f.withObjectInputStream {qin ->
         def q4 = qin.readObject()
            q2= q4
        }

        println "q2 $q2"
    }

}
