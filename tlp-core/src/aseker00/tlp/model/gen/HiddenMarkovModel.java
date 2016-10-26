package aseker00.tlp.model.gen;

import java.util.List;
import java.util.Set;

import aseker00.tlp.estimation.BaumWelch;
import aseker00.tlp.estimation.BaumWelchWithFeatures;
import aseker00.tlp.estimation.MaximumLikelihoodEstimation;
import aseker00.tlp.estimation.RelativeFrequencyLogLikelihood;
import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
import aseker00.tlp.model.ConditionalProbabilityDistribution;
import aseker00.tlp.model.ForwardBackward;
import aseker00.tlp.model.LabeledSequence;
import aseker00.tlp.model.LabeledSequenceImpl;
import aseker00.tlp.model.LinearChainSequenceLabelModelImpl;
import aseker00.tlp.pos.Tagger;

public class HiddenMarkovModel extends LinearChainSequenceLabelModelImpl {
	private ConditionalProbabilityDistribution emissionDist;
	private ConditionalProbabilityDistribution transitionDist;
	
	public HiddenMarkovModel(String name, Tagger tagger, int tChain, int eChain) {
		super(name, tagger, tChain, eChain);
	}
	
	public void emissionProbabilityDistributionIs(ConditionalProbabilityDistribution dist) {
		this.emissionDist = dist;
	}
	
	public ConditionalProbabilityDistribution emissionProbabilityDistribution() {
		return this.emissionDist;
	}
	
	public void transitionProbabilityDistributionIs(ConditionalProbabilityDistribution dist) {
		this.transitionDist = dist;
	}
	
	public ConditionalProbabilityDistribution transitionProbabilityDistribution() {
		return this.transitionDist;
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
	public double conditionalLabelProbability(LabeledSequence sequence) {
		return sequence.conditionalLabelProbability(this);
	}
	
	@Override
	public double conditionalLabelLogProbability(LabeledSequence sequence) {
		return sequence.conditionalLabelLogProbability(this);
	}
	
	public double jointProbability(LabeledSequence sequence) {
		double value = 1;
		for (int i = 0; i <= sequence.length(); i++)
			value *= this.jointProbability(sequence, i);
		return value;
	}
	
	public double jointLogProbability(LabeledSequence sequence) {
		double value = 0;
		for (int i = 0; i <= sequence.length(); i++)
			value += this.jointLogProbability(sequence, i);
		return value;
	}
	
	public double marginalObservationProbability(LabeledSequence sequence) {
		ForwardBackward fb = new ForwardBackward(sequence);
		fb.modelIs(this);
		double z = fb.value();
		return z;
	}
	
	public double marginalObservationLogProbability(LabeledSequence sequence) {
		ForwardBackward fb = new ForwardBackward(sequence);
		fb.modelIs(this);
		double z = fb.value();
		double logz = Math.log(z);
		return logz;
	}
	
	public double priorLabelProbability(LabeledSequence sequence) {
		double value = 1;
		for (int i = 0; i <= sequence.length(); i++)
			value *= this.priorLabelProbability(sequence, i);
		return value;
	}
	
	public double priorLabelLogProbability(LabeledSequence sequence) {
		double value = 0;
		for (int i = 0; i <= sequence.length(); i++)
			value += this.priorLabelLogProbability(sequence, i);
		return value;
	}
	
	public double posteriorObservationProbability(LabeledSequence sequence) {
		double value = 1;
		for (int i = 0; i <= sequence.length(); i++)
			value *= this.posteriorObservationProbability(sequence, i);
		return value;
	}
	
	public double posteriorObservationLogProbability(LabeledSequence sequence) {
		double value = 0;
		for (int i = 0; i <= sequence.length(); i++)
			value += this.posteriorObservationLogProbability(sequence, i);
		return value;
	}

	public double jointProbability(LabeledSequence sequence, int pos) {
		if (pos == sequence.length())
			return this.priorLabelProbability(sequence, pos);
		return this.priorLabelProbability(sequence, pos) * this.posteriorObservationProbability(sequence, pos);
	}
	
	public double jointLogProbability(LabeledSequence sequence, int pos) {
		if (pos == sequence.length())
			return this.priorLabelLogProbability(sequence, pos);
		return this.priorLabelLogProbability(sequence, pos) + this.posteriorObservationLogProbability(sequence, pos);
	}
	
	public double priorLabelProbability(LabeledSequence sequence, int pos) {
		Ngram transition = this.transition(sequence, pos);
		Ngram condition = transition.subgram(0, transition.size()-1);
		Element event = transition.entry(transition.size()-1);
		double p = this.transitionDist.probability(condition, event);
		return p;
	}
	
	public double priorLabelLogProbability(LabeledSequence sequence, int pos) {
		Ngram transition = this.transition(sequence, pos);
		Ngram condition = transition.subgram(0, transition.size()-1);
		Element event = transition.entry(transition.size()-1);
		return this.transitionDist.logProbability(condition, event);
	}
	
	public double posteriorObservationProbability(LabeledSequence sequence, int pos) {
		Ngram emission = this.emission(sequence, pos);
		Ngram condition = emission.subgram(0, emission.size()-1);
		Element event = emission.entry(emission.size()-1);
		double p = this.emissionDist.probability(condition, event);
		return p;
	}
	
	public double posteriorObservationLogProbability(LabeledSequence sequence, int pos) {
		Ngram emission = this.emission(sequence, pos);
		Ngram condition = emission.subgram(0, emission.size()-1);
		Element event = emission.entry(emission.size()-1);
		return this.emissionDist.logProbability(condition, event);
	}
	
	public RelativeFrequencyLogLikelihood logLikelihoodNew() {
		return new RelativeFrequencyLogLikelihood();
	}

	@Override
	public LabeledSequence sequenceNew(List<Element> entries) {
		return new LabeledSequenceImpl(entries);
	}

	@Override
	public LabeledSequence sequenceNew(List<Element> entries, List<Element> labels) {
		return new LabeledSequenceImpl(entries, labels);
	}
	
	public RelativeFrequencyLogLikelihood logLikelihoodStep(LabeledSequence sequence, MaximumLikelihoodEstimation mle) {
		return mle.logLikelihoodStep(sequence, this);
	}
	
	public RelativeFrequencyLogLikelihood logLikelihoodStep(LabeledSequence sequence, BaumWelch bw) {
		return bw.logLikelihoodStep(sequence, this);
	}
	
	public void logLikelihoodPreStep(LabeledSequence sequence, BaumWelchWithFeatures bw2, Set<Element> tokens) {
		bw2.logLikelihoodPreStep(sequence, this, tokens);
	}
}