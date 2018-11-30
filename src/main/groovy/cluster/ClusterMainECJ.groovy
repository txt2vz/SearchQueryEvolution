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

    private final int NUMBER_OF_JOBS = 2

    //indexes suitable for clustering.
    def clusteringIndexesList = [

            IndexEnum.NG3,
            IndexEnum.CLASSIC4,
            IndexEnum.R5,
            IndexEnum.NG6,
            // IndexEnum.NG5,

            //  IndexEnum.CRISIS3,
            //    IndexEnum.R4,
    ]

    List<FitnessMethod> fitnessMethodsList = [

            //        FitnessMethod.SCORE,
            //      FitnessMethod.HITS,
            FitnessMethod.PSEUDOF1
    ]

    List<QueryType> queryTypesList = [
            //     QueryType.OR,
            //       QueryType.OR_WITH_AND_SUBQ,
            //      QueryType.AND_WITH_OR_SUBQ,
            //   QueryType.AND,
            //QueryType.MINSHOULD2,
            //    QueryType.OR_WITH_NOT,
            //   QueryType.SPAN_FIRST
            //      QueryType.ORSETK,
            QueryType.OR1SETK,
            QueryType.OR2_INTERSECT_SETK,
            QueryType.OR3_INSTERSECT_SETK,
            ///  QueryType.OR4_INSTERSECT_SETK,
            QueryType.OR_INTERSECT_MAX_SETK
            // QueryType.ORDNFSETK,
            //     QueryType.MINSHOULDSETK
    ]

    List<IntersectMethod> intersectMethodList = [

            IntersectMethod.HITS20,
            // IntersectMethod.HITS30,
            // IntersectMethod.HITS10,
            //  IntersectMethod.TEN_PERECENT_TOTAL_DIV_K
    ]

    public ClusterMainECJ() {

        final Date startRun = new Date()
        JobReport jobReport = new JobReport()

        clusteringIndexesList.each { IndexEnum ie ->

            NUMBER_OF_JOBS.times { job ->
                EvolutionState state;

                println "Index Enum ie: $ie"
                Indexes.instance.setIndex(ie)

                fitnessMethodsList.each { FitnessMethod fitnessMethod ->
                    ClusterFitness.fitnessMethod = fitnessMethod

                    intersectMethodList.each { IntersectMethod intersectMethod ->
                        ClusterFitness.intersectMethod = intersectMethod

                        [true, false].each { intersectBool ->
                            //     [true].each {intersectBool ->
                            QueryListFromChromosome.intersectTest = intersectBool


                            queryTypesList.each { qt ->
                                println "query type $qt"
                                ClusterQueryECJ.queryType = qt

                                String parameterFilePath = qt.setk ? 'src/cfg/clusterGA_K.params' : 'src/cfg/clusterGA.params'

                                ParameterDatabase parameters = new ParameterDatabase(new File(parameterFilePath));

                                state = initialize(parameters, job)
                                if (NUMBER_OF_JOBS >= 1) {
                                    final String jobFilePrefix = "job." + job;
                                    state.output.setFilePrefix(jobFilePrefix);
                                    state.checkpointPrefix = jobFilePrefix + state.checkpointPrefix;
                                }
                                //  state.parameters.set(new Parameter("generations"), "7")
                                state.output.systemMessage("Job: " + job);
                                state.job = new Object[1]
                                state.job[0] = new Integer(job)

                                state.run(EvolutionState.C_STARTED_FRESH);
                                int popSize = 0;
                                ClusterFitness cfit = (ClusterFitness) state.population.subpops.collect { sbp ->
                                    popSize = popSize + sbp.individuals.size()
                                    sbp.individuals.max() { ind ->
                                        ind.fitness.fitness()
                                    }.fitness
                                }.max { it.fitness() }

                                final int numberOfSubpops = state.parameters.getInt(new Parameter("pop.subpops"), new Parameter("pop.subpops"))
                                final int wordListSizePop0 = state.parameters.getInt(new Parameter("pop.subpop.0.species.max-gene"), new Parameter("pop.subpop.0.species.max-gene"))
                                final int genomeSizePop0 = state.parameters.getInt(new Parameter("pop.subpop.0.species.genome-size"), new Parameter("pop.subpop.0.species.genome-size"))
                                println "wordListSizePop0: $wordListSizePop0 genomeSizePop0 $genomeSizePop0  subPops $numberOfSubpops"

                                jobReport.reportsOut(job, state.generation as int, popSize as int, numberOfSubpops, genomeSizePop0, wordListSizePop0, cfit, 'finalData')
                            }
                        }
                    }
                }
                cleanup(state);
                println "--------END JOB $job  -----------------------------------------------"

            }
        }

        final Date endRun = new Date()
        TimeDuration duration = TimeCategory.minus(endRun, startRun)
        println "Duration: $duration"
        // jobReport.overallSummary(duration)
    }

    static main(args) {
        new ClusterMainECJ()
    }
}