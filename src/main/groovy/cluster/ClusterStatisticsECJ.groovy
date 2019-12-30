package cluster


import ec.EvolutionState
import ec.simple.SimpleStatistics
import index.Indexes

//@CompileStatic
public class ClusterStatisticsECJ extends SimpleStatistics {


    Analysis jr = new Analysis()

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


        if (state.generation % 5 == 0) generationReport(state, cf)

        cf.generationStats(state.generation)
    }

    private void generationReport(EvolutionState state, ClusterFitness cfit) {

        Tuple4 tuple4 = Effectiveness.querySetEffectiveness(cfit.queryMap.keySet())
        final double averageF1 = tuple4.first
        final double averagePrecision = tuple4.second
        final double averageRecall  = tuple4.third

        File fcsv = new File('results/generationReport.csv')
        if (!fcsv.exists()) {
            fcsv << 'generation, averageF1, averagePrecision, averageRecall, baseFitness, indexName, fitnessMethod, intersectMethod, queryType, date \n'
        }
        fcsv << "${state.generation}, ${averageF1.round(5)}, ${averagePrecision.round(5)}, ${averageRecall.round(5)}, ${cfit.getFitness().round(5)}, ${Indexes.index.name()}, ${cfit.fitnessMethod}, ${QueryListFromChromosome.intersectMethod}, ${ClusterQueryECJ.queryType}, ${new Date()} \n"
    }
}