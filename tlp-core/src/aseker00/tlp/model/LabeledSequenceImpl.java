package aseker00.tlp.model;

import java.util.ArrayList;
import java.util.List;

import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
import aseker00.tlp.model.disc.ConditionalRandomField;
import aseker00.tlp.model.disc.FeatureVector;
import aseker00.tlp.model.disc.MaximumEntropyMarkovModel;
import aseker00.tlp.model.gen.HiddenMarkovModel;

public class LabeledSequenceImpl implements LabeledSequence {
	private ArrayList<Element> elements;
	private ArrayList<Element> labels;

	public LabeledSequenceImpl(List<Element> elements) {
		this.elements = new ArrayList<Element>(elements);
		this.labels = new ArrayList<Element>();
		for (int i = 0; i < this.elements.size(); i++)
			this.labels.add(null);
	}

	public LabeledSequenceImpl(List<Element> elements, List<Element> labels) {
		this.elements = new ArrayList<Element>(elements);
		this.labels = new ArrayList<Element>(labels);
	}

	@Override
	public int length() {
		return this.elements.size();
	}

	@Override
	public Element entry(int pos) {
		return this.elements.get(pos);
	}

	@Override
	public List<Element> entriesList() {
		return this.elements;
	}

	@Override
	public Element label(int pos) {
		return this.labels.get(pos);
	}

	@Override
	public void labelIs(int pos, Element label) {
		this.labels.set(pos, label);
	}

	@Override
	public List<Element> labelList() {
		return this.labels;
	}

	@Override
	public void labelListIs(List<Element> labels) {
		this.labels = new ArrayList<Element>(labels);
	}

	@Override
	public void labelTransitionIs(int pos, Ngram transition) {
		for (int i = 0; i < transition.size(); i++) {
			if (pos - i < 0)
				continue;
			if (pos - i == this.length())
				continue;
			Element label = transition.entry(transition.size() - 1 - i);
			this.labels.set(pos - i, label);
		}
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("[");
		for (int i = 0; i < this.length(); i++) {
			Element label = this.label(i);
			Element element = this.entry(i);
			sb.append(label).append("/").append(element);
			if (i < this.length() - 1)
				sb.append(", ");
		}
		sb.append("]");
		return sb.toString();
	}

	public double jointProbability(HiddenMarkovModel hmm) {
		double p = 1.0;
		for (int i = 0; i <= this.length(); i++) {
			double value = hmm.jointProbability(this, i);
			p *= value;
		}
		return p;
	}

	public double jointLogProbability(HiddenMarkovModel hmm) {
		double logp = 0.0;
		for (int i = 0; i <= this.length(); i++) {
			double value = hmm.jointLogProbability(this, i);
			logp += value;
		}
		return logp;
	}

	@Override
	public double potential(HiddenMarkovModel hmm, int pos) {
		return hmm.jointProbability(this, pos);
	}

	@Override
	public double logPotential(HiddenMarkovModel hmm, int pos) {
		return hmm.jointLogProbability(this, pos);
	}

	@Override
	public double potential(MaximumEntropyMarkovModel memm, int pos) {
		return memm.conditionalProbability(this, pos);
	}

	@Override
	public double logPotential(MaximumEntropyMarkovModel memm, int pos) {
		return memm.conditionalLogProbability(this, pos);
	}

	@Override
	public double potential(ConditionalRandomField crf, int pos) {
		return Math.exp(this.logPotential(crf, pos));
	}

	@Override
	public double logPotential(ConditionalRandomField crf, int pos) {
		FeatureVector w = crf.parameters();
		FeatureVector f = crf.featureVector(this, pos);
		return w.dotProduct(f);
	}

	@Override
	public double conditionalLabelProbability(HiddenMarkovModel hmm) {
		double py = hmm.priorLabelProbability(this);
		double pxy = hmm.posteriorObservationProbability(this);
		double px = hmm.marginalObservationProbability(this);
		return py * pxy / px;
	}

	@Override
	public double conditionalLabelLogProbability(HiddenMarkovModel hmm) {
		double py = hmm.priorLabelProbability(this);
		double pxy = hmm.posteriorObservationProbability(this);
		double px = hmm.marginalObservationProbability(this);
		return py + pxy - px;
	}

	@Override
	public double conditionalLabelProbability(MaximumEntropyMarkovModel memm) {
		double result = 1.0;
		for (int i = 0; i <= this.length(); i++) {
			double p = memm.conditionalProbability(this, i);
			result *= p;
		}
		return result;
	}

	@Override
	public double conditionalLabelLogProbability(MaximumEntropyMarkovModel memm) {
		double result = 0.0;
		for (int i = 0; i <= this.length(); i++) {
			double p = memm.conditionalLogProbability(this, i);
			result += p;
		}
		return result;
	}

	@Override
	public double conditionalLabelProbability(ConditionalRandomField crf) {
		return crf.conditionalProbability(this);
	}

	@Override
	public double conditionalLabelLogProbability(ConditionalRandomField crf) {
		return crf.conditionalLogProbability(this);
	}
}