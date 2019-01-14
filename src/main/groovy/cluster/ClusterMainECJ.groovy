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

    static final int NUMBER_OF_JOBS = 10
    static final int NUMBER_OF_RUNS = 1

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

    List<FitnessMethod> fitnessMethodsList = [

            FitnessMethod.PSEUDOF1,
            //   FitnessMethod.PSEUDOF1_K_PENALTY0_3
    ]

    List<QueryType> queryTypesList = [

            QueryType.OR3_INSTERSECT_SETK,
            QueryType.OR_INTERSECT,
            //   QueryType.OR_INTERSECT_SETK,
            //     QueryType.OR3_INTERSECT,
    ]

    List<IntersectMethod> intersectMethodList = [

            //     IntersectMethod.RATIO_POINT_3,
            IntersectMethod.RATIO_POINT_5,
            //   IntersectMethod.RATIO_POINT_7
    ]

    ClusterMainECJ() {

        final Date startRun = new Date()
        List<Double> bestFitForRun = new ArrayList<Double>()
        NUMBER_OF_RUNS.times { runNumber ->
            AnalysisAndReports analysisAndReports = new AnalysisAndReports()

            clusteringIndexesList.each { IndexEnum ie ->

                NUMBER_OF_JOBS.times { job ->
                    EvolutionState state = new EvolutionState()

                    println "Index Enum ie: $ie"
                    Indexes.instance.setIndex(ie)

                    queryTypesList.each { qt ->
                        println "query type $qt"
                        ClusterQueryECJ.queryType = qt
                        String parameterFilePath = qt.setk ? 'src/cfg/clusterGA_K.params' : 'src/cfg/clusterGA.params'

                        fitnessMethodsList.each { FitnessMethod fitnessMethod ->

                            //ClusterFitness.fitnessMethod = qt.setk ? fitnessMethod : FitnessMethod.PSEUDOF1
                            ClusterFitness.fitnessMethod = qt.setk ? FitnessMethod.PSEUDOF1_K_PENALTY0_3 : FitnessMethod.PSEUDOF1


                            intersectMethodList.each { IntersectMethod intersectMethod ->
                                QueryListFromChromosome.intersectMethod = intersectMethod

                                //      [true, false].each { intersectBool ->
                                [true].each { intersectBool ->
                                    //         [false].each { intersectBool ->
                                    QueryListFromChromosome.intersectTest = intersectBool


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

                                    analysisAndReports.reportsOut(runNumber, job, state.generation as int, popSize as int, numberOfSubpops, genomeSizePop0, wordListSizePop0, cfit)
                                }
                            }
                        }
                    }
                    cleanup(state);
                    println "--------END JOB $job  -----------------------------------------------"

                }
            }

            bestFitForRun << analysisAndReports.f1fromMaxPseudoF1(runNumber) //
            analysisAndReports.jobSummary()

        }
        final Date endRun = new Date()
        TimeDuration duration = TimeCategory.minus(endRun, startRun)
        println "Duration: $duration"
        println "Runs from max fitness: $bestFitForRun"


        double average = (double) bestFitForRun.sum() / bestFitForRun.size()
        println "Average: $average"

    }

    static main(args) {
        new ClusterMainECJ()
    }
}