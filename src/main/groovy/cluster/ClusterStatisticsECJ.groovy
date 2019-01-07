package cluster

import ec.EvolutionState
import ec.simple.SimpleStatistics
import index.Indexes

//@CompileStatic
public class ClusterStatisticsECJ extends SimpleStatistics {


    AnalysisAndReports jr = new AnalysisAndReports()

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


        if (state.generation % 50 == 0) generationReport(state, cf)

        cf.generationStats(state.generation)
    }

    private void generationReport(EvolutionState state, ClusterFitness cfit) {

        def (ArrayList<Double> f1list, double averageF1, double averagePrecision, double averageRecall) = jr.calculate_F1_p_r(cfit, false)

        File fcsv = new File('results/generationReport.csv')
        if (!fcsv.exists()) {
            fcsv << 'generation, averageF1, averagePrecision, averageRecall, baseFitness, indexName, fitnessMethod, intersectTest, queryType, date \n'
        }

        fcsv << "${state.generation}, ${averageF1.round(5)}, ${averagePrecision.round(5)}, ${averageRecall.round(5)}, ${cfit.getFitness().round(5)}, ${Indexes.indexEnum.name()}, ${cfit.fitnessMethod}, ${QueryListFromChromosome.intersectTest}, ${ClusterQueryECJ.queryType}, ${new Date()} \n"

    }
}