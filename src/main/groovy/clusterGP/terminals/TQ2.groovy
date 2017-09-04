package clusterGP.terminals

import clusterGP.QueryData
import ec.EvolutionState
import ec.Problem
import ec.gp.ADFStack
import ec.gp.GPData
import ec.gp.GPIndividual
import ec.gp.GPNode

public class TQ2 extends GPNode {
	public String toString() {
		return "tq2: " //+ rd.tq.getTerm().getText()
	}

	/*
	 public void checkConstraints(final EvolutionState state,
	 final int tree,
	 final GPIndividual typicalIndividual,
	 final Parameter individualBase)
	 {
	 super.checkConstraints(state,tree,typicalIndividual,individualBase);
	 if (children.length!=0)
	 state.output.error("Incorrect number of children for node " +
	 toStringForError() + " at " +
	 individualBase);
	 }
	 */
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
		rd.dummy=false;
		rd.tq = rd.termQueryArray[2]

	}
}