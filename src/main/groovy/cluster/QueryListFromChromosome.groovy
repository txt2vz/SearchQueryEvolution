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
    RATIO_POINT_2(0.2d),
    RATIO_POINT_3(0.3d),
    RATIO_POINT_4(0.4d),
    RATIO_POINT_5(0.5d),
    RATIO_POINT_6(0.6d),
    RATIO_POINT_7(0.7d),
    RATIO_POINT_8(0.8d)

    IntersectMethod(double minVal) {
        minIntersectValue = minVal
    }
    double minIntersectValue
}


@CompileStatic
class QueryListFromChromosome {

    // static boolean intersectTest
    static IntersectMethod intersectMethod = IntersectMethod.RATIO_POINT_5

    final TermQuery[] termQueryArray
    BooleanClause.Occur bco = BooleanClause.Occur.SHOULD
    private final int hitsPerPage = Indexes.indexReader.maxDoc()

    //  QueryTermIntersect qti = new QueryTermIntersect()
    // final List<Tuple2<String, String>> intersectWordPairList

    QueryListFromChromosome(TermQuery[] tq) {
        termQueryArray = tq
        println "term query size " + tq.size()
        println "tq $tq"
    }

    private Tuple4<BooleanQuery.Builder[], Integer, Integer, Set<Integer>> getOneWordQueryPerCluster(int[] intChromosome, boolean setk = true) {

        final int k = (setk) ? intChromosome[0] : Indexes.NUMBER_OF_CLUSTERS
        Set<Integer> genes = [] as Set<Integer>
        BooleanQuery.Builder[] bqbL = new BooleanQuery.Builder[k]

        //set k at element 0?
        int index = (setk) ? 1 : 0;
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
            TermQuery tqNew = termQueryArray[gene]

            if (intersectMethod == IntersectMethod.NONE) {
                if (genes.add(gene)) {
                    bqbArray[clusterNumber].add(termQueryArray[gene], bco)
                }
            }

            //     if ((QueryTermIntersect.getIntersectRatio(rootq, tqNew) > intersectMethod.minIntersectValue) && genes.add(gene)) {  //to check whole query rather than first term
            else if ((QueryTermIntersect.getTermIntersectRatioUsingAND(tq0, tqNew) > intersectMethod.minIntersectValue) && genes.add(gene)) {
                bqbArray[clusterNumber].add(tqNew, bco)
            }
        }
        return bqbArray
    }

//Alternate methods
    BooleanQuery.Builder[] getSimple(
            final int[] intChromosome, int minShould = 1, BooleanClause.Occur bco = BooleanClause.Occur.SHOULD) {

        final int k = Indexes.NUMBER_OF_CLUSTERS

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
                bqbArray[clusterNumber].add(termQueryArray[gene], bco)
                clusterNumber = (clusterNumber < k - 1) ? clusterNumber + 1 : 0
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

        Set<Integer> genes = [] as Set<Integer>
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
        Set<Integer> genes = [] as Set<Integer>

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


//    BooleanQuery.Builder[] getORIntersectCheckList(int[] intChromosome, int maxQueryWordsPerCluster) {
//
//        Tuple4 tuple4 = getOneWordQueryPerCluster(intChromosome)
//        BooleanQuery.Builder[] bqbArray = tuple4.first
//        final int k = tuple4.second
//        assert k == bqbArray.size()
//
//        int index = tuple4.third
//        Set<Integer> genes = tuple4.fourth
//
//        for (int i = index; i < intChromosome.size() && i < k * maxQueryWordsPerCluster; i++) {
//
//            final int gene = intChromosome[i]
//            final int clusterNumber = i % k
//
//            BooleanQuery rootq = bqbArray[clusterNumber].build()
//
//            String rootWord = rootq.clauses().first().getQuery().toString(Indexes.FIELD_CONTENTS)
//            String newWord = termQueryArray[gene].toString(Indexes.FIELD_CONTENTS)
//
//            Tuple2<String, String> tuple2WordPairSorted = new Tuple2<String, String>(rootWord, newWord)
//
//            if (intersectTest) {
//                if (intersectWordPairList.contains(tuple2WordPairSorted) && genes.add(gene)) {
//                    bqbArray[clusterNumber].add(termQueryArray[gene], bco)
//                }
//            } else if (genes.add(gene)) {
//                bqbArray[clusterNumber].add(termQueryArray[gene], bco)
//            }
//        }
//        return bqbArray
//    }

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