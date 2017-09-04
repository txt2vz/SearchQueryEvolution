package clusterGP

import ec.EvolutionState
import ec.Problem
import ec.gp.ADFStack
import ec.gp.GPData
import ec.gp.GPIndividual
import ec.gp.GPNode
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery

public class OR2 extends GPNode
	{
	public String toString() { return " OR2 "; }

/*
  public void checkConstraints(final EvolutionState state,
  final int tree,
  final GPIndividual typicalIndividual,
  final Parameter individualBase)
  {
  super.checkConstraints(state,tree,typicalIndividual,individualBase);
  if (children.length!=2)
  state.output.error("Incorrect number of children for node " +
  toStringForError() + " at " +
  individualBase);
  }
*/
	public int expectedChildren() { return 2; }

	public void eval(final EvolutionState state,
                     final int thread,
                     final GPData input,
                     final ADFStack stack,
                     final GPIndividual individual,
                     final Problem problem)
		{
		def bqb = new BooleanQuery.Builder()		
		
		QueryData rd = ((QueryData)(input));
	
		rd.dummy=false;

		children[0].eval(state,thread,input,stack,individual,problem);
		bqb.add(rd.tq, BooleanClause.Occur.SHOULD)	
		
		children[1].eval(state,thread,input,stack,individual,problem);
		bqb.add(rd.tq, BooleanClause.Occur.SHOULD)
		
		rd.bq = bqb.build()
		}
	}