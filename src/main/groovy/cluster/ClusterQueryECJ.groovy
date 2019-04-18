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

    OR(false),
    OR_SETK(true),

    AND(false),
    OR_WITH_AND_SUBQ(false),
    AND_WITH_OR_SUBQ(false),
    OR_WITH_NOT(false),
    MINSHOULD2(false),
    SPAN_FIRST(false),
    ORSETK(true),
    ORDNFSETK(true),
    ORDNF(true),
    MINSHOULDSETK(true),

    OR3_INTERSECT(false),

    OR1SETK(true),
    OR2_INTERSECT_SETK(true),
    OR3_INSTERSECT_SETK(true),
    OR4_INSTERSECT_SETK(true)

    boolean setk

    QueryType(boolean setk) {
        this.setk = setk
    }
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
        IntegerVectorIndividual intVectorIndividual = (IntegerVectorIndividual) ind
        BooleanQuery.Builder[] bqbArray
        final int[] genome = intVectorIndividual.genome as int[]

        switch (queryType) {

            case QueryType.OR:
                bqbArray = qlfc.getORIntersect(genome, 100, false)
                break

            case QueryType.OR_SETK:
                bqbArray = qlfc.getORIntersect(genome, 100, true)
                break;


            case QueryType.AND_WITH_OR_SUBQ:
                bqbArray = qlfc.getDNFQueryList(genome, false)
                break;

            case QueryType.OR_WITH_AND_SUBQ:
                bqbArray = qlfc.getDNFQueryList(genome, true)
                break;

            case QueryType.AND:
                bqbArray = qlfc.getSimple(genome, 1, BooleanClause.Occur.MUST)
                break;

            case QueryType.MINSHOULD2:
                bqbArray = qlfc.getSimple(genome, 2, BooleanClause.Occur.SHOULD)
                break;

            case QueryType.OR_WITH_NOT:
                bqbArray = qlfc.getORwithNOT(genome, false)
                break;

            case QueryType.SPAN_FIRST:
                bqbArray = qlfc.getSpanFirstQueryList(genome, false)
                break;



            case QueryType.OR3_INTERSECT:
                bqbArray = qlfc.getORIntersect(genome, 3, false)
                break;

//*****************set k methods *************************************************************

            case QueryType.OR1SETK:
                bqbArray = qlfc.getOR1QueryList(genome)
                break;


            case QueryType.OR2_INTERSECT_SETK:
                bqbArray = qlfc.getORIntersect(genome, 2)
                break;

            case QueryType.OR3_INSTERSECT_SETK:
                bqbArray = qlfc.getORIntersect(genome, 3)
                break;

            case QueryType.OR4_INSTERSECT_SETK:
                bqbArray = qlfc.getORIntersect(genome, 4)
                break;

            case QueryType.ORDNFSETK:
                bqbArray = qlfc.getOR1DNF(genome)
                break;
        }

        fitness.setClusterFitness(Arrays.asList(bqbArray).asImmutable())

        double rawfitness = fitness.getFitness()  // for ECJ

        ((SimpleFitness) intVectorIndividual.fitness).setFitness(state, rawfitness, false)
        ind.evaluated = true
    }
}