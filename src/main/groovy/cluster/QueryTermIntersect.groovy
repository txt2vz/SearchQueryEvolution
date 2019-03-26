package cluster

import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopDocs
import org.apache.lucene.search.TotalHitCountCollector

@CompileStatic
class QueryTermIntersect {

    static double getTermIntersectRatioUsingAND(Query q0, Query q1){
        IndexSearcher indexSearcher = Indexes.indexSearcher

        TotalHitCountCollector collector = new TotalHitCountCollector();
        BooleanQuery.Builder bqbAnd = new BooleanQuery.Builder();
        bqbAnd.add(q0, BooleanClause.Occur.MUST)
        bqbAnd.add(q1, BooleanClause.Occur.MUST)
        indexSearcher.search(bqbAnd.build(), collector);
        final int andCount = collector.getTotalHits();

        collector = new TotalHitCountCollector();
        indexSearcher.search(q1, collector)
        final int q1Count = collector.getTotalHits()

        return andCount / q1Count
    }
}