package aseker00.tlp.model.disc;

import java.util.List;

import aseker00.tlp.estimation.Lbfgs;
import aseker00.tlp.estimation.LogLinearModelLogLikelihood;
import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
import aseker00.tlp.model.LabeledSequence;
import aseker00.tlp.model.Viterbi;
import aseker00.tlp.pos.Tagger;

public class ConditionalRandomField extends MaximumEntropyMarkovModel {

	public ConditionalRandomField(String name, Tagger tagger, int tChain, int eChain, FeatureFunction ff) {
		super(name, tagger, tChain, eChain, ff);
	}

	public double conditionalProbability(LabeledSequence sequence) {
		FeatureVector globalFeatureVector = this.globalFeatureVector(sequence);
		double value = this.parameters().dotProduct(globalFeatureVector);
		double e = Math.exp(value);
		double z = this.getNorm(sequence);
		double p = e / z;
		return p;
	}

	public double conditionalLogProbability(LabeledSequence sequence) {
		FeatureVector globalFeatureVector = this.globalFeatureVector(sequence);
		double value = this.parameters().dotProduct(globalFeatureVector);
		double z = this.getNorm(sequence);
		double logp = value - Math.log(z);
		return logp;
	}

	@Override
	public double conditionalLabelProbability(LabeledSequence sequence) {
		return sequence.conditionalLabelProbability(this);
	}

	@Override
	public double conditionalLabelLogProbability(LabeledSequence sequence) {
		return sequence.conditionalLabelLogProbability(this);
	}

	@Override
	public double potential(LabeledSequence sequence, int pos) {
		return sequence.potential(this, pos);
	}

	@Override
	public double logPotential(LabeledSequence sequence, int pos) {
		return sequence.logPotential(this, pos);
	}

	@Override
	public double decodedPotential(Viterbi viterbi, LabeledSequence sequence, int pos) {
		double p = this.logPotential(sequence, pos);
		Ngram transition = this.transition(sequence, pos);
		Ngram prefix = transition.subgram(0, this.chain() - 1);
		double value = viterbi.chartValue(pos, prefix);
		return value + p;
	}

	@Override
	public LabeledSequence sequenceNew(List<Element> entries) {
		return new GlobalFeatureLabeledSequenceImpl(entries);
	}

	public LabeledSequence sequenceNew(List<Element> entries, List<Element> labels) {
		return new GlobalFeatureLabeledSequenceImpl(entries, labels);
	}

	public FeatureVector globalFeatureVector(LabeledSequence sequence) {
		FeatureVector globalVector = this.featureFunction.featureVectorNew();
		for (int i = 0; i <= sequence.length(); i++) {
			FeatureVector localVector = this.featureVector(sequence, i);
			globalVector = globalVector.addition(localVector);
		}
		return globalVector;
	}

	private double getNorm(LabeledSequence sequence) {
		return ((GlobalFeatureLabeledSequenceImpl) sequence).norm();
	}

	@Override
	public LogLinearModelLogLikelihood logLikelihoodStep(LabeledSequence sequence, Lbfgs lbfgs) {
		return lbfgs.logLikelihoodStep(sequence, this);
	}
}