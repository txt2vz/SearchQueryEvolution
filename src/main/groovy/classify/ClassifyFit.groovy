package classify

import ec.simple.SimpleFitness
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import index.Indexes
import org.apache.lucene.search.Query

/**
 * Store information about classification classify.query and test/train values
 * 
 * @author Laurie
 * 
 */
@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
public class ClassifyFit extends SimpleFitness {

	double f1train, f1test, BEPTest, tree;
	int positiveMatchTrain, negativeMatchTrain;
	int positiveMatchTest, negativeMatchTest, numberOfTerms = 0;
	Query query;

	int totalPositiveScore=0
	int totalNegativeScore=0;
	int totalPosHits=0
	int totalNegHits=0
	int duplicateCount=0;
	int noHitsCount=0;
	
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

	@TypeChecked(TypeCheckingMode.SKIP)
	public String toString(int gen) {
		return "Gen: $gen  F1: $f1train  Positive Match: $positiveMatchTrain Negative Match: $negativeMatchTrain "
		+ " Total positive Docs: " + Indexes.instance.totalTrainDocsInCat
		+ '\n' + "QueryString: " + query.toString(Indexes.FIELD_CONTENTS) + '\n';
	}
}