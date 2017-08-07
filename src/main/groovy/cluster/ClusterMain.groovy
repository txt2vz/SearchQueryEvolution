package cluster

import ec.EvolutionState
import ec.Evolve
import ec.util.ParameterDatabase
import index.IndexInfo

//@groovy.transform.TypeChecked
class ClusterMain extends Evolve {

	private final String parameterFilePath = 
	
	// 'src/cfg/clusterGP.params'
	 'src/cfg/clusterGA.params'

	private final int NUMBER_OF_JOBS = 3

	public ClusterMain(){  
		EvolutionState state;	
		IndexInfo.instance.setIndexFieldsAndTotals() 
		ParameterDatabase parameters  = null;
 
		final Date startRun = new Date();

			(1..NUMBER_OF_JOBS).each{job ->
			parameters = new ParameterDatabase(new File(parameterFilePath));

			state = initialize(parameters, job);
			state.output.systemMessage("Job: " + job);
			state.job = new Object[1];
			state.job[0] = new Integer(job); 

			if (NUMBER_OF_JOBS >= 1) {
				final String jobFilePrefix = "job." + job;
				state.output.setFilePrefix(jobFilePrefix);
				state.checkpointPrefix = jobFilePrefix 	+ state.checkpointPrefix;
			}
			state.run(EvolutionState.C_STARTED_FRESH);
			
			def popSize=0;
			ClusterFit cfit = (ClusterFit) state.population.subpops.collect { sbp ->
				popSize= popSize + sbp.individuals.size()
				sbp.individuals.max() {ind ->
					ind.fitness.fitness()
				}.fitness
			}.max  {it.fitness()}
			println "Population size: $popSize" 
			cfit.queryStats(job, state.generation, popSize)	

			cleanup(state);
			println ' ---------------------------------END-----------------------------------------------'
		}

		final Date endRun = new Date();
		def time= endRun.getTime() - startRun.getTime();
		println "Total time taken: $time"
	}

	static main (args){
		new ClusterMain()
	}
}