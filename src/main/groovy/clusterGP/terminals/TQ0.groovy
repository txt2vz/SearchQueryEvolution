package clusterGP.terminals

import clusterGP.QueryData
import ec.EvolutionState
import ec.Problem
import ec.gp.ADFStack
import ec.gp.GPData
import ec.gp.GPIndividual
import ec.gp.GPNode

public class TQ0 extends GPNode {
	public String toString() {
		return "tq0: "
	}

	public int expectedChildren() {
		return 0;
	}

	public void eval(final EvolutionState state,
                     final int thread,
                     final GPData input,
                     final ADFStack stack,
                     final GPIndividual individual,
                     final Problem problem) {
			
		QueryData rd = ((QueryData)(input));
		rd.tq = rd.termQueryArray[0]		
	}
}