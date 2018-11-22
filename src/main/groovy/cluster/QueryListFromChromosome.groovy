package cluster

import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.search.BooleanClause

import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopDocs
import org.apache.lucene.search.TotalHitCountCollector
import org.apache.lucene.search.spans.SpanFirstQuery
import org.apache.lucene.search.spans.SpanTermQuery

@CompileStatic
class QueryListFromChromosome {
    final TermQuery[] termQueryArray
    BooleanClause.Occur bco = BooleanClause.Occur.SHOULD
    private final int hitsPerPage = Indexes.indexReader.maxDoc()
    private int minIntersectCount = 20

    QueryListFromChromosome(TermQuery[] tq) {
        termQueryArray = tq
    }

    BooleanQuery.Builder[] getSimple(
            final int[] intChromosome, boolean setk = false, int minShould = 1, BooleanClause.Occur bco = BooleanClause.Occur.SHOULD) {

        final int k = (setk) ? intChromosome[0] : Indexes.NUMBER_OF_CLUSTERS

        BooleanQuery.Builder[] bqbArray = new BooleanQuery.Builder[k]
        for (int i = 0; i < k; i++) {
            bqbArray[i] = (minShould == 1) ?
                    new BooleanQuery.Builder() : new BooleanQuery.Builder().setMinimumNumberShouldMatch(minShould)
        }

        int clusterNumber = 0
        Set<Integer> genes = [] as Set
        for (int i = (setk) ? 1 : 0; i < intChromosome.size(); i++) {
            final int gene = intChromosome[i]

            if (gene >= 0 && genes.add(gene)) {
                bqbArray[clusterNumber].add(termQueryArray[gene], bco)
                clusterNumber = (clusterNumber < k - 1) ? clusterNumber + 1 : i % k
            }
        }
        return bqbArray
    }

    BooleanQuery.Builder[] getDNFQueryList(final int[] intChromosome, boolean ORAND, boolean setk = false) {

        final int k = (setk) ? intChromosome[0] : Indexes.NUMBER_OF_CLUSTERS
        BooleanQuery.Builder[] bqbArray = new BooleanQuery.Builder[k]
        for (int i = 0; i < k; i++) {
            bqbArray[i] = new BooleanQuery.Builder()
        }

        Set andPairSet = [] as Set
        TermQuery term0, term1
        BooleanClause.Occur boOuter, boInner

        if (ORAND) {
            boOuter = BooleanClause.Occur.SHOULD
            boInner = BooleanClause.Occur.MUST
        } else {
            boOuter = BooleanClause.Occur.MUST
            boInner = BooleanClause.Occur.SHOULD
        }

        //  intChromosome.eachWithIndex { int gene, int index ->  //slower
        for (int index = (setk) ? 1 : 0; index < intChromosome.size(); index++) {
            int gene = intChromosome[index]
            int clusterNumber = index % k
            bqbArray[clusterNumber] = bqbArray[clusterNumber]

            if (gene < termQueryArray.size() && gene >= 0) {
                if (term0 == null) {
                    term0 = termQueryArray[gene]
                } else {
                    term1 = termQueryArray[gene]

                    Set andPair = [term0, term1] as Set
                    if ((term0 != term1) && andPairSet.add(andPair)) {

                        BooleanQuery.Builder subbqb = new BooleanQuery.Builder().add(term0, boInner)
                        subbqb.add(term1, boInner)
                        BooleanQuery subq = subbqb.build();

                        TotalHitCountCollector collector = new TotalHitCountCollector();
                        Indexes.indexSearcher.search(subq, collector);
                        if (collector.getTotalHits() > 10) {
                            bqbArray[clusterNumber] = bqbArray[clusterNumber] ?: new BooleanQuery.Builder()
                            bqbArray[clusterNumber].add(subq, boOuter);
                        }
                    }
                    term0 = null;
                }
            }
        }
        return bqbArray
    }

    BooleanQuery.Builder[] getORwithNOT(final int[] intChromosome, boolean setk = false) {

        final int k = (setk) ? intChromosome[0] : Indexes.NUMBER_OF_CLUSTERS
        BooleanQuery.Builder[] bqbArray = new BooleanQuery.Builder[k]
        for (int i = 0; i < k; i++) {
            bqbArray[i] = new BooleanQuery.Builder()
        }

        Set<Integer> genes = [] as Set
        int arrayIndex = 0

        for (int index = (setk) ? 1 : 0; index < intChromosome.size(); index++) {
            int gene = intChromosome[index]
            final int clusterNumber = index % k
            if (gene >= 0) {
                if (arrayIndex >= k && arrayIndex < k * 2) {
                    bqbArray[clusterNumber].add(termQueryArray[gene], BooleanClause.Occur.MUST_NOT)
                } else if (genes.add(gene)) {
                    bqbArray[clusterNumber].add(termQueryArray[gene], BooleanClause.Occur.SHOULD)
                }
            }
            arrayIndex++
        }
        return bqbArray
    }

    BooleanQuery.Builder[] getSpanFirstQueryList(int[] intChromosome, boolean setk) {
        final int k = (setk) ? intChromosome[0] : Indexes.NUMBER_OF_CLUSTERS
        TermQuery term
        BooleanQuery.Builder[] bqbArray = new BooleanQuery.Builder[k]
        for (int i = 0; i < k; i++) {
            bqbArray[i] = new BooleanQuery.Builder()
        }
        Set<Integer> genes = [] as Set

        for (int index = (setk) ? 1 : 0; index < intChromosome.size(); index++) {
            int gene = intChromosome[index]
            int clusterNumber = index % k

            if (gene >= 0) {
                if (term == null) {
                    if (genes.add(gene)) {
                        term = termQueryArray[gene]
                    }
                } else {
                    int sfValue
                    switch (gene) {
                        case 95: sfValue = 150
                            break
                        case 96: sfValue = 200
                            break
                        case 97: sfValue = 250
                            break
                        case 98: sfValue = 300
                            break
                        case 99: sfValue = 400
                            break
                        default: sfValue = gene
                            break
                    }
                    SpanFirstQuery sfq = new SpanFirstQuery(new SpanTermQuery(term.term), sfValue)
                    bqbArray[clusterNumber].add(sfq, BooleanClause.Occur.SHOULD);
                    term = null
                }
            }
        }
        return bqbArray
    }

    //********************************   set k methods  *******  first gene is k

    private Tuple4<BooleanQuery.Builder[], Integer, Integer, Set<Integer>> getOneWordQueryPerCluster(int[] intChromosome) {

        final int k = intChromosome[0]
        Set<Integer> genes = [] as Set
        BooleanQuery.Builder[] bqbL = new BooleanQuery.Builder[k]

        int index = 1  //set k at element 0
        int clusterNumber = 0
        while (clusterNumber < k && index < intChromosome.size()) {
            int gene = intChromosome[index]

            if (gene < termQueryArray.size() && gene >= 0 && genes.add(gene)) {
                bqbL[clusterNumber] = new BooleanQuery.Builder().add(termQueryArray[gene], BooleanClause.Occur.SHOULD)
                clusterNumber++
            }
            index++
        }
        return new Tuple4(bqbL, k, index, genes)
    }

    BooleanQuery.Builder[] getOR1QueryList(int[] intChromosome) {
        return getOneWordQueryPerCluster(intChromosome).first
    }

    BooleanQuery.Builder[] getORIntersect(int[] intChromosome, int maxQueryWordsPerCluster) {

        Tuple4 tuple4 = getOneWordQueryPerCluster(intChromosome)
        BooleanQuery.Builder[] bqbArray = tuple4.first
        final int k = tuple4.second

        if (ClusterFitness.intersectMethod == IntersectMethod.TEN_PERECENT_TOTAL_DIV_K) {
            minIntersectCount = ((Indexes.indexReader.maxDoc() / k) * 0.1).round().toInteger()
        }

        assert k == bqbArray.size()
        int index = tuple4.third
        Set<Integer> genes = tuple4.fourth

        for (int i = index; i < intChromosome.size() && i < k * maxQueryWordsPerCluster; i++) {

            final int gene = intChromosome[i]
            final int clusterNumber = i % k

            BooleanQuery rootq = bqbArray[clusterNumber].build()
            Set<Integer> rootqDocIds = [] as Set<Integer>

            TopDocs rootqTopDocs = Indexes.indexSearcher.search(rootq.clauses().first().getQuery(), hitsPerPage)
            ScoreDoc[] rootqHits = rootqTopDocs.scoreDocs;
            rootqHits.each { ScoreDoc rootqHit -> rootqDocIds << rootqHit.doc }

            TopDocs docs = Indexes.indexSearcher.search(termQueryArray[gene], hitsPerPage)
            ScoreDoc[] hits = docs.scoreDocs;

            int intersectCount = 0
            for (ScoreDoc d : hits) {
                if (rootqDocIds.contains(d.doc)) {
                    intersectCount++
                }
            }

            if (intersectCount > minIntersectCount && genes.add(gene)) {
                bqbArray[clusterNumber].add(termQueryArray[gene], bco)
            }
        }
        return bqbArray
    }

    //first word is OR_segments followed by DNF clauses
    BooleanQuery.Builder[] getOR1DNF(int[] intChromosome) {

        Tuple4 tuple4 = getOneWordQueryPerCluster(intChromosome)
        BooleanQuery.Builder[] bqbArray = tuple4.first
        final int k = tuple4.second

        assert k == bqbArray.size()
        int index = tuple4.third
        Set<Integer> genes = tuple4.fourth

        Set andPairSet = [] as Set
        TermQuery term0, term1
        int queryNumber = 0;


        for (index; index < intChromosome.size(); index++) {
            final int gene = intChromosome[index]
            if (gene < termQueryArray.size() && gene >= 0 && !genes.contains(gene)) {

                if (term0 == null) {
                    term0 = termQueryArray[gene]
                } else {
                    term1 = termQueryArray[gene]

                    Set andPair = [term0, term1] as Set
                    if (term0 != term1 && andPairSet.add(andPair)) {

                        int clusterNumber = queryNumber % k

                        BooleanQuery.Builder subbqb = new BooleanQuery.Builder();
                        subbqb.add(term0, BooleanClause.Occur.MUST);
                        subbqb.add(term1, BooleanClause.Occur.MUST)
                        BooleanQuery subq = subbqb.build()

                        BooleanQuery rootq = bqbArray[clusterNumber].build()
                        Set<Integer> rootqDocIds = [] as Set<Integer>

                        TopDocs rootqTopDocs = Indexes.indexSearcher.search(rootq, hitsPerPage)
                        ScoreDoc[] rootqHits = rootqTopDocs.scoreDocs;
                        rootqHits.each { ScoreDoc rootqHit -> rootqDocIds << rootqHit.doc }

                        TopDocs subqdocs = Indexes.indexSearcher.search(subq, hitsPerPage)
                        ScoreDoc[] subqhits = subqdocs.scoreDocs;

                        int intersectCount = 0
                        for (ScoreDoc d : subqhits) {
                            if (rootqDocIds.contains(d.doc)) {
                                intersectCount++
                            }
                        }

                        if (intersectCount > minIntersectCount) {
                            bqbArray[clusterNumber].add(subq, BooleanClause.Occur.SHOULD)
                            queryNumber++
                        }
                    }
                    term0 = null
                }
            }
        }
        return bqbArray
    }
}