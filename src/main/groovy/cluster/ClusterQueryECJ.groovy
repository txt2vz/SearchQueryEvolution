package cluster

import ec.EvolutionState
import ec.Individual
import ec.Problem
import ec.simple.SimpleFitness
import ec.simple.SimpleProblemForm
import ec.util.Parameter
import ec.vector.IntegerVectorIndividual
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import index.ImportantTerms
import index.Indexes
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery

/**
 * To generate sets of queries for clustering
 */
@CompileStatic
@TypeChecked
enum QueryType {
    OR, ORNOT, AND, ALLNOT, ORNOTEVOLVED, SpanFirst, GP, ORSETK
}

@CompileStatic
public class ClusterQueryECJ extends Problem implements SimpleProblemForm {

    private IndexSearcher searcher = Indexes.indexSearcher
    private TermQuery[] termQueryArray

    final QueryType queryType = QueryType.OR//
                            //   QueryType.ORNOT
                             //    QueryType.ORSETK
    public void setup(final EvolutionState state, final Parameter base) {

        super.setup(state, base);
        println "Total docs for ClusterQueryECJ.groovy   " + Indexes.indexReader.maxDoc()
        termQueryArray = new ImportantTerms().getTFIDFTermQueryList()
    }

    public void evaluate(final EvolutionState state, final Individual ind, final int subpopulation,
                         final int threadnum) {

        if (ind.evaluated)
            return;

        ClusterFitness fitness = (ClusterFitness) ind.fitness;
        IntegerVectorIndividual intVectorIndividual = (IntegerVectorIndividual) ind;

        //list of lucene Boolean Query Builder
        List<BooleanQuery.Builder> bqbList
        int k = 0

        switch (queryType) {
            case QueryType.OR:
                bqbList = QueryListFromChromosome.getORQueryList((int[]) intVectorIndividual.genome, termQueryArray, Indexes.NUMBER_OF_CLUSTERS)
                fitness.setClusterFitness(bqbList)
                break;
            case QueryType.ORSETK :
                Tuple2 returnvalues  = QueryListFromChromosome.getORQueryListSetK((int[]) intVectorIndividual.genome, termQueryArray)
                bqbList = (List<BooleanQuery.Builder>) returnvalues.first
                k= returnvalues.second as int
                fitness.setClusterFitness(bqbList, k)
                break;

        //@TypeChecked(TypeCheckingMode.SKIP)
//			case QueryType.AND :
//				(bqbList, duplicateCount, lowSubqHits) = queryListFromChromosome.getANDQL(intVectorIndividual)
//				break;
			case QueryType.ORNOT :
				bqbList = QueryListFromChromosome.getORQueryListNot((int[]) intVectorIndividual.genome, termQueryArray, Indexes.NUMBER_OF_CLUSTERS)
                fitness.setClusterFitness(bqbList)
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

        //assert bqbList.size() == Indexes.NUMBER_OF_CLUSTERS

//rawfitness used by ECJ for evaluation
        def rawfitness = fitness.getFitness()

        ((SimpleFitness) intVectorIndividual.fitness).setFitness(state, rawfitness, false)
        ind.evaluated = true
    }
}