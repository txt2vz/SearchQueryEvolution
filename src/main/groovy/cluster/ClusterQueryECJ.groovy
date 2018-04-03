package cluster

import ec.EvolutionState
import ec.Individual
import ec.Problem
import ec.simple.SimpleFitness
import ec.simple.SimpleProblemForm
import ec.util.Parameter
import ec.vector.IntegerVectorIndividual
import groovy.transform.CompileStatic
import index.ImportantTerms
import index.Indexes
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery

/**
 * To generate sets of queries for clustering
 */
@CompileStatic
enum QueryType {
    OR, ORNOT, AND, ALLNOT, ORNOTEVOLVED, SpanFirst, GP
}

@CompileStatic
public class ClusterQueryECJ extends Problem implements SimpleProblemForm {

    private IndexSearcher searcher = Indexes.indexSearcher
    private TermQuery[] termQueryArray


    final QueryType queryType = QueryType.OR//
                            //   QueryType.ORNOT

    public void setup(final EvolutionState state, final Parameter base) {

        super.setup(state, base);
        println "Total docs for ClusterQueryECJ.groovy   " + Indexes.indexReader.maxDoc()
        termQueryArray = new ImportantTerms().getTFIDFTermQueryList()
    }

    //@TypeChecked(TypeCheckingMode.SKIP)
    public void evaluate(final EvolutionState state, final Individual ind, final int subpopulation,
                         final int threadnum) {

        if (ind.evaluated)
            return;

        ClusterFitness fitness = (ClusterFitness) ind.fitness;
        IntegerVectorIndividual intVectorIndividual = (IntegerVectorIndividual) ind;

        //list of lucene Boolean Query Builders
        List<BooleanQuery.Builder> bqbList

        switch (queryType) {
            case QueryType.OR:
                bqbList = QueryListFromChromosome.getORQueryList((int[]) intVectorIndividual.genome, termQueryArray, Indexes.NUMBER_OF_CLUSTERS)
                break;
        //@TypeChecked(TypeCheckingMode.SKIP)
//			case QueryType.AND :
//				(bqbList, duplicateCount, lowSubqHits) = queryListFromChromosome.getANDQL(intVectorIndividual)
//				break;
			case QueryType.ORNOT :
				bqbList = QueryListFromChromosome.getORQueryListNot((int[]) intVectorIndividual.genome, termQueryArray, Indexes.NUMBER_OF_CLUSTERS)
				break;
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
        assert bqbList.size() == Indexes.NUMBER_OF_CLUSTERS
        final int hitsPerPage = Indexes.indexReader.maxDoc()

        //set fitness based on set of boolean queries
        fitness.setClusterFitness(bqbList)

//rawfitness used by ECJ for evaluation
        def rawfitness = fitness.getFitness()

        ((SimpleFitness) intVectorIndividual.fitness).setFitness(state, rawfitness, false)
        ind.evaluated = true
    }
}