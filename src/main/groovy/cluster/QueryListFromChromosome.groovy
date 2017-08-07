package cluster

import ec.vector.IntegerVectorIndividual
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import index.ImportantTerms
import index.IndexInfo
import org.apache.lucene.index.Term
import org.apache.lucene.search.*
import org.apache.lucene.search.spans.SpanFirstQuery
import org.apache.lucene.search.spans.SpanTermQuery

@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
class QueryListFromChromosome {

	private IndexSearcher searcher = IndexInfo.indexSearcher
	private final ImportantTerms iw = new ImportantTerms();
	private final TermQuery[] termQueryArray = iw.getTFIDFTermQueryList()
	//terms from previous run  classic4
	private final String[] notWords = ["pressure", "layer", "heat", "boundary", "computer", "library", "retrieval", "information", "cells", "patients", "blood", "algorithm"] as String[]
	private final String[] notWords20NG5 = ["jesus", "christ", "god", "windows", "high", "nasa", "orbit", "hockey", "nhl", "players", "sale"] as String[]
	
	public List getORQueryList(IntegerVectorIndividual intVectorIndividual) {

		//list of boolean queries
		List <BooleanQuery.Builder> bqbL = []
		// set of genes - for duplicate checking
		Set genes = [] as Set

		intVectorIndividual.genome.eachWithIndex {int gene, int index ->
			int clusterNumber =  index % IndexInfo.NUMBER_OF_CLUSTERS
			bqbL[clusterNumber] = bqbL[clusterNumber] ?: new BooleanQuery.Builder()

			if (gene < termQueryArray.size() && gene >= 0 && genes.add(gene)){
				bqbL[clusterNumber].add(termQueryArray[gene],BooleanClause.Occur.SHOULD)
			}
		}
		return bqbL
	}
	
	@TypeChecked(TypeCheckingMode.SKIP)
	public List getSpanFirstQL(IntegerVectorIndividual intVectorIndividual) {

		int spanValue, wordInd0
		//def word=null
		int qNumber=0;
		def wordSet = [] as Set
		def duplicateCount=0
		def sfMap = [0:50, 1:100, 2:200, 3:400, 4:2000]

		def bqbList = []		
		
		for (int i = 0; i < (intVectorIndividual.genome.length - 1); i = i + 2) {

			int clusterNumber =  qNumber % IndexInfo.NUMBER_OF_CLUSTERS
			qNumber++
			bqbList[clusterNumber] = bqbList[clusterNumber] ?: new BooleanQuery.Builder()

			if (intVectorIndividual.genome[i] >= termArray.length || intVectorIndividual.genome[i] < 0
			|| intVectorIndividual.genome[i + 1] >= termArray.length || intVectorIndividual.genome[i + 1] < 0
			|| intVectorIndividual.genome[i] == intVectorIndividual.genome[i + 1])
				continue;
			else {
				wordInd0 = intVectorIndividual.genome[i];
			}

			def term = termArray[wordInd0];

			if (!wordSet.add(term)) duplicateCount++

			def sfIndex = intVectorIndividual.genome[i + 1]
			def sfValue = sfMap[sfIndex]

			SpanFirstQuery sfq = new SpanFirstQuery(new SpanTermQuery( term),
					sfValue);

			bqbList[clusterNumber].add(sfq, BooleanClause.Occur.SHOULD);
		}

		return [bqbList, duplicateCount]
	}

	@TypeChecked(TypeCheckingMode.SKIP)
	public List getORNOTfromEvolvedList(IntegerVectorIndividual intVectorIndividual ) {

		def duplicateCount = 0
		def genes =[] as Set
		def bqbList = []
		int clusterNumber =  -1

		intVectorIndividual.genome.eachWithIndex {gene, index ->
			def z = index % IndexInfo.NUMBER_OF_CLUSTERS
			if ( z == 0) clusterNumber++
			//int clusterNumber =  0//index % IndexInfo.NUMBER_OF_CLUSTERS

			assert clusterNumber < IndexInfo.NUMBER_OF_CLUSTERS

			bqbList[clusterNumber] = bqbList[clusterNumber] ?: new BooleanQuery.Builder()

			if (gene >= 0){

				//if (index >=  (intVectorIndividual.genome.size() -  IndexInfo.NUMBER_OF_CLUSTERS )){
				if (z==4){
					//if ()
					assert gene <= notWords20NG5.size()
					//String wrd = notWords20NG5[gene]
					TermQuery tq = new TermQuery(notWords20NG5[gene])
					bqbList[clusterNumber].add(tq,BooleanClause.Occur.MUST_NOT)
				} else {
					if (genes.add(gene) && gene < termArray.size()) {
						//String wrd = termArray[gene]
						TermQuery tq = new TermQuery(termArray[gene])
						bqbList[clusterNumber].add(tq,BooleanClause.Occur.SHOULD)
					}
				}
			}

		}
		return bqbList
	}

	@TypeChecked(TypeCheckingMode.SKIP)
	public List getORNOTQL(IntegerVectorIndividual intVectorIndividual ) {

		def duplicateCount = 0
		def genes =[] as Set
		def bqbList = []

		intVectorIndividual.genome.eachWithIndex {gene, index ->

			int clusterNumber =  index % IndexInfo.NUMBER_OF_CLUSTERS
			//String wrd = termArray[gene]
			bqbList[clusterNumber] = bqbList[clusterNumber] ?: new BooleanQuery.Builder()

			if (gene < termArray.size() && gene >= 0){

				TermQuery tq = new TermQuery( termArray[gene])

				if (index >= (intVectorIndividual.genome.size() -  IndexInfo.NUMBER_OF_CLUSTERS )){
					bqbList[clusterNumber].add(tq,BooleanClause.Occur.MUST_NOT)
				} else {
					bqbList[clusterNumber].add(tq,BooleanClause.Occur.SHOULD)
					if (!genes.add(gene)) {
						duplicateCount = duplicateCount + 1
					}
				}
			}
		}
		return [bqbList, duplicateCount]
	}

	@TypeChecked(TypeCheckingMode.SKIP)
	public List getALLNOTQL(IntegerVectorIndividual intVectorIndividual ) {


		final MatchAllDocsQuery allQ = new MatchAllDocsQuery();

		//list of queries
		def bqbL = []
		// set of genes - for duplicate checking
		def genes = [] as Set

		//println "in allNot $allQ"

		intVectorIndividual.genome.eachWithIndex {gene, index ->
			int clusterNumber =  index % IndexInfo.NUMBER_OF_CLUSTERS
			if (bqbL[clusterNumber] == null) {
				bqbL[clusterNumber] = new BooleanQuery.Builder()
				bqbL[clusterNumber].add(allQ,BooleanClause.Occur.SHOULD)
			}

			if (gene < termArray.size() && gene >= 0 && genes.add(gene)){

				String word = termArray[gene]
				TermQuery tq = new TermQuery(new Term(IndexInfo.FIELD_CONTENTS, word))
				bqbL[clusterNumber].add(tq,BooleanClause.Occur.MUST_NOT)
			}
		}
		//println "end allNot bqbl  $bqbL"
		return bqbL
	}

	@TypeChecked(TypeCheckingMode.SKIP)
	//query in DNF format - could be used to generate graph for cluster visualisation?
	public List getANDQL(IntegerVectorIndividual intVectorIndividual) {

		int duplicateCount=0
		int lowSubqHits=0
		int treePen=0
		int graphPen=0
		int hitsMin=4

		def andPairSet =[] as Set
		def clusterSets =[]
		def term0=null, term1=null
		int qNumber=0;

		def bqbList = []

		intVectorIndividual.genome.eachWithIndex {gene, index ->
			assert gene < termArray.size()

			//if (gene >termArray.size() || gene < 0)
			//	gene =0;

			if (term0==null){
				term0 = termArray[gene]
			} else {
				term1 = termArray[gene]

				int clusterNumber =  qNumber % IndexInfo.NUMBER_OF_CLUSTERS
				qNumber++

				def wrds=[term0, term1] as Set
				if (term0==term1 || !andPairSet.add(wrds))
				{
					duplicateCount= duplicateCount + 1;
				}

				//check in graph form for viz
				if (! clusterSets[clusterNumber]){
					clusterSets[clusterNumber]= [] as Set
				}else
				if (!  (clusterSets[clusterNumber].contains(term0) ||clusterSets[clusterNumber].contains(term1) ) )
					graphPen = graphPen + 1

				//check that query will be in tree form (for Viz)
				//	if (clusterSets[clusterNumber].size()>0 )
				//		if (! ( (clusterSets[clusterNumber].contains(word0) && !clusterSets[clusterNumber].contains(word1))
				//		|| (clusterSets[clusterNumber].contains(word1) && !clusterSets[clusterNumber].contains(word0)) )) treePen = treePen + 1;

				clusterSets[clusterNumber].add(term0)
				clusterSets[clusterNumber].add(term1)

				BooleanQuery.Builder subbqb = new BooleanQuery.Builder();
				subbqb.add(new TermQuery(term0), BooleanClause.Occur.MUST);
				subbqb.add(new TermQuery( term1), BooleanClause.Occur.MUST);
				BooleanQuery subq = subbqb.build();

				bqbList[clusterNumber] = bqbList[clusterNumber] ?: new BooleanQuery.Builder()

				//check that the subquery returns something
				TotalHitCountCollector collector = new TotalHitCountCollector();
				searcher.search(subq, collector);
				if (collector.getTotalHits() < hitsMin)
				{
					lowSubqHits = lowSubqHits + 1;
				}

				bqbList[clusterNumber].add(subq, BooleanClause.Occur.SHOULD);
				term0=null;
			}
		}
		return [bqbList, duplicateCount, lowSubqHits]
	}
}