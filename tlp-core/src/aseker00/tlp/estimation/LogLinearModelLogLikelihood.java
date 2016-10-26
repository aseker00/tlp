package aseker00.tlp.estimation;

import aseker00.tlp.model.disc.FeatureVector;

public class LogLinearModelLogLikelihood implements LogLikelihoodFunction {
	private double value;
	private FeatureVector sumFeatures;
	private FeatureVector sumExpectedFeatures;

	public LogLinearModelLogLikelihood(FeatureVector sumFeatures, FeatureVector sumExpectedFeatures) {
		this.sumFeatures = sumFeatures;
		this.sumExpectedFeatures = sumExpectedFeatures;
	}

	@Override
	public LogLikelihoodFunction additionInc(LogLikelihoodFunction other) {
		LogLinearModelLogLikelihood llmllf = (LogLinearModelLogLikelihood) other;
		this.valueInc(llmllf.value);
		this.sumFeaturesInc(llmllf.sumFeatures);
		this.sumExpectedFeaturesInc(llmllf.sumExpectedFeatures);
		return this;
	}

	public void valueInc(double value) {
		this.value += value;
	}

	@Override
	public double value() {
		return this.value;
	}

	public void sumFeaturesInc(FeatureVector v) {
		this.sumFeatures.additionInc(v);
	}

	public FeatureVector sumFeatures() {
		return this.sumFeatures;
	}

	public void sumExpectedFeaturesInc(FeatureVector v) {
		this.sumExpectedFeatures.additionInc(v);
	}

	public FeatureVector sumExpectedFeatures() {
		return this.sumExpectedFeatures;
	}
}