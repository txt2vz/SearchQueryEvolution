package cluster

import classify.ClassifyUnassigned
import classify.LuceneClassifyMethod
import classify.UpdateAssignedFieldInIndex
import ec.EvolutionState
import ec.Evolve
import ec.util.ParameterDatabase
import ec.util.Parameter
import groovy.time.TimeCategory
import groovy.time.TimeDuration
import groovy.transform.CompileStatic
import index.IndexEnum
import index.Indexes
import org.apache.lucene.classification.Classifier

@CompileStatic
class ClusterMainECJ extends Evolve {

    final static int NUMBER_OF_JOBS = 5
    final static boolean onlyDocsInOneCluster = false
    final static boolean luceneClassify = true
    final static boolean useSameIndexForEffectivenessMeasure = true

    //indexes suitable for clustering.
    List<Tuple2<IndexEnum, IndexEnum>> clusteringIndexes = [

            new Tuple2<IndexEnum, IndexEnum>(IndexEnum.R4, IndexEnum.R4TEST),
            new Tuple2<IndexEnum, IndexEnum>(IndexEnum.R5, IndexEnum.R5TEST),
            new Tuple2<IndexEnum, IndexEnum>(IndexEnum.R6, IndexEnum.R6TEST),

            new Tuple2<IndexEnum, IndexEnum>(IndexEnum.NG3, IndexEnum.NG3TEST),
            new Tuple2<IndexEnum, IndexEnum>(IndexEnum.NG5, IndexEnum.NG5TEST),
            new Tuple2<IndexEnum, IndexEnum>(IndexEnum.NG6, IndexEnum.NG6TEST),

            new Tuple2<IndexEnum, IndexEnum>(IndexEnum.CLASSIC4, IndexEnum.CLASSIC4TEST),

            new Tuple2<IndexEnum, IndexEnum>(IndexEnum.CRISIS3, IndexEnum.CRISIS3TEST)
    ]

    List<Double> kPenalty = [0.04d]
    //       [0.0d, 0.01d, 0.02d, 0.03d, 0.04d, 0.05d, 0.06d, 0.07d, 0.08d, 0.09d, 0.1d]


    List<QueryType> queryTypesList = [

            QueryType.OR1,
            QueryType.OR1SETK,

            //       QueryType.OR,
            //     QueryType.OR_SETK
    ]

    List<IntersectMethod> intersectMethodList = [
            IntersectMethod.RATIO_POINT_5
    ]

    List<LuceneClassifyMethod> classifyMethodList = [
            LuceneClassifyMethod.KNN,
            //       LuceneClassifyMethod.NB
    ]

    ClusterMainECJ() {

        final Date startRun = new Date()

        File timingFile = new File("results/timing.csv")
        File queryFile = new File('results/qFile.txt')
        if (!timingFile.exists()) {
            timingFile << 'index, queryType, GAtime, KNNtime, overallTime \n'
        }

        Analysis analysis = new Analysis()

        clusteringIndexes.each { Tuple2<IndexEnum, IndexEnum> trainTestIndexes ->

            NUMBER_OF_JOBS.times { job ->
                EvolutionState state = new EvolutionState()

                println "Index Enum trainTestIndexes: $trainTestIndexes"
                Indexes.setIndex(trainTestIndexes.first)

                kPenalty.each { kPenalty ->
                    ClusterFitness.kPenalty = kPenalty

                    queryTypesList.each { qt ->
                        println "query type $qt"
                        ClusterQueryECJ.queryType = qt
                        String parameterFilePath = qt.setk ? 'src/cfg/clusterGA_K.params' : 'src/cfg/clusterGA.params'

                        ClusterFitness.fitnessMethod = qt.setk ? FitnessMethod.UNIQUE_HITS_K_PENALTY : FitnessMethod.UNIQUE_HITS_COUNT

                        intersectMethodList.each { IntersectMethod intersectMethod ->
                            final Date indexTime = new Date()
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
                            ClusterFitness clusterFitness = (ClusterFitness) state.population.subpops.collect { sbp ->
                                popSize = popSize + sbp.individuals.size()
                                sbp.individuals.max() { ind ->
                                    ind.fitness.fitness()
                                }.fitness
                            }.max { it.fitness() }

                            final int numberOfSubpops = state.parameters.getInt(new Parameter("pop.subpops"), new Parameter("pop.subpops"))
                            final int wordListSizePop0 = state.parameters.getInt(new Parameter("pop.subpop.0.species.max-gene"), new Parameter("pop.subpop.0.species.max-gene"))
                            final int genomeSizePop0 = state.parameters.getInt(new Parameter("pop.subpop.0.species.genome-size"), new Parameter("pop.subpop.0.species.genome-size"))
                            println "wordListSizePop0: $wordListSizePop0 genomeSizePop0 $genomeSizePop0  subPops $numberOfSubpops"

                            final Date GATime = new Date()
                            TimeDuration durationGA = TimeCategory.minus(new Date(), indexTime)
                            timingFile << trainTestIndexes.first.name() + ",  " + qt + ",  " + durationGA.toMilliseconds()

                            if (luceneClassify) {

                                clusterFitness.queriesToFile(queryFile)
                                UpdateAssignedFieldInIndex.updateAssignedField(trainTestIndexes.first, queryFile, onlyDocsInOneCluster)

                                classifyMethodList.each { classifyMethod ->
                                    Classifier classifier = ClassifyUnassigned.classifyUnassigned(trainTestIndexes.first, classifyMethod)

                                    TimeDuration durationKNN = TimeCategory.minus(new Date(), GATime)
                                    TimeDuration overallTime = TimeCategory.minus(new Date(), indexTime)
                                    timingFile << ",  " + durationKNN.toMilliseconds() + ', ' + overallTime.toMilliseconds() + '\n'
                                    IndexEnum checkEffectifnessIndex = useSameIndexForEffectivenessMeasure ? trainTestIndexes.first : trainTestIndexes.second

                                    Tuple3 t3ClassiferResult = Effectiveness.classifierEffectiveness(classifier, checkEffectifnessIndex, clusterFitness.k)

                                    analysis.reportsOut(job, state.generation as int, popSize as int, numberOfSubpops, genomeSizePop0, wordListSizePop0, clusterFitness, t3ClassiferResult, classifyMethod, onlyDocsInOneCluster)
                                }
                            } else {

                                analysis.reportsOut(job, state.generation as int, popSize as int, numberOfSubpops, genomeSizePop0, wordListSizePop0, clusterFitness, null, null, onlyDocsInOneCluster)
                            }
                        }
                    }
                    cleanup(state);
                    println "--------END JOB $job  -----------------------------------------------"
                }
            }
        }

        analysis.jobSummary()

        final Date endRun = new Date()
        TimeDuration duration = TimeCategory.minus(endRun, startRun)
        println "Duration: $duration"
    }

    static main(args) {
        new ClusterMainECJ()
    }
}