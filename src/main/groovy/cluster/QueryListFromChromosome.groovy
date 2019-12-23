package cluster

import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.search.BooleanClause

import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopDocs
import org.apache.lucene.search.TotalHitCountCollector
import org.apache.lucene.search.spans.SpanFirstQuery
import org.apache.lucene.search.spans.SpanTermQuery

@CompileStatic
enum IntersectMethod {

    NONE(0.0d),
    RATIO_POINT_1(0.1d),
    RATIO_POINT_2(0.2d),
    RATIO_POINT_3(0.3d),
    RATIO_POINT_4(0.4d),
    RATIO_POINT_5(0.5d),
    RATIO_POINT_6(0.6d),
    RATIO_POINT_7(0.7d),
    RATIO_POINT_8(0.8d),
    RATIO_POINT_9(0.9d)

    IntersectMethod(double minVal) {
        intersectRatio = minVal
    }
    double intersectRatio
}


@CompileStatic
class QueryListFromChromosome {

    static IntersectMethod intersectMethod = IntersectMethod.RATIO_POINT_5

    List<TermQuery> termQueryList
    BooleanClause.Occur bco = BooleanClause.Occur.SHOULD
    private final int hitsPerPage = Indexes.indexReader.maxDoc()

    QueryListFromChromosome(List<TermQuery> tql) {
        termQueryList = tql
        println "term query list size " + tql.size()
        println "tql $tql"
    }

    private Tuple4<BooleanQuery.Builder[], Integer, Integer, Set<Integer>> getOneWordQueryPerCluster(int[] intChromosome, boolean setk = true) {

        final int k = (setk) ? intChromosome[0] : Indexes.index.numberOfCategories
        Set<Integer> genes = [] as Set<Integer>
        BooleanQuery.Builder[] bqbL = new BooleanQuery.Builder[k]

        //set k at element 0?
        int index = (setk) ? 1 : 0;
        int clusterNumber = 0
        while (clusterNumber < k && index < intChromosome.size()) {

            final int gene = intChromosome[index]
            assert gene < termQueryList.size() && gene >= 0

            if (genes.add(gene))   {
                bqbL[clusterNumber] = new BooleanQuery.Builder().add(termQueryList[gene], BooleanClause.Occur.SHOULD)
                clusterNumber++
            }
            index++
        }
        return new Tuple4(bqbL, k, index, genes)
    }

    BooleanQuery.Builder[] getOR1QueryList(int[] intChromosome, boolean setk) {
        return getOneWordQueryPerCluster(intChromosome, setk).first
    }

    BooleanQuery.Builder[] getORIntersect(int[] intChromosome, int maxQueryWordsPerCluster = 100, boolean setk = true) {

        Tuple4 tuple4 = getOneWordQueryPerCluster(intChromosome, setk)
        BooleanQuery.Builder[] bqbArray = tuple4.first
        final int k = tuple4.second
        assert k == bqbArray.size()

        int index = tuple4.third
        Set<Integer> genes = tuple4.fourth

        for (int i = index; i < intChromosome.size() && i < k * maxQueryWordsPerCluster; i++) {

            final int gene = intChromosome[i]
            final int clusterNumber = i % k

            BooleanQuery rootq = bqbArray[clusterNumber].build()
            Query tq0 = rootq.clauses().first().getQuery()
            TermQuery tqNew = termQueryList[gene]

            if ((QueryTermIntersect.getTermIntersectRatioUsingAND(tq0, tqNew) >= intersectMethod.intersectRatio) && genes.add(gene)) {
                bqbArray[clusterNumber].add(tqNew, bco)
            }
        }
        return bqbArray
    }

//Alternate methods
    BooleanQuery.Builder[] getSimple(
            final int[] intChromosome, int minShould = 1, BooleanClause.Occur bco = BooleanClause.Occur.SHOULD) {

        final int k = Indexes.index.numberOfCategories

        BooleanQuery.Builder[] bqbArray = new BooleanQuery.Builder[k]
        for (int i = 0; i < k; i++) {
            bqbArray[i] = (minShould == 1) ?
                    new BooleanQuery.Builder() : new BooleanQuery.Builder().setMinimumNumberShouldMatch(minShould)
        }

        int clusterNumber = 0
        Set<Integer> genes = [] as Set<Integer>
        for (int i = 0; i < intChromosome.size(); i++) {
            final int gene = intChromosome[i]

            if (gene >= 0 && genes.add(gene)) {
                bqbArray[clusterNumber].add(termQueryList[gene], bco)
                clusterNumber = (clusterNumber < k - 1) ? clusterNumber + 1 : 0
            }
        }
        return bqbArray
    }

    BooleanQuery.Builder[] getOR1wihtMinShould(final int[] intChromosome, boolean setk = false) {

        Tuple4 tuple4 = getOneWordQueryPerCluster(intChromosome, setk)
        BooleanQuery.Builder[] bqbArray = tuple4.first
        final int k = tuple4.second
        assert k == bqbArray.size()

        final int index = tuple4.third
        Set<Integer> genes = tuple4.fourth

        BooleanQuery.Builder[] bqbMinShouldArray = new BooleanQuery.Builder[k]

        int clusterNumber = 0

        for (int i = index; i < intChromosome.size(); i++) {
            final int gene = intChromosome[i]
            assert gene >= 0

            if (genes.add(gene)) {
                if (bqbMinShouldArray[clusterNumber] == null) {
                    bqbMinShouldArray[clusterNumber] = new BooleanQuery.Builder().setMinimumNumberShouldMatch(2)
                }
                bqbMinShouldArray[clusterNumber].add(termQueryList[gene], BooleanClause.Occur.SHOULD)
                clusterNumber = (clusterNumber < k - 1) ? clusterNumber + 1 : 0
            }
        }

        for (int i = 0; i < k; i++) {
            bqbArray[i].add(bqbMinShouldArray[i].build(), BooleanClause.Occur.SHOULD)
        }

        return bqbArray
    }

    BooleanQuery.Builder[] getDNFQueryList(final int[] intChromosome, boolean ORAND, boolean setk = false) {

        final int k = (setk) ? intChromosome[0] : Indexes.index.numberOfCategories
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

            if (gene < termQueryList.size() && gene >= 0) {
                if (term0 == null) {
                    term0 = termQueryList[gene]
                } else {
                    term1 = termQueryList[gene]

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

        final int k = (setk) ? intChromosome[0] : Indexes.index.numberOfCategories
        BooleanQuery.Builder[] bqbArray = new BooleanQuery.Builder[k]
        for (int i = 0; i < k; i++) {
            bqbArray[i] = new BooleanQuery.Builder()
        }

        Set<Integer> genes = [] as Set<Integer>
        int arrayIndex = 0

        for (int index = (setk) ? 1 : 0; index < intChromosome.size(); index++) {
            int gene = intChromosome[index]
            final int clusterNumber = index % k
            if (gene >= 0) {
                if (arrayIndex >= k && arrayIndex < k * 2) {
                    bqbArray[clusterNumber].add(termQueryList[gene], BooleanClause.Occur.MUST_NOT)
                } else if (genes.add(gene)) {
                    bqbArray[clusterNumber].add(termQueryList[gene], BooleanClause.Occur.SHOULD)
                }
            }
            arrayIndex++
        }
        return bqbArray
    }

    BooleanQuery.Builder[] getSpanFirstQueryList(int[] intChromosome, boolean setk) {
        final int k = (setk) ? intChromosome[0] : Indexes.index.numberOfCategories
        TermQuery term
        BooleanQuery.Builder[] bqbArray = new BooleanQuery.Builder[k]
        for (int i = 0; i < k; i++) {
            bqbArray[i] = new BooleanQuery.Builder()
        }
        Set<Integer> genes = [] as Set<Integer>

        for (int index = (setk) ? 1 : 0; index < intChromosome.size(); index++) {
            int gene = intChromosome[index]
            int clusterNumber = index % k

            if (gene >= 0) {
                if (term == null) {
                    if (genes.add(gene)) {
                        term = termQueryList[gene]
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
        int queryNumber = 0

        for (index; index < intChromosome.size(); index++) {
            final int gene = intChromosome[index]
            if (gene < termQueryList.size() && gene >= 0 && !genes.contains(gene)) {

                if (term0 == null) {
                    term0 = termQueryList[gene]
                } else {
                    term1 = termQueryList[gene]

                    Set andPair = [term0, term1] as Set
                    if (term0 != term1 && andPairSet.add(andPair)) {

                        int clusterNumber = queryNumber % k

                        BooleanQuery.Builder subbqb = new BooleanQuery.Builder();
                        subbqb.add(term0, BooleanClause.Occur.MUST);
                        subbqb.add(term1, BooleanClause.Occur.MUST)
                        BooleanQuery subq = subbqb.build()

                        BooleanQuery rootq = bqbArray[clusterNumber].build()
                        Set<Integer> rootqDocID_Set = [] as Set<Integer>

                        TopDocs rootqTopDocs = Indexes.indexSearcher.search(rootq, hitsPerPage)
                        ScoreDoc[] rootqHits = rootqTopDocs.scoreDocs;
                        rootqHits.each { ScoreDoc rootqHit -> rootqDocID_Set << rootqHit.doc }

                        TopDocs subqdocs = Indexes.indexSearcher.search(subq, hitsPerPage)
                        ScoreDoc[] subqhits = subqdocs.scoreDocs;

                        int intersectCount = 0
                        for (ScoreDoc d : subqhits) {
                            if (rootqDocID_Set.contains(d.doc)) {
                                intersectCount++
                            }
                        }

                        //   if (intersectCount > minIntersectCount) {
                        bqbArray[clusterNumber].add(subq, BooleanClause.Occur.SHOULD)
                        queryNumber++
                        //  }
                    }
                    term0 = null
                }
            }
        }
        return bqbArray
    }
}