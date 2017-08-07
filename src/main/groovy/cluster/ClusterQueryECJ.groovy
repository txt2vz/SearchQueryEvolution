package cluster

import ec.EvolutionState
import ec.Individual
import ec.Problem
import ec.simple.SimpleFitness
import ec.simple.SimpleProblemForm
import ec.util.Parameter
import ec.vector.IntegerVectorIndividual
import index.ImportantTerms
import index.IndexInfo
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery

/**
 * To generate sets of queries for clustering
 */

@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
public class ClusterQueryECJ extends Problem implements SimpleProblemForm {

	private IndexSearcher searcher = IndexInfo.indexSearcher;
	//private final int coreClusterSize=20
	private QueryListFromChromosome queryListFromChromosome
	//private EvalQueryList evalQueryList
	private TermQuery[] termQueryArray

	enum QueryType {
		OR, ORNOT, AND, ALLNOT, ORNOTEVOLVED, SpanFirst, GP
	}
	final QueryType queryType = QueryType.OR

	public void setup(final EvolutionState state, final Parameter base) {

		super.setup(state, base);
		println "Total docs for ClusterQueryECJ.groovy   " + IndexInfo.indexReader.maxDoc()
		queryListFromChromosome = new QueryListFromChromosome()
	//	evalQueryList = new EvalQueryList();
		termQueryArray = new ImportantTerms().getTFIDFTermQueryList()
	}

	//@TypeChecked(TypeCheckingMode.SKIP)
	public void evaluate(final EvolutionState state, final Individual ind, final int subpopulation,
                         final int threadnum) {

		if (ind.evaluated)
			return;

		ClusterFitECJ fitness = (ClusterFitECJ) ind.fitness;
		IntegerVectorIndividual intVectorIndividual = (IntegerVectorIndividual) ind;

		//list of lucene Boolean Query Builders
		List <BooleanQuery.Builder> bqbList
		int duplicateCount = 0, lowSubqHits=0

		switch (queryType) {
			case QueryType.OR :
				bqbList = queryListFromChromosome.getORQueryList((int[])intVectorIndividual.genome, termQueryArray)
				break;
				//@TypeChecked(TypeCheckingMode.SKIP)
//			case QueryType.AND :
//				(bqbList, duplicateCount, lowSubqHits) = queryListFromChromosome.getANDQL(intVectorIndividual)
//				break;
//			case QueryType.ORNOT :
//				(bqbList, duplicateCount) = queryListFromChromosome.getORNOTQL(intVectorIndividual)
//				break;
//			case QueryType.ALLNOT :
//				bqbList = queryListFromChromosome.getALLNOTQL(intVectorIndividual)
//				break;
//			case QueryType.ORNOTEVOLVED :
//				bqbList = queryListFromChromosome.getORNOTfromEvolvedList(intVectorIndividual)
//				break;
//			case QueryType.SpanFirst :
//				(bqbList, duplicateCount)  = queryListFromChromosome.getSpanFirstQL(intVectorIndividual)
//				break;
		}
		assert bqbList.size() == IndexInfo.NUMBER_OF_CLUSTERS
		final int hitsPerPage = IndexInfo.indexReader.maxDoc()
		
		//set fitness based on set of boolean queries
	//	evalQueryList.setClusterFitness(fitness, bqbList, false)
		fitness.setClusterFitness(bqbList)

	
		//fitness must be positive for ECJ - most runs start with large negative score
	//	int minScore = 1000
	//	fitness.scorePlus1000 = (fitness.scoreOnly < -minScore) ? 0 : fitness.scoreOnly + minScore

	//	def negIndicators =
	//			//major penalty for query returning nothing or empty query
	//			(fitness.zeroHitsCount * 100) + fitness.coreClusterPenalty + fitness.duplicateCount + fitness.lowSubqHits + 1;

//				fitness.baseFitness = (fitness.scorePlus1000 / negIndicators) * fitness.fraction * fitness.fraction
		//fitness.baseFitness = (fitness.scorePlus1000 / negIndicators) 
		//fitness.baseFitness =  fitness.scoreOnly  //(fitness.scoreOnly / negIndicators)// - fitness.missedDocs
		//force positive
		//		if (fitness.scoreOnly> 0) {
		//			fitness.baseFitness = fitness.scoreOnly / negIndicators
		//		} else
		//			fitness.baseFitness =
		//					(fitness.positiveScoreTotal + 1) / (fitness.negativeScoreTotal +  negIndicators + 1)

		//rawfitness used by ECJ for evaluation
		def rawfitness = fitness.baseFitness

		((SimpleFitness) intVectorIndividual.fitness).setFitness(state, rawfitness, false)
		ind.evaluated = true;

		// to improve improve recall?
		//fitness.baseFitness * fitness.fraction

		//baseFitness * (1/(Math.log(missedDocs)))
		//baseFitness * (1/(Math.pow(1.01,missedDocs)))
	}
}