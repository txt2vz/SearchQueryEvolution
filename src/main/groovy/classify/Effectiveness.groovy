package classify

@groovy.transform.CompileStatic
@groovy.transform.TypeChecked

public class Effectiveness {

	public static double precision(final int positiveMatch, final int negativeMatch) {
		final int totalRetrieved = positiveMatch + negativeMatch;
		if (totalRetrieved > 0)
			return (double) positiveMatch / totalRetrieved;
		else
			return 0.0;
	}

	public static double recall(final int positiveMatch, final int totalPositive) {

		if (totalPositive > 0)
			return (double) positiveMatch / totalPositive;
		else
			return 0.0;
	}

	/**
	 * Fitness is based on the F1 measure which combines precision and recall
	 */
	public static double f1(final int positiveMatch, final int negativeMatch, final int totalPositive) {

		if (positiveMatch <= 0 || totalPositive <= 0) {
			return 0.0;
		}

		final double recall = recall(positiveMatch, totalPositive);
		final double precision = precision(positiveMatch, negativeMatch);

		return (2 * precision * recall) / (precision + recall);
	}

	/**
	 * Break even point. Alternative (older) measure of classification accuracy
	 */
	public static double bep(int positiveMatch, int negativeMatch, int totalPositive) {

		if (positiveMatch <= 0 || totalPositive <= 0) {
			return 0.0;
		}
		final double recall = recall(positiveMatch, totalPositive);
		final double precision = precision(positiveMatch, negativeMatch);

		return (precision + recall) / 2;
	}
}