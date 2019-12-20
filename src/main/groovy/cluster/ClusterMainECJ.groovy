package cluster

import classify.ClassifyUnassigned
import index.Effectiveness
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

    static final int NUMBER_OF_JOBS = 2

    //indexes suitable for clustering.
    List <Tuple2 <IndexEnum, IndexEnum>> clusteringIndexes = [

            new Tuple2<IndexEnum, IndexEnum>(IndexEnum.R4TRAIN, IndexEnum.R4TEST),
            new Tuple2<IndexEnum, IndexEnum>(IndexEnum.R5TRAIN, IndexEnum.R5TEST),
           new Tuple2<IndexEnum, IndexEnum>(IndexEnum.R6TRAIN, IndexEnum.R6TEST),

             new Tuple2<IndexEnum, IndexEnum>(IndexEnum.NG3TRAIN, IndexEnum.NG3TEST),
           new Tuple2<IndexEnum, IndexEnum>(IndexEnum.NG5TRAIN, IndexEnum.NG5TEST),
           new Tuple2<IndexEnum, IndexEnum>(IndexEnum.NG6TRAIN, IndexEnum.NG6TEST),

           new Tuple2<IndexEnum, IndexEnum>(IndexEnum.CLASSIC4TRAIN, IndexEnum.CLASSIC4TEST),

           new Tuple2<IndexEnum, IndexEnum>(IndexEnum.CRISIS3TRAIN, IndexEnum.CRISIS3TEST)
    ]

    List<Double> kPenalty =     [0.04d]
     //       [0.0d, 0.01d, 0.02d, 0.03d, 0.04d, 0.05d, 0.06d, 0.07d, 0.08d, 0.09d, 0.1d]


    List<QueryType> queryTypesList = [

         QueryType.OR1,
         QueryType.OR1SETK,

         //  QueryType.OR,
    //     QueryType.OR_SETK
          //  QueryType.MINSHOULD2,
     //       QueryType.AND
       //     QueryType.OR_WITH_MINSHOULD2
    ]

    List<IntersectMethod> intersectMethodList = [
            IntersectMethod.RATIO_POINT_5
    ]

    List <LuceneClassifyMethod> classifyMethodList = [
            LuceneClassifyMethod.KNN,
   //       LuceneClassifyMethod.NB
    ]

    final static boolean luceneClassify = true
    final static boolean useSameIndexForEffectivenessMeasure = true

    ClusterMainECJ() {

        final Date startRun = new Date()

        File timingFile = new File("results/timing.txt")
        File queryFile = new File('results/qFile.txt')

        Analysis analysis = new Analysis()

        clusteringIndexes.each { Tuple2<IndexEnum, IndexEnum> trainTestIndexes ->
            final Date indexTime = new Date()
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

                            if (luceneClassify) {

                                clusterFitness.queriesToFile(queryFile)
                                UpdateAssignedFieldInIndex.updateAssignedField(trainTestIndexes.first, queryFile)

                                classifyMethodList.each { classifyMethod ->
                                    println " "
                                    println "Lucene Classify Method: $classifyMethod"
                                    Classifier classifier = ClassifyUnassigned.classifyUnassigned(trainTestIndexes.first, classifyMethod)

                                    IndexEnum checkEffectifnessIndex = useSameIndexForEffectivenessMeasure ? trainTestIndexes.first : trainTestIndexes.second

                                    Tuple3 t3ClassiferResult = Effectiveness.classifierEffectiveness(classifier, checkEffectifnessIndex, clusterFitness.k)
                                //    Tuple3 t3ClassiferResult = Effectiveness.classifierEffectiveness(classifier, trainTestIndexes.second, clusterFitness.k)
                                   // Indexes.setIndex(trainTestIndexes.first)  //GA evaluation on train index
                                    analysis.reportsOut(job, state.generation as int, popSize as int, numberOfSubpops, genomeSizePop0, wordListSizePop0, clusterFitness, t3ClassiferResult, classifyMethod)
                                }
                            } else {

                                analysis.reportsOut(job, state.generation as int, popSize as int, numberOfSubpops, genomeSizePop0, wordListSizePop0, clusterFitness,null,null)
                            }
                        }
                    }
                    cleanup(state);
                    println "--------END JOB $job  -----------------------------------------------"
                }
            }

            final Date endTime = new Date()
            TimeDuration durationT = TimeCategory.minus(endTime, indexTime)
            println "Duration: $durationT"
            String s =  trainTestIndexes.toString() + "  " + durationT + '\n'
            timingFile << s
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