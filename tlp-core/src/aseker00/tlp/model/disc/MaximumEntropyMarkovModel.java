package aseker00.tlp.model.disc;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import aseker00.tlp.estimation.Lbfgs;
import aseker00.tlp.estimation.LogLinearModelLogLikelihood;
import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
import aseker00.tlp.model.LabeledSequence;
import aseker00.tlp.model.LinearChainSequenceLabelModelImpl;
import aseker00.tlp.pos.Tagger;

public class MaximumEntropyMarkovModel extends LinearChainSequenceLabelModelImpl implements LogLinearModel {
	protected FeatureVector weights;
	protected FeatureFunction featureFunction;
	protected HashMap<Element, Double> norms;
	protected HashMap<Element, Set<Element>> conditionalEvents;

	public MaximumEntropyMarkovModel(String name, Tagger tagger, int tChain, int eChain, FeatureFunction ff) {
		super(name, tagger, tChain, eChain);
		this.featureFunction = ff;
		this.weights = ff.featureVectorNew();
		this.norms = new HashMap<Element, Double>();
		this.conditionalEvents = new HashMap<Element, Set<Element>>();
	}
	
	public void eventSetIs(Element condition, Set<Element> events) {
		this.conditionalEvents.put(condition, events);
	}
	
	public void featureFunctionTemplateIs(FeatureTemplate key, FeatureTemplate ft) {
		this.featureFunction.templateIs(key, ft);
	}
	
	public void normIs(Element condition, double value) {
		this.norms.put(condition, value);
	}

	public void weightsVectorIs(FeatureVector weights) {
		this.weights = weights;
	}

	@Override
	public FeatureVector parameters() {
		return this.weights;
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

	@Override
	public double probability(Element condition, Element event) {
		return 0;
	}

	@Override
	public double logProbability(Element condition, Element event) {
		return 0;
	}

	@Override
	public double conditionalProbability(LabeledSequence sequence, int pos) {
		FeatureVector fv = this.featureVector(sequence, pos);
		double p = this.parameters().dotProduct(fv);
		double e = Math.exp(p);
		double z = this.getNorm(sequence, pos);
		double value = e / z;
		return value;
	}

	@Override
	public double conditionalLogProbability(LabeledSequence sequence, int pos) {
		FeatureVector fv = this.featureVector(sequence, pos);
		double p = this.parameters().dotProduct(fv);
		double z = this.getNorm(sequence, pos);
		double g = Math.log(z);
		double value = p - g;
		return value;
	}

	@Override
	public Set<Element> conditions() {
		return this.conditionalEvents.keySet();
	}

	@Override
	public Set<Element> events(Element condition) {
		return this.conditionalEvents.get(condition);
	}

	@Override
	public int features() {
		return this.featureFunction.features().size();
	}

	@Override
	public FeatureFunction function() {
		return this.featureFunction;
	}
	
	public FeatureVector featureVectorNew() {
		return this.featureFunction.featureVectorNew();
	}

	public FeatureVector featureVector(LabeledSequence sequence, int pos) {
		return this.featureVector(sequence, pos, 1.0);
	}

	public FeatureVector featureVector(LabeledSequence sequence, int pos, double value) {
		return this.featureFunction.featureVector(sequence, pos, value);
	}
	
	public FeatureVector featureVector(Ngram transition, Ngram emission) {
		return this.featureVector(transition, emission, 1.0);
	}
	
	public FeatureVector featureVector(Ngram transition, Ngram emission, double value) {
		return this.featureFunction.featureVector(transition, emission, value);
	}

	protected double getNorm(LabeledSequence sequence, int pos) {
		return ((FeatureLabeledSequenceImpl) sequence).norm(pos);
	}
	
	protected double getNorm(Element condition) {
		return this.norms.getOrDefault(condition, 0.0);
	}

	@Override
	public List<Element> top(Element condition, int len) {
		// TODO Auto-generated method stub
		return null;
	}

	public LogLinearModelLogLikelihood logLikelihoodNew() {
		return new LogLinearModelLogLikelihood(this.featureVectorNew(), this.featureVectorNew());
	}

	public LogLinearModelLogLikelihood logLikelihoodStep(LabeledSequence sequence, Lbfgs lbfgs) {
		return lbfgs.logLikelihoodStep(sequence, this);
	}

	@Override
	public LabeledSequence sequenceNew(List<Element> entries) {
		return new FeatureLabeledSequenceImpl(entries);
	}

	@Override
	public LabeledSequence sequenceNew(List<Element> entries, List<Element> labels) {
		return new FeatureLabeledSequenceImpl(entries, labels);
	}
}