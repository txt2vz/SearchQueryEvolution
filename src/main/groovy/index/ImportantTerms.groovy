package index

import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
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

//@groovy.transform.CompileStatic
//@groovy.transform.TypeChecked
public class ImportantTerms  {

	public final static int SPAN_FIRST_MAX_END = 300;
	private final static int MAX_TERMQUERYLIST_SIZE = 300;

	private final IndexSearcher indexSearcher= IndexInfo.indexSearcher;
	private final IndexReader indexReader = indexSearcher.indexReader
	private Terms terms
	private TermsEnum termsEnum
	private Set<String> stopSet= StopSet.getStopSetFromFile()

	public static void main(String[] args){
		IndexInfo.instance.categoryNumber = '2'
		IndexInfo.instance.setIndexFieldsAndTotals()
		def iw = new ImportantTerms()
		//	iw.getF1TermQueryList()
		//iw.getTFIDFTermQueryList()
		iw.getIGTermQueryList()
		//iw.getChiTermQueryList()
		//iw.getORTermQueryList()
	}

	public ImportantTerms() {
		terms = MultiFields.getTerms(indexReader, IndexInfo.FIELD_CONTENTS)
		termsEnum = terms.iterator();
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

	/**
	 * create a set of words based on F1 measure of the term when used to classify current category
	 */
	//@TypeChecked(TypeCheckingMode.SKIP)
	public TermQuery[] getF1TermQueryList(){

		println "Important words terms.getDocCount: ${terms.getDocCount()}"
		println "Important words terms.size ${terms.size()}"

		BytesRef termbr
		def termQueryMap = [:]

		while((termbr = termsEnum.next()) != null) {

			Term t = new Term(IndexInfo.FIELD_CONTENTS, termbr);
			if ( isUsefulTerm(t) ){

				Query tq = new TermQuery(t)
				final int positiveHits = IndexInfo.getQueryHitsWithFilter(indexSearcher,IndexInfo.trainDocsInCategoryFilter, tq)
				final int negativeHits = IndexInfo.getQueryHitsWithFilter(indexSearcher,IndexInfo.otherTrainDocsFilter, tq)
				double F1 = classify.Effectiveness.f1(positiveHits, negativeHits,
						IndexInfo.totalTrainDocsInCat)

				if (F1 > 0.02) {
					termQueryMap += [(tq): F1]
				}
			}
		}

		termQueryMap= termQueryMap.sort{-it.value}
		println "termQueryMap: $termQueryMap"
		TermQuery[] termQueryList = (TermQuery[])termQueryMap.keySet().take(MAX_TERMQUERYLIST_SIZE).asImmutable().toArray()
		println "f1 map size: ${termQueryMap.size()}  termQuerylist size: ${termQueryList.size()}  termQuerylist: $termQueryList"
		return termQueryList
	}

	public TermQuery[] getTFIDFTermQueryList(){

		println "Important terms terms.getDocCount: ${terms.getDocCount()}"
		def termQueryMap = [:]
		BytesRef termbr;

		while((termbr = termsEnum.next()) != null) {

			Term t = new Term(IndexInfo.FIELD_CONTENTS, termbr);
			if (isUsefulTerm(t)){

				long indexDf = indexReader.docFreq(t);
				int docCount = indexReader.numDocs()

				//for lucene 5 : TFIDFSimilarity tfidfSim = new DefaultSimilarity()
				TFIDFSimilarity tfidfSim = new ClassicSimilarity()
				PostingsEnum docsEnum = termsEnum.postings(MultiFields.getTermDocsEnum(indexReader, IndexInfo.FIELD_CONTENTS, termbr ))
				double tfidfTotal=0

				if (docsEnum != null) {
					while (docsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
						double tfidf = tfidfSim.tf(docsEnum.freq()) * tfidfSim.idf(docCount, indexDf)
						tfidfTotal +=tfidf
					}
				}
				termQueryMap+= [(new TermQuery(t)) : tfidfTotal]
			}
		}

		termQueryMap= termQueryMap.sort{a, b -> a.value <=> b.value}
		TermQuery[] termQueryList = termQueryMap.keySet().take(MAX_TERMQUERYLIST_SIZE)
		println "tfidf map size: ${termQueryMap.size()}  termQuerylist size: ${termQueryList.size()}  termQuerylist: $termQueryList"
		return termQueryList
	}
	
	private double log2(double value) {
		return (Math.log(value) / Math.log(2));
	}

	private double eValXY(double x, double y) {
						
		return (
				-((x / ( x + y )) * log2( x / ( x + y ))) - ( y / ( x + y ) * log2( y / ( x + y ) ))
				)
	}
	
	
	// Written by Prasanna on 2017/05/07 to calculate information gain
	public TermQuery[] getIGTermQueryList(){

		println "Important words terms.getDocCount: ${terms.getDocCount()}"
		println "Important words terms.size ${terms.size()}"

		BytesRef termbr
		def termQueryMap = [:]

		while((termbr = termsEnum.next()) != null) {

			Term t = new Term(IndexInfo.FIELD_CONTENTS, termbr);
			if ( isUsefulTerm(t) ){

				Query tq = new TermQuery(t)
				//final int positiveHits = IndexInfo.getQueryHitsWithFilter(indexSearcher,IndexInfo.instance.catTrainBQ, tq)
				//final int negativeHits = IndexInfo.getQueryHitsWithFilter(indexSearcher,IndexInfo.instance.othersTrainBQ, tq)
				
				final int tp = IndexInfo.getQueryHitsWithFilter(indexSearcher,IndexInfo.instance.trainDocsInCategoryFilter, tq)
				final int fn = IndexInfo.instance.totalTrainDocsInCat - tp
				final int fp = IndexInfo.getQueryHitsWithFilter(indexSearcher,IndexInfo.instance.otherTrainDocsFilter, tq)
				final int tn = IndexInfo.instance.totalOthersTrainDocs - fp
				final int all = tp + fn + fp + tn
				final int posClass = tp + fn
				final int negClass =  fp + tn
				final double probPos = posClass / all
				final double probNeg = negClass / all
				final double probWord = ( tp + fp )	/ all
				final double probInvWord = (1 - probWord )
								
				//println "true positive $tp"
				//println "false negative $fn"
				//println "false positive $fp"
				//println "true negative $tn"
				//println "ALL $all"
				//println "Positive class: $posClass  Negative class: $negClass"
				//println " probPos $probPos probNeg $probNeg probWord $probWord probInvWord $probInvWord"
				
				double IG = 0
				if (tp != 0 && tn != 0 && fp != 0 && fn != 0) {
					IG = 	eValXY (posClass, negClass) - ( probWord * eValXY (tp, fp) + probInvWord * eValXY (fn, tn) )
				}
				/*
				def F1 = classify.Effectiveness.f1(positiveHits, negativeHits,
						IndexInfo.instance.totalTrainDocsInCat)
				*/
				if (IG > 0.002 ) {
					termQueryMap += [(tq): IG]
				}
			}
		}

		termQueryMap= termQueryMap.sort{-it.value}
		println "termQueryMap: $termQueryMap"
		TermQuery[] termQueryList = termQueryMap.keySet().take(MAX_TERMQUERYLIST_SIZE).asImmutable()
		println "IG map size: ${termQueryMap.size()}  termQuerylist size: ${termQueryList.size()}  termQuerylist: $termQueryList"
		return termQueryList
	}


	private double chi_t(double cnt, double expect) {
		
		return (
				Math.pow((cnt - expect),2) / expect
				)
	}
	
	public TermQuery[] getChiTermQueryList(){
		
				println "Important words terms.getDocCount: ${terms.getDocCount()}"
				println "Important words terms.size ${terms.size()}"
		
				BytesRef termbr
				def termQueryMap = [:]
		
				while((termbr = termsEnum.next()) != null) {
		
					Term t = new Term(IndexInfo.FIELD_CONTENTS, termbr);
					if ( isUsefulTerm(t) ){
		
						Query tq = new TermQuery(t)
						//final int positiveHits = IndexInfo.getQueryHitsWithFilter(indexSearcher,IndexInfo.instance.catTrainBQ, tq)
						//final int negativeHits = IndexInfo.getQueryHitsWithFilter(indexSearcher,IndexInfo.instance.othersTrainBQ, tq)
						
						final int tp = IndexInfo.getQueryHitsWithFilter(indexSearcher,IndexInfo.instance.trainDocsInCategoryFilter, tq)
						final int fn = IndexInfo.instance.totalTrainDocsInCat - tp
						final int fp = IndexInfo.getQueryHitsWithFilter(indexSearcher,IndexInfo.instance.otherTrainDocsFilter, tq)
						final int tn = IndexInfo.instance.totalOthersTrainDocs - fp
						final int all = tp + fn + fp + tn
						final int posClass = tp + fn
						final int negClass =  fp + tn
						final double probPos = posClass / all
						final double probNeg = negClass / all
						final double probWord = ( tp + fp )	/ all
						final double probInvWord = (1 - probWord )
										
						//println "true positive $tp"
						//println "false negative $fn"
						//println "false positive $fp"
						//println "true negative $tn"
						//println "ALL $all"
						//println "Positive class: $posClass  Negative class: $negClass"
						//println " probPos $probPos probNeg $probNeg probWord $probWord probInvWord $probInvWord"
						double chi = 0
						if (tp != 0 && tn != 0 && fp != 0 && fn != 0) {
						chi = chi_t ( tp, ((tp + fp) * probPos) ) + 	chi_t ( fn, ((fn + tn) * probPos) ) + chi_t ( fp, ((tp + fp) * probNeg) ) + chi_t ( tn, ((fn + tn) * probNeg) )
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
		
				termQueryMap= termQueryMap.sort{-it.value}
				println "termQueryMap: $termQueryMap"
				TermQuery[] termQueryList = termQueryMap.keySet().take(MAX_TERMQUERYLIST_SIZE).asImmutable()
				println "Chi map size: ${termQueryMap.size()}  termQuerylist size: ${termQueryList.size()}  termQuerylist: $termQueryList"
				return termQueryList
			}

	public TermQuery[] getORTermQueryList(){
		
				println "Important words terms.getDocCount: ${terms.getDocCount()}"
				println "Important words terms.size ${terms.size()}"
		
				BytesRef termbr
				def termQueryMap = [:]
		
				while((termbr = termsEnum.next()) != null) {
		
					Term t = new Term(IndexInfo.FIELD_CONTENTS, termbr);
					if ( isUsefulTerm(t) ){
		
						Query tq = new TermQuery(t)
						//final int positiveHits = IndexInfo.getQueryHitsWithFilter(indexSearcher,IndexInfo.instance.catTrainBQ, tq)
						//final int negativeHits = IndexInfo.getQueryHitsWithFilter(indexSearcher,IndexInfo.instance.othersTrainBQ, tq)
						
						final int tp = IndexInfo.getQueryHitsWithFilter(indexSearcher,IndexInfo.instance.trainDocsInCategoryFilter, tq)
						final int fn = IndexInfo.instance.totalTrainDocsInCat - tp
						final int fp = IndexInfo.getQueryHitsWithFilter(indexSearcher,IndexInfo.instance.otherTrainDocsFilter, tq)
						final int tn = IndexInfo.instance.totalOthersTrainDocs - fp
						final int all = tp + fn + fp + tn
						final int posClass = tp + fn
						final int negClass =  fp + tn
						final double probPos = posClass / all
						final double probNeg = negClass / all
						final double probWord = ( tp + fp )	/ all
						final double probInvWord = (1 - probWord )
										
						//println "true positive $tp"
						//println "false negative $fn"
						//println "false positive $fp"
						//println "true negative $tn"
						//println "ALL $all"
						//println "Positive class: $posClass  Negative class: $negClass"
						//println " probPos $probPos probNeg $probNeg probWord $probWord probInvWord $probInvWord"
						double or = 0
						
						if (tp != 0 && tn != 0 && fp != 0 && fn != 0) {
							or = ( tp * tn ) / ( fp * fn )
						}
						
						/*
						def F1 = classify.Effectiveness.f1(positiveHits, negativeHits,
								IndexInfo.instance.totalTrainDocsInCat)
						*/
						
						if (or > 0.15 ) {
							termQueryMap += [(tq): or]
						}
					}
				}
		
				termQueryMap= termQueryMap.sort{-it.value}
				println "termQueryMap: $termQueryMap"
				TermQuery[] termQueryList = termQueryMap.keySet().take(MAX_TERMQUERYLIST_SIZE).asImmutable()
				println "OR map size: ${termQueryMap.size()}  termQuerylist size: ${termQueryList.size()}  termQuerylist: $termQueryList"
				return termQueryList
			}
}