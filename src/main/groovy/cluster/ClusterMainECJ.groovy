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

    static final int NUMBER_OF_RUNS = 2
    static final int JOBS_FOR_PSEUDO_F1_SELECTION = 5

    //indexes suitable for clustering.
    def clusteringIndexesList = [

//           IndexEnum.NG3,
            IndexEnum.CRISIS3,
            IndexEnum.CLASSIC4,
//            IndexEnum.R4,
//            IndexEnum.R5,
//            IndexEnum.NG5,
//            IndexEnum.NG6,
//              IndexEnum.R6

    ]

    List<FitnessMethod> fitnessMethodsList = [

          //  FitnessMethod.PSEUDOF1,
            FitnessMethod.PSEUDOF1_K_PENALTY0_3
    ]

    List<QueryType> queryTypesList = [

            QueryType.OR3_INSTERSECT_SETK,
       //     QueryType.OR_INTERSECT_SETK

    ]

    List<IntersectMethod> intersectMethodList = [

            //     IntersectMethod.RATIO_POINT_3,
            IntersectMethod.RATIO_POINT_5,
            //   IntersectMethod.RATIO_POINT_7
    ]

    public ClusterMainECJ() {

        final Date startRun = new Date()

        NUMBER_OF_RUNS.times { int runNumber ->
            JobReport jobReport = new JobReport()
            clusteringIndexesList.each { IndexEnum ie ->

                JOBS_FOR_PSEUDO_F1_SELECTION.times { jobForPseudoF1Selection ->
                    EvolutionState state;

                    println "Index Enum ie: $ie"
                    Indexes.instance.setIndex(ie)

                    fitnessMethodsList.each { FitnessMethod fitnessMethod ->
                        ClusterFitness.fitnessMethod = fitnessMethod

                        intersectMethodList.each { IntersectMethod intersectMethod ->
                            QueryListFromChromosome.intersectMethod = intersectMethod

                            //    [true, false].each { intersectBool ->
                            [true].each { intersectBool ->
                                //  [false].each { intersectBool ->
                                QueryListFromChromosome.intersectTest = intersectBool


                                queryTypesList.each { qt ->
                                    println "query type $qt"
                                    ClusterQueryECJ.queryType = qt

                                    String parameterFilePath = qt.setk ? 'src/cfg/clusterGA_K.params' : 'src/cfg/clusterGA.params'

                                    ParameterDatabase parameters = new ParameterDatabase(new File(parameterFilePath));

                                    state = initialize(parameters, jobForPseudoF1Selection)
                                    if (JOBS_FOR_PSEUDO_F1_SELECTION >= 1) {
                                        final String jobFilePrefix = "jobForPseudoF1Selection." + jobForPseudoF1Selection;
                                        state.output.setFilePrefix(jobFilePrefix);
                                        state.checkpointPrefix = jobFilePrefix + state.checkpointPrefix;
                                    }
                                    //  state.parameters.set(new Parameter("generations"), "7")
                                    state.output.systemMessage("Job: " + jobForPseudoF1Selection);
                                    state.job = new Object[1]
                                    state.job[0] = new Integer(jobForPseudoF1Selection)

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

                                    jobReport.reportsOut(runNumber, jobForPseudoF1Selection, state.generation as int, popSize as int, numberOfSubpops, genomeSizePop0, wordListSizePop0, cfit)
                                }
                            }
                        }
                    }
                    cleanup(state);
                    println "--------END JOB $jobForPseudoF1Selection  -----------------------------------------------"

                }
                jobReport.overallSummary(runNumber)
            }

//            final Date endJob = new Date()
//            TimeDuration duration = TimeCategory.minus(endJob, startRun)
//            println "Duration: $duration"

        }

        final Date endRun = new Date()
        TimeDuration duration = TimeCategory.minus(endRun, startRun)
        println "Duration: $duration"
    }

    static main(args) {
        new ClusterMainECJ()
    }
}