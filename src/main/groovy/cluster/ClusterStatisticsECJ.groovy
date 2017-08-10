package cluster

import ec.EvolutionState
import ec.simple.SimpleStatistics

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

        cf.generationStats(state.generation)
    }
}