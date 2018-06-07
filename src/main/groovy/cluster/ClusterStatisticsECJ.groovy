package cluster

import ec.EvolutionState
import ec.simple.SimpleStatistics
import groovy.transform.CompileStatic

@CompileStatic
public class ClusterStatisticsECJ extends SimpleStatistics {

    public void finalStatistics(final EvolutionState state, final int result) {
        // print out the other statistics
        super.finalStatistics(state, result);
    }

    public void postEvaluationStatistics(EvolutionState state) {
        super.postEvaluationStatistics(state);

        ClusterFitness cf = (ClusterFitness) state.population.subpops.collect { sbp ->
            sbp.individuals.max() { ind ->
                ind.fitness.fitness()
            }.fitness
        }.max { it.fitness() }

        generationReport(state, cf)

        cf.generationStats(state.generation)
    }

    private void generationReport(EvolutionState state, ClusterFitness cf) {
        File fcsv = new File("results/generationReport.csv")
        if (!fcsv.exists()) {
            fcsv << 'generation, baseFitness, hitsPlus, scorePlus \n'
        }
        fcsv << " ${state.generation}, ${cf.baseFitness}, ${cf.hitsPlus}, ${cf.scorePlus} \n"
    }
}