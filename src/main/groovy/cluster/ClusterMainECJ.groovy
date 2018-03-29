package cluster

import ec.EvolutionState
import ec.Evolve
import ec.util.ParameterDatabase
import index.IndexEnum
import index.IndexInfo

//@groovy.transform.CompileStatic
//@groovy.transform.TypeChecked
class ClusterMainECJ extends Evolve {

    private final String parameterFilePath =
            'src/cfg/clusterGA.params'

    private final int NUMBER_OF_JOBS = 3

    //indexes suitable for clustering.
    def clusteringIndexes = [
            IndexEnum.NG5,
            IndexEnum.CLASSIC4,
            IndexEnum.CRISIS3
            //     IndexEnum.OHS3
            //IndexEnum.NG3
    ]

    public ClusterMainECJ() {

        final Date startRun = new Date()
        def jobReport = new JobReport()

        clusteringIndexes.each { ie ->
            EvolutionState state;
            ParameterDatabase parameters = null;
            println "Index Enum ie: $ie"
            IndexInfo.instance.setIndex(ie)

            NUMBER_OF_JOBS.times { job ->
                parameters = new ParameterDatabase(new File(parameterFilePath));

                state = initialize(parameters, job)
                state.output.systemMessage("Job: " + job);
                state.job = new Object[1];
                state.job[0] = new Integer(job);

                if (NUMBER_OF_JOBS >= 1) {
                    final String jobFilePrefix = "job." + job;
                    state.output.setFilePrefix(jobFilePrefix);
                    state.checkpointPrefix = jobFilePrefix + state.checkpointPrefix;
                }
                state.run(EvolutionState.C_STARTED_FRESH);

                int popSize = 0;
                ClusterFitness cfit = (ClusterFitness) state.population.subpops.collect { sbp ->
                    popSize = popSize + sbp.individuals.size()
                    sbp.individuals.max() { ind ->
                        ind.fitness.fitness()
                    }.fitness
                }.max { it.fitness() }

                jobReport.queriesReport(job, state.generation as int, popSize as int, cfit)

                cleanup(state);
                println ' ---------------------------------END-----------------------------------------------'
            }
        }
        final Date endRun = new Date();
        long time = endRun.getTime() - startRun.getTime();
        jobReport.writeOverallToFile()
        println "Total time taken: $time " + new Date(time).format("'T'HH:mm:ss.SSS")
    }

    static main(args) {
        new ClusterMainECJ()
    }
}