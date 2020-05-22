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
import index.ImportantTermQueries
import index.Indexes
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.TermQuery

/**
 * To generate sets of queries for clustering
 */
@CompileStatic
@TypeChecked
enum QueryTypeECJ {

    OR(false),
    OR_SETK(true),
    OR1(false),
    OR1SETK(true),

    MINSHOULD2(false),
    OR1_WITH_MINSHOULD2(false),

    AND(false),
    OR_WITH_AND_SUBQ(false),
    AND_WITH_OR_SUBQ(false),
    OR_WITH_NOT(false),
    SPAN_FIRST(false),
    ORDNFSETK(true),
    ORDNF(true),
    MINSHOULDSETK(true),
    OR3_INTERSECT(false),
    OR2_INTERSECT_SETK(true),
    OR3_INSTERSECT_SETK(true),
    OR4_INSTERSECT_SETK(true)

    boolean setk

    QueryTypeECJ(boolean setk) {
        this.setk = setk
    }
}

@CompileStatic
public class ClusterQueryECJ extends Problem implements SimpleProblemForm {

    private QueryListFromChromosome qlfc
    static QueryTypeECJ queryType

    public void setup(final EvolutionState state, final Parameter base) {

        super.setup(state, base);
        println "Total docs for ClusterQueryECJ.groovy   " + Indexes.indexReader.numDocs()
        List <TermQuery>  tql = ImportantTermQueries.getTFIDFTermQueryList(Indexes.indexReader)
        qlfc = new QueryListFromChromosome(tql)
    }

    public void evaluate(final EvolutionState state, final Individual ind, final int subpopulation,
                         final int threadnum) {

        if (ind.evaluated)
            return;

        ECJclusterFitness fitness = (ECJclusterFitness) ind.fitness;
        IntegerVectorIndividual intVectorIndividual = (IntegerVectorIndividual) ind
        BooleanQuery.Builder[] bqbArray
        final int[] genome = intVectorIndividual.genome as int[]

        switch (queryType) {

            case QueryTypeECJ.OR1:
                bqbArray = qlfc.getOR1QueryList(genome, false)
                break;

            case QueryTypeECJ.OR:
                bqbArray = qlfc.getORIntersect(genome, 100, false)
                break

            case QueryTypeECJ.OR_SETK:
                bqbArray = qlfc.getORIntersect(genome, 100, true)
                break;

            case QueryTypeECJ.MINSHOULD2:
                bqbArray = qlfc.getSimple(genome, 2, BooleanClause.Occur.SHOULD)
                break;

            case QueryTypeECJ.OR1_WITH_MINSHOULD2:
                bqbArray = qlfc.getOR1wihtMinShould(genome)
                break;

            case QueryTypeECJ.AND_WITH_OR_SUBQ:
                bqbArray = qlfc.getDNFQueryList(genome, false)
                break;

            case QueryTypeECJ.OR_WITH_AND_SUBQ:
                bqbArray = qlfc.getDNFQueryList(genome, true)
                break;

            case QueryTypeECJ.AND:
                bqbArray = qlfc.getSimple(genome, 1, BooleanClause.Occur.MUST)
                break;

            case QueryTypeECJ.OR_WITH_NOT:
                bqbArray = qlfc.getORwithNOT(genome, false)
                break;

            case QueryTypeECJ.SPAN_FIRST:
                bqbArray = qlfc.getSpanFirstQueryList(genome, false)
                break;

            case QueryTypeECJ.OR3_INTERSECT:
                bqbArray = qlfc.getORIntersect(genome, 3, false)
                break;


//*****************set k methods *************************************************************

            case QueryTypeECJ.OR1SETK:
                bqbArray = qlfc.getOR1QueryList(genome, true)
                break;

            case QueryTypeECJ.OR2_INTERSECT_SETK:
                bqbArray = qlfc.getORIntersect(genome, 2)
                break;

            case QueryTypeECJ.OR3_INSTERSECT_SETK:
                bqbArray = qlfc.getORIntersect(genome, 3)
                break;

            case QueryTypeECJ.OR4_INSTERSECT_SETK:
                bqbArray = qlfc.getORIntersect(genome, 4)
                break;

            case QueryTypeECJ.ORDNFSETK:
                bqbArray = qlfc.getOR1DNF(genome)
                break;
        }

        fitness.setClusterFitness(Arrays.asList(bqbArray).asImmutable())

        double rawfitness = fitness.getFitness()  // for ECJ

        ((SimpleFitness) intVectorIndividual.fitness).setFitness(state, rawfitness, false)
        ind.evaluated = true
    }
}