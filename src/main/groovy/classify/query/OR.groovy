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
import index.ImportantTerms
import index.Indexes
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery

//@groovy.transform.CompileStatic
//@groovy.transform.TypeChecked

/**
 * To generate queries to perform binary text classification using GA string of
 * integer pairs which are translated into OR_segments (lucene SHOULD) queries
 *
 * @author Laurie
 */


public class OR extends Problem implements SimpleProblemForm {

    private IndexSearcher searcher = Indexes.indexSearcher
    private final ImportantTerms importantTerms = new ImportantTerms()
    private TermQuery[] termQueryArray

    public void setup(final EvolutionState state, final Parameter base) {

        super.setup(state, base);

        println "Category number: ${Indexes.instance.getCategoryNumber()} Category Name: ${Indexes.instance.getCategoryName()} " +
                "Total train docs: ${Indexes.instance.totalTrainDocsInCat} " +
                "Total test docs: ${Indexes.instance.totalTestDocsInCat}"

        termQueryArray = importantTerms.getImportantTerms() as TermQuery[]
    }

    public void evaluate(final EvolutionState state, final Individual ind,
                         final int subpopulation, final int threadnum) {

        if (ind.evaluated)
            return;

        ClassifyFit fitness = (ClassifyFit) ind.fitness;
        BooleanQuery.Builder bqb = new BooleanQuery.Builder();
        IntegerVectorIndividual intVectorIndividual = (IntegerVectorIndividual) ind;

        def genes = [] as Set
     //   int duplicateCount = 0;

        intVectorIndividual.genome.each { int gene ->

            //use gene set to prevent duplicates
            if (gene < termQueryArray.size() && gene >= 0 && genes.add(gene)) {
                bqb.add(termQueryArray[gene], BooleanClause.Occur.SHOULD)
            }
        }

        fitness.query = bqb.build()
        fitness.positiveMatchTrain = Indexes.getQueryHitsWithFilter(searcher, Indexes.trainDocsInCategoryFilter, fitness.query)
        fitness.negativeMatchTrain = Indexes.getQueryHitsWithFilter(searcher, Indexes.otherTrainDocsFilter, fitness.query)

        fitness.f1train = Effectiveness.f1(fitness.positiveMatchTrain, fitness.negativeMatchTrain, Indexes.totalTrainDocsInCat);

        ((SimpleFitness) intVectorIndividual.fitness).setFitness(state, fitness.f1train, false)
        ind.evaluated = true;
        //}
    }
}