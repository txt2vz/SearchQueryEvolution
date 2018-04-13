package index

import org.apache.lucene.document.Document
import org.apache.lucene.index.*
import org.apache.lucene.search.DocIdSetIterator
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.similarities.ClassicSimilarity
import org.apache.lucene.search.similarities.TFIDFSimilarity
import org.apache.lucene.util.BytesRef

/**
 * Return a list of termqueries likely to be useful for building boolean queries for classification or clustering
 * Terms should be in order of their likely usefulness in query building 
 * @author Laurie
 */

//the method for term selection / dimension reduction
enum ImportantTermsMethod {
    F1, TFIDF, IG, CHI, OR, MERGED
}

//@groovy.transform.CompileStatic
//@groovy.transform.TypeChecked
public class ImportantTerms {

    public final static int SPAN_FIRST_MAX_END = 300;
    private final static int MAX_TERMQUERYLIST_SIZE = 300;

    private final IndexSearcher indexSearcher = Indexes.indexSearcher;
    private final IndexReader indexReader = indexSearcher.indexReader
    private TermsEnum termsEnum
    private Set<String> stopSet = StopSet.getStopSetFromFile()

    public static final ImportantTermsMethod itm = ImportantTermsMethod.TFIDF

    public static void main(String[] args) {
        Indexes.instance.setIndex(Indexes.indexEnum = IndexEnum.NG3)
        Indexes.instance.categoryNumber = '2'
        Indexes.instance.setIndexFieldsAndTotals()

        def iw = new ImportantTerms()
        iw.mergeMethods()
        //    iw.getF1TermQueryList()
        //  iw.getTFIDFTermQueryList()
        //        iw.getTFIDFTermQueryListForCategory()
        //iw.getIGTermQueryList()
        //iw.getChiTermQueryList()
        // iw.getORTermQueryList()
    }

    public ImportantTerms() {

        Terms terms = MultiFields.getTerms(indexReader, Indexes.FIELD_CONTENTS)
        termsEnum = MultiFields.getTerms(indexReader, Indexes.FIELD_CONTENTS).iterator()

        println "Important words terms.getDocCount: ${terms.getDocCount()}"
        println "Important words terms.size ${terms.size()}"
    }

    public TermQuery[] getImportantTerms() {

        switch (itm) {
            case itm.F1: return getF1TermQueryList(); break;
            case itm.TFIDF: return getTFIDFTermQueryListForCategory(); break;
            case itm.IG: return getIGTermQueryList(); break;
            case itm.OR: return getORTermQueryList(); break;
            case itm.MERGED: return mergeMethods(); break;
            default: println "Incorrect selection method in getImportantTerms()";
        }

        return getF1TermQueryList()
    }

    //screen terms likely to be ineffective
    private boolean isUsefulTerm(Term t) {
        int df = indexReader.docFreq(t)
        def word = t.text()

        return (
                df > 2
                        && !stopSet.contains(word)
                        && !word.contains("'")
                        && word.length() > 1
                        && word.charAt(0).isLetter()
                //  && !word.contains(".")
        )
    }

    private TermQuery[] mergeMethods() {
        // IndexInfo.instance.categoryNumber = '1'
        Indexes.instance.setIndexFieldsAndTotals()

        TermQuery[] ortq = getORTermQueryList()
        termsEnum = MultiFields.getTerms(indexReader, Indexes.FIELD_CONTENTS).iterator()

        TermQuery[] f1tq = getF1TermQueryList()
        termsEnum = MultiFields.getTerms(indexReader, Indexes.FIELD_CONTENTS).iterator()

        TermQuery[] igtq = getIGTermQueryList()
        termsEnum = MultiFields.getTerms(indexReader, Indexes.FIELD_CONTENTS).iterator()

        TermQuery[] chitq = getChiTermQueryList()

        //use sets to hold unique strings for each method
        def ors = [] as Set
        def f1s = [] as Set
        def igs = [] as Set
        def chis = [] as Set

        //populate the sets with extracted strings (words from the document)
        ortq.take(MAX_TERMQUERYLIST_SIZE).each { ors << it.toString(Indexes.FIELD_CONTENTS) }
        f1tq.take(MAX_TERMQUERYLIST_SIZE).each { f1s << it.toString(Indexes.FIELD_CONTENTS) }
        igtq.take(MAX_TERMQUERYLIST_SIZE).each { igs << it.toString(Indexes.FIELD_CONTENTS) }
        chitq.take(MAX_TERMQUERYLIST_SIZE).each { chis << it.toString(Indexes.FIELD_CONTENTS) }

        println " "
        println "F1 $f1s"
        println "OR $ors"
        println "IG $igs"
        println "chisie ${chis.size()} CHI $chis"

        //experiment with set manipulations
        def f1MinusOR = f1s - ors
        def ORMinusF1 = ors - f1s
        def ORplusF1 = ors + f1s
        def F1plusOR = f1s + ors
        println "f1MinusOR $f1MinusOR "
        println "ORMinusF1 $ORMinusF1"
        println ORplusF1.size() + " ORplusF1 $ORplusF1"
        println F1plusOR.size() + " F1plusOR $F1plusOR"
        println "intersect ${f1s.intersect(ors)}"
        println "intersect ${ors.intersect(f1s)}"

        //merge two or more sets of words created from different methods
        def merged = [] as Set


        MAX_TERMQUERYLIST_SIZE.times {
            if (it < f1s.size())
                merged << f1s[it]

            //          if (it < ors.size() )
            //             merged << ors[it]

//            if (it < chis.size() )
//                merged << chis[it]

            if (it < igs.size())
                merged << igs[it]

//            if (it < chitq.size() ) {
//                returnTQ += chitq[it]
//            }
        }

        //merged return list of termQueries
        TermQuery[] returnTQ = []
        merged.each {
            returnTQ += new TermQuery(new Term(Indexes.FIELD_CONTENTS, it))
        }

        println merged.size() + " merged $merged "

        //  TermQuery[] termQueryList = (TermQuery[]) termQueryMap.keySet().take(MAX_TERMQUERYLIST_SIZE).asImmutable().toArray()
        println returnTQ.size() + " returnTQ: $returnTQ"
//        println "orlist  0 " + termQueryList
//        println "OR map size: ${termQueryMap.size()}  termQuerylist size: ${termQueryList.size()}  termQuerylist: $termQueryList"
//        println "ORzzzz " +  termQueryList[0].toString(IndexInfo.FIELD_CONTENTS) //termQueryList.each {it.getTerm().text() }  //(IndexInfo.FIELD_CONTENTS)}
//        println "ORzzzz " +  termQueryList.each {print it.toString(IndexInfo.FIELD_CONTENTS) + " "}

        return returnTQ
    }

    /**
     * create a set of words based on F1 measure of the term when used to classify current category
     */
    //@TypeChecked(TypeCheckingMode.SKIP)
    private TermQuery[] getF1TermQueryList() {

        BytesRef termbr
        def termQueryMap = [:]

        while ((termbr = termsEnum.next()) != null) {

            Term t = new Term(Indexes.FIELD_CONTENTS, termbr);
            if (isUsefulTerm(t)) {

                Query tq = new TermQuery(t)
                final int positiveHits = Indexes.getQueryHitsWithFilter(indexSearcher, Indexes.trainDocsInCategoryFilter, tq)
                final int negativeHits = Indexes.getQueryHitsWithFilter(indexSearcher, Indexes.otherTrainDocsFilter, tq)
                double F1 = classify.Effectiveness.f1(positiveHits, negativeHits, Indexes.totalTrainDocsInCat)

                if (F1 > 0.02) {
                    termQueryMap += [(tq): F1]
                }
            }
        }

        termQueryMap = termQueryMap.sort { -it.value }
        println "termQueryMap: $termQueryMap"
        TermQuery[] termQueryList = (TermQuery[]) termQueryMap.keySet().take(MAX_TERMQUERYLIST_SIZE).asImmutable().toArray()
        println "f1 map size: ${termQueryMap.size()}  termQuerylist size: ${termQueryList.size()}  termQuerylist: $termQueryList"
        return termQueryList
    }

//for clustering
    public TermQuery[] getTFIDFTermQueryList() {
        println "getTFIDFTermQueryList Index: " + Indexes.indexEnum

        def termQueryMap = [:]
        BytesRef termbr;
        TFIDFSimilarity tfidfSim = new ClassicSimilarity()
        int docCount = indexReader.numDocs()

        while ((termbr = termsEnum.next()) != null) {

            Term t = new Term(Indexes.FIELD_CONTENTS, termbr);
            if (isUsefulTerm(t)) {

                long docFreq = indexReader.docFreq(t);
                double tfidfTotal = 0

                PostingsEnum docsEnum = termsEnum.postings(MultiFields.getTermDocsEnum(indexReader, Indexes.FIELD_CONTENTS, termbr))
                if (docsEnum != null) {
                    while (docsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {

                        // double tfidf = tfidfSim.tf(docsEnum.freq()) * tfidfSim.idf(docFreq, docCount)
                        //strangely the above commented code  line is correct according to https://lucene.apache.org/core/7_2_0/core/org/apache/lucene/search/similarities/TFIDFSimilarity.html but
                        //does not produce useful reuslt.  Using the code below produces negative idf values but does produce useful lists of terms???
                        double tfidf = tfidfSim.tf(docsEnum.freq()) * tfidfSim.idf(docCount, docFreq)
                        tfidfTotal += tfidf

                        //uncomment for debugging information
                        //    if (docsEnum.freq() > 10)
                        //        println " Term: $t  Docid: ${docsEnum.docID()} docsEnum.freq: ${docsEnum.freq()} tfidf: $tfidf tfidfTotal: $tfidfTotal tfidfSim.tf(docsEnum.freq()): ${tfidfSim.tf(docsEnum.freq())} tfidfSim.idf(docCount, docFreq): ${tfidfSim.idf(docCount, docFreq)}"

                    }
                }
                termQueryMap += [(new TermQuery(t)): tfidfTotal]
            }
        }

        termQueryMap = termQueryMap.sort { a, b -> a.value <=> b.value }
        //reverse sort
        // termQueryMap = termQueryMap.sort { a, b -> b.value <=> a.value }

        TermQuery[] termQueryArray = termQueryMap.keySet().take(MAX_TERMQUERYLIST_SIZE)
        println "termQueryMap size: ${termQueryMap.size()}  termQuerylist size: ${termQueryArray.size()}  termQuerylist: $termQueryArray"
        println "termQueryMap ${termQueryMap.take(50)}"
        return termQueryArray
    }

    private TermQuery[] getTFIDFTermQueryListForCategory() {

        def termQueryMap = [:]
        BytesRef termbr;
        TFIDFSimilarity tfidfSim = new ClassicSimilarity()
        int totalTrainDocsInCat = Indexes.totalTrainDocsInCat

        while ((termbr = termsEnum.next()) != null) {

            Term t = new Term(Indexes.FIELD_CONTENTS, termbr);
            if (isUsefulTerm(t)) {
                Query tq = new TermQuery(t)
                long matchingTrainDocsInCategoryDF = Indexes.getQueryHitsWithFilter(indexSearcher, Indexes.trainDocsInCategoryFilter, tq)
                double tfidfTotal = 0

                PostingsEnum docsEnum = termsEnum.postings(MultiFields.getTermDocsEnum(indexReader, Indexes.FIELD_CONTENTS, termbr))
                if (docsEnum != null) {
                    while (docsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {

                        Document d = indexSearcher.doc(docsEnum.docID())

                        String categoryNumber = d.get(Indexes.FIELD_CATEGORY_NUMBER)
                        String testTrain = d.get(Indexes.FIELD_TEST_TRAIN)
                        if (categoryNumber == Indexes.categoryNumber && testTrain == "train") {

                            double tfidf = tfidfSim.tf(docsEnum.freq()) * tfidfSim.idf(totalTrainDocsInCat, matchingTrainDocsInCategoryDF)

                            // double tfidf = tfidfSim.tf(docsEnum.freq()) * tfidfSim.idf(matchingTrainDocsInCategoryDF, totalTrainDocsInCat)
                            tfidfTotal += tfidf
                            //      if (docsEnum.freq() > 11)
                            //        println " Term $t  Docid ${docsEnum.docID()} docsEnum.freq ${docsEnum.freq()} tfidf $tfidf tfidfTotal $tfidfTotal categoryNumber: $categoryNumber matchingTrainDocsInCategoryDF $matchingTrainDocsInCategoryDF  totalTrainDocsInCat $totalTrainDocsInCat"

                        }
                    }
                }
                termQueryMap += [(tq): tfidfTotal]
            }
        }

        termQueryMap = termQueryMap.sort { a, b -> a.value <=> b.value }
        TermQuery[] termQueryList = termQueryMap.keySet().take(MAX_TERMQUERYLIST_SIZE)
        println "tfidf map size: ${termQueryMap.size()}  termQuerylist size: ${termQueryList.size()}  termQuerylist: $termQueryList"
        println "termQUeryMap $termQueryMap"
        return termQueryList
    }


    private double log2(double value) {
        return (Math.log(value) / Math.log(2));
    }

    private double eValXY(double x, double y) {

        return (
                -((x / (x + y)) * log2(x / (x + y))) - (y / (x + y) * log2(y / (x + y)))
        )
    }

    // Written by Prasanna on 2017/05/07 to calculate information gain
    private TermQuery[] getIGTermQueryList() {

        BytesRef termbr
        def termQueryMap = [:]

        while ((termbr = termsEnum.next()) != null) {

            Term t = new Term(Indexes.FIELD_CONTENTS, termbr);
            if (isUsefulTerm(t)) {

                Query tq = new TermQuery(t)
                //final int positiveHits = IndexInfo.getQueryHitsWithFilter(indexSearcher,IndexInfo.instance.catTrainBQ, tq)
                //final int negativeHits = IndexInfo.getQueryHitsWithFilter(indexSearcher,IndexInfo.instance.othersTrainBQ, tq)

                final int tp = Indexes.getQueryHitsWithFilter(indexSearcher, Indexes.instance.trainDocsInCategoryFilter, tq)
                final int fn = Indexes.instance.totalTrainDocsInCat - tp
                final int fp = Indexes.getQueryHitsWithFilter(indexSearcher, Indexes.instance.otherTrainDocsFilter, tq)
                final int tn = Indexes.instance.totalOthersTrainDocs - fp
                final int all = tp + fn + fp + tn
                final int posClass = tp + fn
                final int negClass = fp + tn
                final double probPos = posClass / all
                final double probNeg = negClass / all
                final double probWord = (tp + fp) / all
                final double probInvWord = (1 - probWord)

                //println "true positive $tp"
                //println "false negative $fn"
                //println "false positive $fp"
                //println "true negative $tn"
                //println "ALL $all"
                //println "Positive class: $posClass  Negative class: $negClass"
                //println " probPos $probPos probNeg $probNeg probWord $probWord probInvWord $probInvWord"

                double IG = 0
                if (tp != 0 && tn != 0 && fp != 0 && fn != 0) {
                    IG = eValXY(posClass, negClass) - (probWord * eValXY(tp, fp) + probInvWord * eValXY(fn, tn))
                }
                /*
                def F1 = classify.Effectiveness.f1(positiveHits, negativeHits,
                        IndexInfo.instance.totalTrainDocsInCat)
                */
                if (IG > 0.002) {
                    termQueryMap += [(tq): IG]
                }
            }
        }

        termQueryMap = termQueryMap.sort { -it.value }
        println "termQueryMap: $termQueryMap"
        TermQuery[] termQueryList = termQueryMap.keySet().take(MAX_TERMQUERYLIST_SIZE).asImmutable()
        println "IG map size: ${termQueryMap.size()}  termQuerylist size: ${termQueryList.size()}  termQuerylist: $termQueryList"
        return termQueryList
    }


    private double chi_t(double cnt, double expect) {

        return (
                Math.pow((cnt - expect), 2) / expect
        )
    }

    private TermQuery[] getChiTermQueryList() {


        BytesRef termbr
        def termQueryMap = [:]

        while ((termbr = termsEnum.next()) != null) {

            Term t = new Term(Indexes.FIELD_CONTENTS, termbr);
            if (isUsefulTerm(t)) {

                Query tq = new TermQuery(t)
                //final int positiveHits = IndexInfo.getQueryHitsWithFilter(indexSearcher,IndexInfo.instance.catTrainBQ, tq)
                //final int negativeHits = IndexInfo.getQueryHitsWithFilter(indexSearcher,IndexInfo.instance.othersTrainBQ, tq)

                final int tp = Indexes.getQueryHitsWithFilter(indexSearcher, Indexes.instance.trainDocsInCategoryFilter, tq)
                final int fn = Indexes.instance.totalTrainDocsInCat - tp
                final int fp = Indexes.getQueryHitsWithFilter(indexSearcher, Indexes.instance.otherTrainDocsFilter, tq)
                final int tn = Indexes.instance.totalOthersTrainDocs - fp
                final int all = tp + fn + fp + tn
                final int posClass = tp + fn
                final int negClass = fp + tn
                final double probPos = posClass / all
                final double probNeg = negClass / all
                final double probWord = (tp + fp) / all
                final double probInvWord = (1 - probWord)

                //println "true positive $tp"
                //println "false negative $fn"
                //println "false positive $fp"
                //println "true negative $tn"
                //println "ALL $all"
                //println "Positive class: $posClass  Negative class: $negClass"
                //println " probPos $probPos probNeg $probNeg probWord $probWord probInvWord $probInvWord"
                double chi = 0
                if (tp != 0 && tn != 0 && fp != 0 && fn != 0) {
                    chi = chi_t(tp, ((tp + fp) * probPos)) + chi_t(fn, ((fn + tn) * probPos)) + chi_t(fp, ((tp + fp) * probNeg)) + chi_t(tn, ((fn + tn) * probNeg))
                }
                /*
                def F1 = classify.Effectiveness.f1(positiveHits, negativeHits,
                        IndexInfo.instance.totalTrainDocsInCat)
                */
                if (chi > 100) {
                    termQueryMap += [(tq): chi]
                }
            }
        }

        termQueryMap = termQueryMap.sort { -it.value }
        println "termQueryMap: $termQueryMap"
        TermQuery[] termQueryList = termQueryMap.keySet().take(MAX_TERMQUERYLIST_SIZE).asImmutable()
        println "Chi map size: ${termQueryMap.size()}  termQuerylist size: ${termQueryList.size()}  termQuerylist: $termQueryList"
        return termQueryList
    }

    private TermQuery[] getORTermQueryList() {

        BytesRef termbr
        def termQueryMap = [:]

        while ((termbr = termsEnum.next()) != null) {

            Term t = new Term(Indexes.FIELD_CONTENTS, termbr);
            if (isUsefulTerm(t)) {

                Query tq = new TermQuery(t)
                //final int positiveHits = IndexInfo.getQueryHitsWithFilter(indexSearcher,IndexInfo.instance.catTrainBQ, tq)
                //final int negativeHits = IndexInfo.getQueryHitsWithFilter(indexSearcher,IndexInfo.instance.othersTrainBQ, tq)

                final int tp = Indexes.getQueryHitsWithFilter(indexSearcher, Indexes.instance.trainDocsInCategoryFilter, tq)
                final int fn = Indexes.instance.totalTrainDocsInCat - tp
                final int fp = Indexes.getQueryHitsWithFilter(indexSearcher, Indexes.instance.otherTrainDocsFilter, tq)
                final int tn = Indexes.instance.totalOthersTrainDocs - fp
                final int all = tp + fn + fp + tn
                final int posClass = tp + fn
                final int negClass = fp + tn
                final double probPos = posClass / all
                final double probNeg = negClass / all
                final double probWord = (tp + fp) / all
                final double probInvWord = (1 - probWord)

                //println "true positive $tp"
                //println "false negative $fn"
                //println "false positive $fp"
                //println "true negative $tn"
                //println "ALL $all"
                //println "Positive class: $posClass  Negative class: $negClass"
                //println " probPos $probPos probNeg $probNeg probWord $probWord probInvWord $probInvWord"
                double or = 0

                if (tp != 0 && tn != 0 && fp != 0 && fn != 0) {
                    or = (tp * tn) / (fp * fn)
                }

                /*
                def F1 = classify.Effectiveness.f1(positiveHits, negativeHits,
                        IndexInfo.instance.totalTrainDocsInCat)
                */

                if (or > 0.15) {
                    termQueryMap += [(tq): or]
                }
            }
        }

        termQueryMap = termQueryMap.sort { -it.value }
        println "termQueryMap: $termQueryMap"
        TermQuery[] termQueryList = termQueryMap.keySet().take(MAX_TERMQUERYLIST_SIZE).asImmutable()
        println "OR map size: ${termQueryMap.size()}  termQuerylist size: ${termQueryList.size()}  termQuerylist: $termQueryList"
        return termQueryList
    }
}