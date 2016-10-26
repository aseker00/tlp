package aseker00.tlp.model.disc;

import aseker00.tlp.estimation.LogLinearModelLogLikelihood;

public class L2Regularization implements FeatureRegularization {
	double lambda;
	public void lambdaIs(double d) {
		this.lambda = d;
	}
	public double featureFunctionValue(LogLinearModel llm, LogLinearModelLogLikelihood function) {
		//return function.value() - this.lambda*llm.parameters().norm2()/2;
		//double norm = llm.parameters().norm();
		//double norm2 = llm.parameters().norm2();
		return function.value() - this.lambda*llm.parameters().norm2();
	}
	public FeatureVector featureFunctionGradient(LogLinearModel llm, LogLinearModelLogLikelihood function) {
		//return function.sumFeatures().subtraction(function.sumExpectedFeatures()).subtraction(llm.parameters().product(this.lambda));
		return function.sumFeatures().subtraction(function.sumExpectedFeatures()).subtraction(llm.parameters().product(2*this.lambda));
	}
}