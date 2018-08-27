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
import org.apache.lucene.search.TermQuery

/**
 * To generate sets of queries for clustering
 */
@CompileStatic
@TypeChecked
enum QueryType {
    OR, AND, OR_WITH_AND_SUBQ, AND_WITH_OR_SUBQ, OR_WITH_NOT, MINSHOULD2, SPAN_FIRST, ORSETK, ORDNFSETK, ORDNF, OR1SETK, MINSHOULDSETK, ORorig, OR_INTERSECT
}

@CompileStatic
public class ClusterQueryECJ extends Problem implements SimpleProblemForm {

    private QueryListFromChromosome qlfc
    static QueryType queryType

    public void setup(final EvolutionState state, final Parameter base) {

        super.setup(state, base);
        println "Total docs for ClusterQueryECJ.groovy   " + Indexes.indexReader.maxDoc()
        TermQuery[] tqa = new ImportantTerms().getTFIDFTermQueryList()
        qlfc = new QueryListFromChromosome(tqa)
    }

    public void evaluate(final EvolutionState state, final Individual ind, final int subpopulation,
                         final int threadnum) {

        if (ind.evaluated)
            return;

        ClusterFitness fitness = (ClusterFitness) ind.fitness;
        IntegerVectorIndividual intVectorIndividual = (IntegerVectorIndividual) ind;

        //list of lucene Boolean Query Builder
        List<BooleanQuery.Builder> bqbList
        final int[] genome = intVectorIndividual.genome as int[]
        qlfc.intChromosome = genome

        switch (queryType) {
            case QueryType.OR:
                // bqbList = qlfc.getOR_List()
                //   qlfc.numberOfClusters = genome[0]
                //  qlfc.intChromosome = genome[1..genome.size() - 1] as int[]
                bqbList = qlfc.OR_segments(false)
                break;

            case QueryType.ORorig:
                bqbList = qlfc.getOR_List(false)
                break;

            case QueryType.AND:
                qlfc.bco = BooleanClause.Occur.MUST
                bqbList = qlfc.getOR_List(false)
                break;

            case QueryType.MINSHOULD2:
                qlfc.minShould = 2
                bqbList = qlfc.getOR_List(false)
                break;

            case QueryType.AND_WITH_OR_SUBQ:
                bqbList = qlfc.getDNFQueryList(false, false )
                break;

            case QueryType.OR_WITH_AND_SUBQ:
                bqbList = qlfc.getDNFQueryList(false, true)
                break;

            case QueryType.OR_WITH_NOT:
                bqbList = qlfc.getORwithNOT(false)
                break;

            case QueryType.SPAN_FIRST:
                bqbList = qlfc.getSpanFirstQueryList(false)
                break;

//*****************set k methods *************************************************************
            case QueryType.OR1SETK:
                bqbList = qlfc.getOR1QueryList()
                break;

            case QueryType.OR_INTERSECT:
                bqbList = qlfc.getOR2ntersect()
                break;

            case QueryType.ORDNFSETK:
                bqbList = qlfc.getOR1DNFQueryList()
                break;

            case QueryType.ORSETK:
               // qlfc.numberOfClusters = genome[0]
               // qlfc.intChromosome = genome[1..genome.size() - 1] as int[]
                bqbList = qlfc.getOR_List(true)
                break

          //  case QueryType.MINSHOULDSETK:
            //    qlfc.numberOfClusters = genome[0]
              //  qlfc.intChromosome = genome[1..genome.size() - 1] as int[]
                //qlfc.minShould = 2
               // bqbList = qlfc.getOR_List(true)

//			case QueryType.ALLNOT :
//				bqbList = queryListFromChromosome.getALLNOTQL(intVectorIndividual)
//				break;
//			case QueryType.ORNOTEVOLVED :
//				bqbList = queryListFromChromosome.getORNOTfromEvolvedList(intVectorIndividual)
//				break;

        }
        fitness.setClusterFitness(bqbList)

//rawfitness used by ECJ for evaluation
        def rawfitness = fitness.getFitness()

        ((SimpleFitness) intVectorIndividual.fitness).setFitness(state, rawfitness, false)
        ind.evaluated = true
    }
}