package aseker00.tlp.model.disc;

import aseker00.tlp.model.ConditionalProbabilityDistribution;
import aseker00.tlp.model.LabeledSequence;

public interface LogLinearModel extends ConditionalProbabilityDistribution {
	public int features();
	public FeatureFunction function();
	public FeatureVector parameters();
	public double conditionalProbability(LabeledSequence sequence, int pos);
	public double conditionalLogProbability(LabeledSequence sequence, int pos);
}