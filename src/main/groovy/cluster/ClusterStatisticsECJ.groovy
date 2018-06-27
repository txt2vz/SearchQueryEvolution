package cluster

import ec.EvolutionState
import ec.simple.SimpleStatistics
import groovy.transform.CompileStatic
import index.Indexes

//@CompileStatic
public class ClusterStatisticsECJ extends SimpleStatistics {


    JobReport jr = new JobReport()

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

       if (state.generation%5==0)
        generationReport(state, cf)

        cf.generationStats(state.generation)
    }

    private void generationReport(EvolutionState state, ClusterFitness cfit) {

        def (ArrayList<Double> f1list, double averageF1, double averagePrecision, double averageRecall) = jr.calculate_F1_p_r(cfit, false)

        File fcsv = new File('results/generationReport.csv')
        if (!fcsv.exists()) {
            fcsv << 'generation, averageF1, averagePrecision, averageRecall, baseFitness, indexName, fitnessMethod, queryType, date \n'
        }

        fcsv << "${state.generation}, ${averageF1.round(2)}, ${averagePrecision.round(2)}, ${averageRecall.round(2)}, ${cfit.getFitness().round(2)}, ${Indexes.indexEnum.name()}, ${cfit.fitnessMethod}, ${ ClusterQueryECJ.queryType}, ${new Date()} \n"

    }
}