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
import index.ImportantTerms
import index.Indexes
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery

/**
 * To generate sets of queries for clustering
 */
@CompileStatic
@TypeChecked
enum QueryType {
    OR, ORNOT, AND,  SPAN_FIRST, ORSETK, DNF_OR_AND, AND_OR, ORDNFSETK, MINSHOULD2
}

@CompileStatic
public class ClusterQueryECJ extends Problem implements SimpleProblemForm {

    private IndexSearcher searcher = Indexes.indexSearcher
    private TermQuery[] termQueryArray

    static QueryType queryType //=   QueryType.OR

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
                bqbList = QueryListFromChromosome.getSimpleQueryList((int[]) intVectorIndividual.genome, termQueryArray, Indexes.NUMBER_OF_CLUSTERS, BooleanClause.Occur.SHOULD, 1)
                fitness.setClusterFitness(bqbList)
                break;

            case QueryType.AND:
                bqbList = QueryListFromChromosome.getSimpleQueryList((int[]) intVectorIndividual.genome, termQueryArray, Indexes.NUMBER_OF_CLUSTERS, BooleanClause.Occur.MUST, 1)
                fitness.setClusterFitness(bqbList)
                break;

            case QueryType.MINSHOULD2:
                bqbList = QueryListFromChromosome.getSimpleQueryList((int[]) intVectorIndividual.genome, termQueryArray, Indexes.NUMBER_OF_CLUSTERS, BooleanClause.Occur.SHOULD, 2)
                fitness.setClusterFitness(bqbList)
                break;

            case QueryType.AND_OR:
                bqbList = QueryListFromChromosome.getDNFQueryList((int[]) intVectorIndividual.genome, termQueryArray, Indexes.NUMBER_OF_CLUSTERS, false)
                fitness.setClusterFitness(bqbList)
                break;

            case QueryType.DNF_OR_AND:
                bqbList = QueryListFromChromosome.getDNFQueryList((int[]) intVectorIndividual.genome, termQueryArray, Indexes.NUMBER_OF_CLUSTERS, true)
                fitness.setClusterFitness(bqbList)
                break;

            case QueryType.ORNOT:
                bqbList = QueryListFromChromosome.getORwithNOT((int[]) intVectorIndividual.genome, termQueryArray, Indexes.NUMBER_OF_CLUSTERS)
                fitness.setClusterFitness(bqbList)
                break;

            case QueryType.SPAN_FIRST:
                bqbList = QueryListFromChromosome.getSpanFirstQueryList((int[]) intVectorIndividual.genome, termQueryArray, Indexes.NUMBER_OF_CLUSTERS)
                fitness.setClusterFitness(bqbList)
                break;

//******************************************************************************


            case QueryType.ORDNFSETK:
                bqbList = QueryListFromChromosome.getORDNFQueryListSetK((int[]) intVectorIndividual.genome, termQueryArray)
                fitness.setClusterFitness(bqbList)
                break;

//			case QueryType.ALLNOT :
//				bqbList = queryListFromChromosome.getALLNOTQL(intVectorIndividual)
//				break;
//			case QueryType.ORNOTEVOLVED :
//				bqbList = queryListFromChromosome.getORNOTfromEvolvedList(intVectorIndividual)
//				break;
        }

        //assert bqbList.size() == Indexes.NUMBER_OF_CLUSTERS

//rawfitness used by ECJ for evaluation
        def rawfitness = fitness.getFitness()

        ((SimpleFitness) intVectorIndividual.fitness).setFitness(state, rawfitness, false)
        ind.evaluated = true
    }
}