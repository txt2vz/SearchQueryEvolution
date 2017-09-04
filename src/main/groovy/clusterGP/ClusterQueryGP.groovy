package clusterGP

import ec.EvolutionState
import ec.Individual
import ec.gp.GPIndividual
import ec.gp.GPProblem
import ec.simple.SimpleFitness
import ec.simple.SimpleProblemForm
import ec.util.Parameter
import index.ImportantTerms
import index.IndexInfo
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery
import cluster.*

public class ClusterQueryGP extends GPProblem implements SimpleProblemForm {

	private IndexSearcher searcher = IndexInfo.indexSearcher
	private TermQuery[] termQueryArray

	public void setup(final EvolutionState state,
                      final Parameter base) {
		super.setup(state, base);
		termQueryArray = new ImportantTerms().getTFIDFTermQueryList()

		// verify our input is the right class (or subclasses from it)
		if (!(input instanceof QueryData))
			state.output.fatal("GPData class must subclass from " + QueryData.class,
					base.push(P_DATA), null);
	}

	public void evaluate(final EvolutionState state, final Individual ind, final int subpopulation,
                         final int threadnum) {

		if (ind.evaluated)
			return;

		ClusterFitness fitness = (ClusterFitness) ind.fitness;
		GPIndividual gpInd= (GPIndividual) ind;
		QueryData input = (QueryData)(this.input);

		//		((GPIndividual)ind).trees[0].child.eval(
		//				state,threadnum,input,stack,((GPIndividual)ind),this);

		gpInd.trees[0].child.eval(
				state,threadnum,input,stack,((GPIndividual)ind),this);

		//set fitness based on set of boolean queries
		//fitness.setClusterFitness(fitness, input.bqbArray.toList(), true)
		//println "bqbArray size " + input.bqbArray.size()
		fitness.setClusterFitness(input.bqbArray.toList())


		//fitness must be positive for ECJ - most runs start with large negative score
	//	def final minScore = 1000
	//	fitness.scorePlus1000 = (fitness.scoreOnly < -minScore) ? 0 : fitness.scoreOnly + minScore

	//	def negIndicators =
				//major penalty for query returning nothing or empty query
	//			(fitness.zeroHitsCount * 100) + fitness.coreClusterPenalty + fitness.duplicateCount + fitness.lowSubqHits + 1;

	//	fitness.baseFitness = (fitness.scorePlus1000 / negIndicators) * fitness.fraction * fitness.fraction
		//fitness.baseFitness = (fitness.scorePlus1000 / negIndicators)
		//fitness.baseFitness =  fitness.scoreOnly  //(fitness.scoreOnly / negIndicators)// - fitness.missedDocs
		//force positive
		//		if (fitness.scoreOnly> 0) {
		//			fitness.baseFitness = fitness.scoreOnly / negIndicators
		//		} else
		//			fitness.baseFitness =
		//					(fitness.positiveScoreTotal + 1) / (fitness.negativeScoreTotal +  negIndicators + 1)

		//rawfitness used by ECJ for evaluation
		def rawfitness = fitness.getFitness()
//		if (fitness.isDummy || fitness.emptyQueries || fitness.zeroHitsCount >0 )
//		{
//			rawfitness = 0
//
//		}
		((SimpleFitness) gpInd.fitness).setFitness(state, rawfitness, false)
		ind.evaluated = true;
	}
}
