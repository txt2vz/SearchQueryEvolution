 package classify

 import ec.EvolutionState
 import ec.Evolve
 import ec.util.ParameterDatabase
 import groovy.time.TimeCategory
 import groovy.time.TimeDuration
 import index.ImportantTermsOld
 import index.IndexEnum
 import index.Indexes

 class GAmainClassify extends Evolve {

     private final String parameterFilePath = //'src/cfg/classifyOrSubpop.params'
             'src/cfg/classify.params'
     private int totPosMatchedTest = 0, totTest = 0, totNegMatchTest = 0;
     private final int NUMBER_OF_JOBS = 1
     private double microF1AllRunsTotal = 0, macroF1AllRunsTotal = 0, microBEPAllRunsTotal = 0;

     public GAmainClassify(){
         println "Start..."
         EvolutionState state;
         Indexes.instance.setIndex(IndexEnum.Science4)

         Formatter bestResultsOut = new Formatter('results/resultsClassify.csv');
         final String fileHead = "categoryName, categoryNumber, f1train, f1test, totPositiveTest, totNegativeTest, totTestDocsInCat, query" + '\n';

         ParameterDatabase parameters = null;
         final Date startTime = new Date();
         bestResultsOut.format("%s \n", startTime);
         bestResultsOut.format("Term selector: %s  \n", ImportantTermsOld.itm);
         bestResultsOut.format("%s", fileHead);

         (1..NUMBER_OF_JOBS).each{job ->
             parameters = new ParameterDatabase(new File(parameterFilePath));

             double sumF1test = 0;

             Indexes.NUMBER_OF_CATEGORIES.times{ categoryNumber ->

                 Indexes.instance.setCategoryNumber(String.valueOf(categoryNumber))
                 Indexes.instance.setIndexFieldsAndTotals()

                 state = initialize(parameters, job);

                 state.output.systemMessage("Job: " + job);
                 state.job = new Object[1];
                 state.job[0] = new Integer(job + categoryNumber);

                 if (NUMBER_OF_JOBS >= 1) {
                     final String jobFilePrefix = "job." + job + "." + categoryNumber
                     state.output.setFilePrefix(jobFilePrefix)
                     state.checkpointPrefix = jobFilePrefix 	+ state.checkpointPrefix
                 }
                 state.run(EvolutionState.C_STARTED_FRESH);

                 def popSize=0;
                 ClassifyFit cfit = (ClassifyFit) state.population.subpops.collect { sbp ->
                     popSize= popSize + sbp.individuals.size()
                     sbp.individuals.max() {ind ->
                         ind.fitness.fitness()
                     }.fitness
                 }.max  {it.fitness()}
                 println "pop size $popSize"

                 final double testF1 = cfit.f1test
                 final double trainF1 = cfit.f1train
                 sumF1test += testF1;

                 totPosMatchedTest += cfit.positiveMatchTest
                 totNegMatchTest += cfit.negativeMatchTest
                 totTest += Indexes.instance.totalTestDocsInCat;

                 println "cfit.getQueryMinimal: ${cfit.getQueryMinimal()}"

                 bestResultsOut.format(
                         "%s, %d, %.3f, %.3f, %d, %d, %d, %s \n",
                         Indexes.instance.getCategoryName(), categoryNumber, trainF1, testF1,
                         cfit.positiveMatchTest,
                         cfit.negativeMatchTest,
                         Indexes.instance.totalTestDocsInCat,
                         cfit.getQueryString() )
                 bestResultsOut.flush();
                 println "Test F1 for cat $categoryNumber : $testF1 *******************************"
                 cleanup(state);
             }

             final double microF1test = Effectiveness.f1(totPosMatchedTest,
                     totNegMatchTest, totTest);

             final double macroF1test = sumF1test / Indexes.NUMBER_OF_CATEGORIES;
             println "OVERALL test: micro f1: $microF1test  macroF1: $macroF1test "

             bestResultsOut.format(" \n");
             bestResultsOut.format("Run Number, %d", job );

             bestResultsOut
                     .format(", Micro F1test: , %.4f,  Macro F1test: , %.4f, Total Positive Matches , %d, Total Negative Matches, %d, Total Docs,  %d \n",
                     microF1test, macroF1test, totPosMatchedTest,
                     totNegMatchTest, totTest);

             macroF1AllRunsTotal = macroF1AllRunsTotal + macroF1test;
             microF1AllRunsTotal = microF1AllRunsTotal + microF1test;

             final double microAverageF1AllRuns = microF1AllRunsTotal / (job);
             final double macroAverageF1AllRuns = macroF1AllRunsTotal / (job);
			 
			 println  "${ImportantTermsOld.itm}  ALL Runs Micro: $microAverageF1AllRuns Macro: $macroAverageF1AllRuns ${Indexes.indexEnum.toString()}"

             bestResultsOut
                     .format(",, Overall Test Micro F1 , %.4f, Macro F1, %.4f",
                     microAverageF1AllRuns, macroAverageF1AllRuns
                     );

             totPosMatchedTest = 0;
             totNegMatchTest = 0;
             totTest = 0;

             bestResultsOut.format(" \n");
             bestResultsOut.format(" \n");
             bestResultsOut.flush();

             println " ---------------------------------END-----------------------------------------------"
         }

         final Date endTime = new Date()
         TimeDuration duration = TimeCategory.minus(endTime, startTime)
         println "Duration: $duration"

         bestResultsOut.close();
     }

     static main (args){
         new GAmainClassify()
     }
 }