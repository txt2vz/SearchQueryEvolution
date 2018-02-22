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

            // 'src/cfg/clusterGP.params'
            'src/cfg/clusterGA.params'

    private final int NUMBER_OF_JOBS = 1

  //  def indexes = [IndexEnum.CLASSIC4, IndexEnum.CRISIS3, IndexEnum.OHS3, IndexEnum.NG3]
    def indexes = [ IndexEnum.CLASSIC4, IndexEnum.NG3]

    public ClusterMainECJ() {
        EvolutionState state;    
        ParameterDatabase parameters = null;
        final Date startRun = new Date();
	//	IndexInfo.instance.setIndex(IndexEnum.CRISIS3)
        def f1Totals = []
        indexes.each { ie ->
            //IndexEnum.values().each { ie ->
            // for (IndexEnum ie: IndexEnum.values()){
            println "ie $ie"
            IndexInfo.instance.setIndex(ie)

            //IndexInfo.indexEnum = IndexEnum.NG3
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
                println "Population size: $popSize"
                cfit.finalQueryStats(job, state.generation as int, popSize as int)

                f1Totals << cfit.averageF1

                cleanup(state);
                println ' ---------------------------------END-----------------------------------------------'
            }
        }
            final Date endRun = new Date();
            def time = endRun.getTime() - startRun.getTime();
            println "Total time taken: $time"
            println "F1 list $f1Totals"
            println " F1 Average " +  f1Totals.sum() / f1Totals.size()

        //}
    }

    static main(args) {
        new ClusterMainECJ()
    }
}