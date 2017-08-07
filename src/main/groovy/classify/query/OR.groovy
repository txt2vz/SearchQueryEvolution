package classify.query

import classify.ClassifyFit
import classify.Effectiveness
import ec.EvolutionState
import ec.Individual
import ec.Problem
import ec.simple.SimpleFitness
import ec.simple.SimpleProblemForm
import ec.util.Parameter
import ec.vector.IntegerVectorIndividual
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import index.ImportantTerms
import index.IndexInfo
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery

@groovy.transform.CompileStatic
@groovy.transform.TypeChecked

/**
 * To generate queries to perform binary text classification using GA string of
 * integer pairs which are translated into OR (lucene SHOULD) queries
 * 
 * @author Laurie
 */

//HitCounts is a groovy trait for counting query hits in a category
public class OR extends Problem implements SimpleProblemForm {

	private IndexSearcher searcher = IndexInfo.indexSearcher
	private final ImportantTerms importantTerms = new ImportantTerms()
	private TermQuery[] termQueryArray

	
	@TypeChecked(TypeCheckingMode.SKIP)
	public void setup(final EvolutionState state, final Parameter base) {

		super.setup(state, base);

		println("Total train docs in OR for cat:  " + IndexInfo.instance.getCategoryNumber() + " "
				+ IndexInfo.instance.totalTrainDocsInCat + " Total test docs for cat "
				+ IndexInfo.instance.totalTestDocsInCat)

		termQueryArray = importantTerms.getF1TermQueryList()
	}

	public void evaluate(final EvolutionState state, final Individual ind,
                         final int subpopulation, final int threadnum) {

		if (ind.evaluated)
			return;

		ClassifyFit fitness = (ClassifyFit) ind.fitness;
		BooleanQuery.Builder bqb = new BooleanQuery.Builder();
		IntegerVectorIndividual intVectorIndividual = (IntegerVectorIndividual) ind;

		def genes =[] as Set
		int duplicateCount=0;

		intVectorIndividual.genome.each {int gene ->
			
			//use gene set to prevent duplicates
			if (gene < termQueryArray.size() && gene >= 0 && genes.add(gene)){				
				bqb.add (termQueryArray[gene],BooleanClause.Occur.SHOULD)
			}

			fitness.query = bqb.build()			
			fitness.positiveMatchTrain = IndexInfo.getQueryHitsWithFilter(searcher,IndexInfo.trainDocsInCategoryFilter, fitness.query)
			fitness.negativeMatchTrain = IndexInfo.getQueryHitsWithFilter(searcher,IndexInfo.otherTrainDocsFilter, fitness.query)

			fitness.f1train = Effectiveness.f1(fitness.positiveMatchTrain, fitness.negativeMatchTrain, IndexInfo.totalTrainDocsInCat);

			((SimpleFitness) intVectorIndividual.fitness).setFitness(state, fitness.f1train, false)
			ind.evaluated = true;
		}
	}
}