package aseker00.tlp.model.disc;

import aseker00.tlp.estimation.LogLinearModelLogLikelihood;

public interface FeatureRegularization {
	public double featureFunctionValue(LogLinearModel llm, LogLinearModelLogLikelihood function);
	public FeatureVector featureFunctionGradient(LogLinearModel llm, LogLinearModelLogLikelihood function);
}
