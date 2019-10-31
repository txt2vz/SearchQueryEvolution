package classify

import ec.simple.SimpleFitness
import index.Indexes
import org.apache.lucene.search.Query

@groovy.transform.CompileStatic
public class ClassifyFit extends SimpleFitness {

	double f1train, f1test, BEPTest
	int positiveMatchTrain, negativeMatchTrain, positiveMatchTest, negativeMatchTest
	Query query;
	
	public String getQueryString(){
		return query.toString(Indexes.FIELD_CONTENTS)
	}

	public String getQueryMinimal() {
		return QueryReadable.getQueryMinimal(query);
	}

	public String getQueryJSONForViz(){
		return QueryReadable.getQueryJSONForViz(query);
	}

	public String fitnessToStringForHumans() {
		return  "F1train: $f1train  fitness: " + this.fitness();
	}

	public String toString(int gen) {
		return "Gen: $gen  F1: $f1train  Positive Match: $positiveMatchTrain Negative Match: $negativeMatchTrain " +
		" Total positive Docs: " + Indexes.instance.totalTrainDocsInCat +
		 '\n' + "QueryString: " + query.toString(Indexes.FIELD_CONTENTS) + '\n';
	}
}