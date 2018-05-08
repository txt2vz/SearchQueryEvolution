package cluster

import ec.EvolutionState
import ec.Evolve
import ec.util.ParameterDatabase
import ec.util.Parameter
import groovy.time.TimeCategory
import groovy.time.TimeDuration
import groovy.transform.CompileStatic
import index.IndexEnum
import index.Indexes

@CompileStatic
class ClusterMainECJ extends Evolve {

    private final String parameterFilePath =
            'src/cfg/clusterGA.params'
    //  'src/cfg/clusterGA_K.params'

    private final int NUMBER_OF_JOBS = 2

    //indexes suitable for clustering.
    def clusteringIndexes = [
        //   IndexEnum.CRISIS3,
          // IndexEnum.CLASSIC4,
           IndexEnum.R4,
           //IndexEnum.NG5,
           //IndexEnum.NG6

         //  IndexEnum.R6
    ]

    public ClusterMainECJ() {

        final Date startRun = new Date()
        JobReport jobReport = new JobReport()

        clusteringIndexes.each { IndexEnum ie ->
            EvolutionState state;
            ParameterDatabase parameters = null

            println "Index Enum ie: $ie"
            Indexes.instance.setIndex(ie)

            NUMBER_OF_JOBS.times { job ->
                parameters = new ParameterDatabase(new File(parameterFilePath));

                state = initialize(parameters, job)
                state.output.systemMessage("Job: " + job);
                state.job = new Object[1]
                state.job[0] = new Integer(job)

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

                final int numberOfSubpops =  state.parameters.getInt(new Parameter("pop.subpops"),new Parameter("pop.subpops" ))
                final int wordListSizePop0 =  state.parameters.getInt(new Parameter("pop.subpop.0.species.max-gene"),new Parameter("pop.subpop.0.species.max-gene" ))
                final int genomeSizePop0 = state.parameters.getInt(new Parameter("pop.subpop.0.species.genome-size"),new Parameter("pop.subpop.0.species.genome-size" ))
                println "wordListSizePop0: $wordListSizePop0 genomeSizePop0 $genomeSizePop0"

                jobReport.queriesReport(job, state.generation as int, popSize as int, numberOfSubpops, genomeSizePop0, wordListSizePop0, cfit)
                cleanup(state);
                println "--------END JOB $job  -----------------------------------------------"
            }
        }
        final Date endRun = new Date()
        TimeDuration duration = TimeCategory.minus(endRun, startRun)
        println "Duration: $duration"
        jobReport.overallSummary(duration)
    }

    static main(args) {
        new ClusterMainECJ()
    }
}