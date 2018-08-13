package cluster

import ec.vector.IntegerVectorIndividual
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import index.Indexes
import org.apache.lucene.index.Term
import org.apache.lucene.search.*
import org.apache.lucene.search.spans.SpanFirstQuery
import org.apache.lucene.search.spans.SpanTermQuery

@CompileStatic
class QueryListFromChromosome {

    static List<BooleanQuery.Builder> getSimpleQueryList(int[] intChromosome, TermQuery[] termQueryArray, int numberOfClusters, BooleanClause.Occur bco, int minShould) {
        //list of boolean queries
        List<BooleanQuery.Builder> bqbL = []

        // set of genes - for duplicate checking
        Set<Integer> genes = [] as Set

        int arrayIndex = 0
        for (int gene : intChromosome) {
            final int clusterNumber = arrayIndex % numberOfClusters
            arrayIndex++

            bqbL[clusterNumber] = bqbL[clusterNumber] ?: new BooleanQuery.Builder()

            if (gene >= 0 && genes.add(gene)) {
                if (minShould > 1)
                    bqbL[clusterNumber].add(termQueryArray[gene], bco).setMinimumNumberShouldMatch(minShould)
                else
                    bqbL[clusterNumber].add(termQueryArray[gene], bco)
            }
        }
        return bqbL
    }

    static List<BooleanQuery.Builder> getDNFQueryList(int[] intArray, TermQuery[] termQueryArray, int numberOfClusters, boolean ORAND) {

        Set andPairSet = [] as Set
        TermQuery term0, term1
        List<BooleanQuery.Builder> bqbL = []
        BooleanClause.Occur boOuter, boInner

        if (ORAND) {
            boOuter = BooleanClause.Occur.SHOULD
            boInner = BooleanClause.Occur.MUST
        } else {
            boOuter = BooleanClause.Occur.MUST
            boInner = BooleanClause.Occur.SHOULD
        }

        intArray.eachWithIndex { int gene, int index ->

            int clusterNumber = index % numberOfClusters
            bqbL[clusterNumber] = bqbL[clusterNumber] ?: new BooleanQuery.Builder()

            if (gene < termQueryArray.size() && gene >= 0) {
                if (term0 == null) {
                    term0 = termQueryArray[gene]
                } else {
                    term1 = termQueryArray[gene]

                    Set andPair = [term0, term1] as Set
                    if ((term0 != term1) && andPairSet.add(andPair)) {

                        BooleanQuery.Builder subbqb = new BooleanQuery.Builder();
                        subbqb.add(term0, boInner)
                        subbqb.add(term1, boInner)
                        BooleanQuery subq = subbqb.build();

                        //check that the subquery returns something
                        TotalHitCountCollector collector = new TotalHitCountCollector();
                        Indexes.indexSearcher.search(subq, collector);
                        if (collector.getTotalHits() > 10) {
                            bqbL[clusterNumber] = bqbL[clusterNumber] ?: new BooleanQuery.Builder()
                            bqbL[clusterNumber].add(subq, boOuter);
                        }
                    }
                    term0 = null;
                }
            }
        }
        return bqbL
    }

    static List<BooleanQuery.Builder> getORwithNOT(int[] intChromosome, TermQuery[] termQueryArray, int numberOfClusters) {
        //list of boolean queries
        List<BooleanQuery.Builder> bqbL = []

        // set of genes - for duplicate checking
        Set<Integer> genes = [] as Set
        int arrayIndex = 0

        for (int gene : intChromosome) {
            final int clusterNumber = arrayIndex % numberOfClusters

            bqbL[clusterNumber] = bqbL[clusterNumber] ?: new BooleanQuery.Builder()

            if (gene >= 0) {
                if (arrayIndex >= numberOfClusters && arrayIndex < numberOfClusters * 2) {
                    bqbL[clusterNumber].add(termQueryArray[gene], BooleanClause.Occur.MUST_NOT)
                } else if (genes.add(gene)) {
                    bqbL[clusterNumber].add(termQueryArray[gene], BooleanClause.Occur.SHOULD)
                }
            }
            arrayIndex++
        }
        return bqbL
    }

    static List<BooleanQuery.Builder> getSpanFirstQueryList(int[] intArray, TermQuery[] termQueryArray, int numberOfClusters) {

        TermQuery term
        List<BooleanQuery.Builder> bqbL = []
        Set<Integer> genes = [] as Set

        intArray.eachWithIndex { int gene, int index ->

            int clusterNumber = index % numberOfClusters
            bqbL[clusterNumber] = bqbL[clusterNumber] ?: new BooleanQuery.Builder()

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
                    bqbL[clusterNumber].add(sfq, BooleanClause.Occur.SHOULD);
                    term = null
                }
            }
        }
        return bqbL
    }

    //*******************************************************************************

//one word query setting k
    static List<BooleanQuery.Builder> getOR1QueryList(int[] intArray, TermQuery[] termQueryArray, int numberOfClusters) {

        List<BooleanQuery.Builder> bqbL = []

        for (int clusterNumber = 0; clusterNumber < numberOfClusters; clusterNumber++){
            bqbL[clusterNumber] =  new BooleanQuery.Builder()

            int gene = intArray[clusterNumber]

            if (gene < termQueryArray.size() && gene >= 0) {//&& genes.add(gene)) {
                bqbL[clusterNumber].add(termQueryArray[gene], BooleanClause.Occur.SHOULD)
            }
        }

        return bqbL
    }

        //first word is OR followed by DNF clauses
    static List<BooleanQuery.Builder> getORDNFQueryList(int[] intArray, TermQuery[] termQueryArray, int numberOfClusters) {

        Set andPairSet = [] as Set
        TermQuery term0, term1
        int queryNumber = 0;

        List<BooleanQuery.Builder> bqbL = []

        intArray.eachWithIndex { int gene, int index ->

            if (index < numberOfClusters) {
                int clusterNumber = index % numberOfClusters
                bqbL[clusterNumber] = bqbL[clusterNumber] ?: new BooleanQuery.Builder()

                if (gene < termQueryArray.size() && gene >= 0) {//&& genes.add(gene)) {
                    bqbL[clusterNumber].add(termQueryArray[gene], BooleanClause.Occur.SHOULD)
                }

            } else if (gene < termQueryArray.size() && gene >= 0) {
                if (term0 == null) {
                    term0 = termQueryArray[gene]
                } else {
                    term1 = termQueryArray[gene]

                    //  Set andPair = [term0, term1] as Set
                    if (term0 != term1) {////&& andPairSet.add(andPair)) {

                        int clusterNumber = queryNumber % numberOfClusters
                        queryNumber++

                        BooleanQuery.Builder subbqb = new BooleanQuery.Builder();
                        subbqb.add(term0, BooleanClause.Occur.MUST);
                        subbqb.add(term1, BooleanClause.Occur.MUST)
                        BooleanQuery subq = subbqb.build();

                        //check that the subquery returns something
                        TotalHitCountCollector collector = new TotalHitCountCollector();
                        Indexes.indexSearcher.search(subq, collector);
                        if (collector.getTotalHits() > 10) {
                            bqbL[clusterNumber] = bqbL[clusterNumber] ?: new BooleanQuery.Builder()
                            bqbL[clusterNumber].add(subq, BooleanClause.Occur.SHOULD);
                        }
                    }
                    term0 = null;
                }
            }
        }
        return bqbL
    }


    static List<BooleanQuery.Builder> getORQueryListNot(int[] intArray, TermQuery[] termQueryArray, int numberOfClusters) {
        //list of boolean queries
        List<BooleanQuery.Builder> bqbL = []

        // set of genes - for duplicate checking
        Set<Integer> genes = [] as Set

        intArray.eachWithIndex { int gene, int index ->
            int clusterNumber = index % numberOfClusters
            bqbL[clusterNumber] = bqbL[clusterNumber] ?: new BooleanQuery.Builder()

            if (gene < termQueryArray.size() && gene >= 0 && genes.add(gene)) {
                bqbL[clusterNumber].add(termQueryArray[gene], BooleanClause.Occur.SHOULD)
            }
        }

        BooleanQuery qNot
        bqbL.eachWithIndex { BooleanQuery.Builder bqb, int index ->

            if (index == 0) {
                qNot = bqb.build()
            } else {
                BooleanQuery qn0 = bqb.build()
                def clauses = qNot.clauses()
                for (clause in clauses) {
                    bqb.add(clause.getQuery(), BooleanClause.Occur.MUST_NOT)
                }
                //   bqb.add(qNot, BooleanClause.Occur.MUST_NOT)
                qNot = qn0
            }
        }
        return bqbL
    }

    @TypeChecked(TypeCheckingMode.SKIP)
    public List getORNOTfromEvolvedList(IntegerVectorIndividual intVectorIndividual) {

        def duplicateCount = 0
        def genes = [] as Set
        def bqbList = []
        int clusterNumber = -1

        intVectorIndividual.genome.eachWithIndex { gene, index ->
            def z = index % Indexes.NUMBER_OF_CLUSTERS
            if (z == 0) clusterNumber++
            //int clusterNumber =  0//index % IndexInfo.NUMBER_OF_CLUSTERS

            assert clusterNumber < Indexes.NUMBER_OF_CLUSTERS

            bqbList[clusterNumber] = bqbList[clusterNumber] ?: new BooleanQuery.Builder()

            if (gene >= 0) {

                //if (index >=  (intVectorIndividual.genome.size() -  IndexInfo.NUMBER_OF_CLUSTERS )){
                if (z == 4) {
                    //if ()
                    assert gene <= notWords20NG5.size()
                    //String wrd = notWords20NG5[gene]
                    TermQuery tq = new TermQuery(notWords20NG5[gene])
                    bqbList[clusterNumber].add(tq, BooleanClause.Occur.MUST_NOT)
                } else {
                    if (genes.add(gene) && gene < termArray.size()) {
                        //String wrd = termArray[gene]
                        TermQuery tq = new TermQuery(termArray[gene])
                        bqbList[clusterNumber].add(tq, BooleanClause.Occur.SHOULD)
                    }
                }
            }

        }
        return bqbList
    }

    @TypeChecked(TypeCheckingMode.SKIP)
    public List getORNOTQL(IntegerVectorIndividual intVectorIndividual) {

        def duplicateCount = 0
        def genes = [] as Set
        def bqbList = []

        intVectorIndividual.genome.eachWithIndex { gene, index ->

            int clusterNumber = index % Indexes.NUMBER_OF_CLUSTERS
            //String wrd = termArray[gene]
            bqbList[clusterNumber] = bqbList[clusterNumber] ?: new BooleanQuery.Builder()

            if (gene < termArray.size() && gene >= 0) {

                TermQuery tq = new TermQuery(termArray[gene])

                if (index >= (intVectorIndividual.genome.size() - Indexes.NUMBER_OF_CLUSTERS)) {
                    bqbList[clusterNumber].add(tq, BooleanClause.Occur.MUST_NOT)
                } else {
                    bqbList[clusterNumber].add(tq, BooleanClause.Occur.SHOULD)
                    if (!genes.add(gene)) {
                        duplicateCount = duplicateCount + 1
                    }
                }
            }
        }
        return [bqbList, duplicateCount]
    }

    @TypeChecked(TypeCheckingMode.SKIP)
    public List getALLNOTQL(IntegerVectorIndividual intVectorIndividual) {


        final MatchAllDocsQuery allQ = new MatchAllDocsQuery();

        //list of queries
        def bqbL = []
        // set of genes - for duplicate checking
        def genes = [] as Set

        //println "in allNot $allQ"

        intVectorIndividual.genome.eachWithIndex { gene, index ->
            int clusterNumber = index % Indexes.NUMBER_OF_CLUSTERS
            if (bqbL[clusterNumber] == null) {
                bqbL[clusterNumber] = new BooleanQuery.Builder()
                bqbL[clusterNumber].add(allQ, BooleanClause.Occur.SHOULD)
            }

            if (gene < termArray.size() && gene >= 0 && genes.add(gene)) {

                String word = termArray[gene]
                TermQuery tq = new TermQuery(new Term(Indexes.FIELD_CONTENTS, word))
                bqbL[clusterNumber].add(tq, BooleanClause.Occur.MUST_NOT)
            }
        }
        //println "end allNot bqbl  $bqbL"
        return bqbL
    }
}