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

    static final int NUMBER_OF_JOBS = 11

    //indexes suitable for clustering.
    def clusteringIndexesList = [

            IndexEnum.NG3,
            IndexEnum.CRISIS3,
            IndexEnum.CLASSIC4,
            IndexEnum.R4,
            IndexEnum.R5,
            IndexEnum.NG5,
            IndexEnum.R6,
            IndexEnum.NG6
    ]

    List<Double> kPenalty =


            [0.0d, 0.01d, 0.02d, 0.03d, 0.04d, 0.05d, 0.06d, 0.07d, 0.08d, 0.09d, 0.1d]
    //     [0.04d]


    List<QueryType> queryTypesList = [

            //    QueryType.OR,
            QueryType.OR_SETK,

    ]

    List<IntersectMethod> intersectMethodList = [

            IntersectMethod.NONE,
            IntersectMethod.RATIO_POINT_1,
            IntersectMethod.RATIO_POINT_2,
            IntersectMethod.RATIO_POINT_3,
            IntersectMethod.RATIO_POINT_4,

            IntersectMethod.RATIO_POINT_5,

            IntersectMethod.RATIO_POINT_6,
            IntersectMethod.RATIO_POINT_7,
            IntersectMethod.RATIO_POINT_8,
            IntersectMethod.RATIO_POINT_9
    ]

    ClusterMainECJ() {

        final Date startRun = new Date()

        AnalysisAndReports analysisAndReports = new AnalysisAndReports()

        clusteringIndexesList.each { IndexEnum ie ->

            NUMBER_OF_JOBS.times { job ->
                EvolutionState state = new EvolutionState()

                println "Index Enum ie: $ie"
                Indexes.instance.setIndex(ie)

                kPenalty.each { kPenalty ->
                    ClusterFitness.kPenalty = kPenalty

                    queryTypesList.each { qt ->
                        println "query type $qt"
                        ClusterQueryECJ.queryType = qt
                        String parameterFilePath = qt.setk ? 'src/cfg/clusterGA_K.params' : 'src/cfg/clusterGA.params'

                        ClusterFitness.fitnessMethod = qt.setk ? FitnessMethod.UNIQUE_HITS_K_PENALTY : FitnessMethod.UNIQUE_HITS_COUNT

                        intersectMethodList.each { IntersectMethod intersectMethod ->
                            QueryListFromChromosome.intersectMethod = intersectMethod

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

                            analysisAndReports.reportsOut(job, state.generation as int, popSize as int, numberOfSubpops, genomeSizePop0, wordListSizePop0, cfit)
                        }
                    }
                    cleanup(state);
                    println "--------END JOB $job  -----------------------------------------------"
                }
            }
        }

        analysisAndReports.jobSummary()

        final Date endRun = new Date()
        TimeDuration duration = TimeCategory.minus(endRun, startRun)
        println "Duration: $duration"

    }

    static main(args) {
        new ClusterMainECJ()
    }
}