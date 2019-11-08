package classify

import ec.EvolutionState
import ec.simple.SimpleStatistics
import groovy.transform.CompileStatic
import index.Indexes
import org.apache.lucene.search.*

@CompileStatic
public class ClassifyGAStatistics extends SimpleStatistics {

	public void finalStatistics(final EvolutionState state, final int result) {
		// print out the other statistics
		super.finalStatistics(state, result);
	}

	public void postEvaluationStatistics(EvolutionState state) {
		super.postEvaluationStatistics(state);
		//		Fitness bestFitOfSubp = null, bestFitOfPop = null;
		//		for (int subPop = 0; subPop < state.population.subpops.length; ++subPop) {
		//			bestFitOfSubp = state.population.subpops[subPop].individuals[0].fitness;
		//			for (int i = 1; i < state.population.subpops[subPop].individuals.length; ++i) {
		//				Fitness fit = state.population.subpops[subPop].individuals[i].fitness;
		//				if (fit.betterThan(bestFitOfSubp))
		//					bestFitOfSubp = fit;
		//			}
		//			if (bestFitOfPop == null)
		//				bestFitOfPop = bestFitOfSubp;
		//			else if (bestFitOfSubp.betterThan(bestFitOfPop))
		//				bestFitOfPop = bestFitOfSubp;
		//		}
		//
		//		final ClassifyFit cf = (ClassifyFit) bestFitOfPop;

		ClassifyFit gaFit = (ClassifyFit) state.population.subpops.collect { sbp ->
			sbp.individuals.max() {ind ->
				ind.fitness.fitness()}.fitness
		}.max  {it.fitness()}


		// get test results on best individual
		Query q = gaFit.query
		IndexSearcher searcher = Indexes.indexSearcher;

		TotalHitCountCollector collector = new TotalHitCountCollector()
		BooleanQuery.Builder bqb = new BooleanQuery.Builder()

		bqb.add(q, BooleanClause.Occur.MUST);
		bqb.add(Indexes.testDocsInCategoryFilter, BooleanClause.Occur.FILTER);

		searcher.search(bqb.build(), collector);
		gaFit.positiveMatchTest = collector.getTotalHits();

		collector = new TotalHitCountCollector();
		bqb = new BooleanQuery.Builder();
		bqb.add(q, BooleanClause.Occur.MUST);
		bqb.add(Indexes.otherTestDocsFilter, BooleanClause.Occur.FILTER);
		searcher.search(bqb.build(), collector);

		gaFit.negativeMatchTest = collector.getTotalHits();

		gaFit.f1test = Effectiveness.f1(gaFit.positiveMatchTest, gaFit.negativeMatchTest,
				Indexes.totalTestDocsInCat)

		gaFit.BEPTest = Effectiveness.bep(gaFit.positiveMatchTest, gaFit.negativeMatchTest,
				Indexes.totalTestDocsInCat)

		println "Fitness: " + gaFit.fitness() + " F1Test: " + gaFit.f1test +
				" F1Train: " + gaFit.f1train + " positive match test: " + gaFit.positiveMatchTest +
				" negative match test: " + gaFit.negativeMatchTest

		println "QueryString: " + gaFit.getQueryString()
	}
}