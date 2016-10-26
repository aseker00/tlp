package aseker00.tlp.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
import aseker00.tlp.model.disc.ConditionalRandomField;
import aseker00.tlp.model.gen.HiddenMarkovModel;

public class ForwardBackward {
	public class Chart {
		ArrayList<HashMap<Ngram, Double>> values;

		Chart(int size) {
			this.values = new ArrayList<HashMap<Ngram, Double>>();
			for (int i = 0; i < size; i++)
				this.values.add(new HashMap<Ngram, Double>());
		}

		public void valueIs(int index, Ngram transition, double value) {
			this.values.get(index).put(transition, value);
		}

		public void valueInc(int index, Ngram transition, double value) {
			double cur = this.value(index, transition);
			this.valueIs(index, transition, value + cur);
		}

		public Double value(int index, Ngram transition) {
			return this.values.get(index).getOrDefault(transition, 0.0);
		}
	}

	private LabeledSequence sequence;
	private Chart labelsChart;
	private Chart transitionsChart;
	private double z;

	public ForwardBackward(LabeledSequence sequence) {
		this.sequence = sequence;
		this.labelsChart = new Chart(sequence.length());
		this.transitionsChart = new Chart(sequence.length() - 1);
	}

	public double labelValue(int t, Ngram label) {
		return this.labelsChart.value(t, label);
	}

	public double transitionValue(int t, Ngram transition) {
		return this.transitionsChart.value(t, transition);
	}

	public double value() {
		return this.z;
	}

	public void modelIs(HiddenMarkovModel hmm) {
		Chart alpha = initializeForward(hmm);
		for (int i = 1; i < this.sequence.length(); i++)
			forwardRecursion(hmm, i, alpha);
		Chart beta = initializeBackward(hmm);
		for (int i = this.sequence.length() - 2; i >= 0; i--)
			backwardRecursion(hmm, i, beta);
		for (int i = 0; i < this.sequence.length(); i++)
			setLabelCounts(hmm, i, alpha, beta);
		for (int i = 0; i < this.sequence.length() - 1; i++)
			setLabelTransitionCounts(hmm, i, alpha, beta);
		this.z = getForwardBackwardValue(hmm, alpha, beta, this.sequence.length() - 1);
	}

	public void modelIs(ConditionalRandomField crf) {
		List<Element> labels = new ArrayList<Element>(this.sequence.labelList());
		Chart alpha = initializeForward(crf);
		for (int i = 1; i < this.sequence.length(); i++)
			forwardRecursion(crf, i, alpha);
		Chart beta = initializeBackward(crf);
		for (int i = this.sequence.length() - 2; i >= 0; i--)
			backwardRecursion(crf, i, beta);
		for (int i = 0; i < this.sequence.length(); i++)
			setLabelCounts(crf, i, alpha, beta);
		for (int i = 0; i < this.sequence.length() - 1; i++)
			setLabelTransitionCounts(crf, i, alpha, beta);
		this.z = getForwardBackwardValue(crf, alpha, beta, this.sequence.length() - 1);
		this.sequence.labelListIs(labels);
	}

	private double getForwardBackwardValue(HiddenMarkovModel hmm, Chart alpha, Chart beta, int t) {
		Set<Ngram> transitions = hmm.transitions(this.sequence, t);
		Set<Ngram> suffixes = new HashSet<Ngram>();
		for (Ngram transition : transitions) {
			Ngram suffix = transition.subgram(1);
			suffixes.add(suffix);
		}
		double value = 0.0;
		for (Ngram suffix : suffixes) {
			double a = alpha.value(t, suffix);
			value += a;
		}
		return value;
	}

	private double getForwardBackwardValue(ConditionalRandomField crf, Chart alpha, Chart beta, int t) {
		Set<Ngram> transitions = crf.transitions(this.sequence, t);
		Set<Ngram> suffixes = new HashSet<Ngram>();
		for (Ngram transition : transitions) {
			Ngram suffix = transition.subgram(1);
			suffixes.add(suffix);
		}
		double value = 0.0;
		for (Ngram suffix : suffixes) {
			double a = alpha.value(t, suffix);
			Double b = beta.value(t, suffix);
			value += a * b;
		}
		return value;
	}

	private void setLabelTransitionCounts(LinearChainSequenceLabelModel model, int t, Chart alpha, Chart beta) {
		Set<Ngram> transitions = model.transitions(this.sequence, t + 1);
		for (Ngram transition : transitions) {
			Ngram prefix = transition.subgram(0, transition.size() - 1);
			double a = alpha.value(t, prefix);
			this.sequence.labelTransitionIs(t + 1, transition);
			double p = model.potential(this.sequence, t + 1);
			Ngram suffix = transition.subgram(1);
			double b = beta.value(t + 1, suffix);
			this.transitionsChart.valueIs(t, transition, a * p * b);
		}
	}

	private void setLabelCounts(LinearChainSequenceLabelModel model, int t, Chart alpha, Chart beta) {
		Set<Ngram> transitions = model.transitions(this.sequence, t);
		Set<Ngram> suffixes = new HashSet<Ngram>();
		for (Ngram transition : transitions) {
			Ngram suffix = transition.subgram(1);
			suffixes.add(suffix);
		}
		for (Ngram suffix : suffixes) {
			Double a = alpha.value(t, suffix);
			Double b = beta.value(t, suffix);
			this.labelsChart.valueIs(t, suffix, a * b);
		}
	}

	private void backwardRecursion(LinearChainSequenceLabelModel model, int t, Chart beta) {
		Set<Ngram> transitions = model.transitions(this.sequence, t + 1);
		for (Ngram transition : transitions) {
			this.sequence.labelTransitionIs(t + 1, transition);
			double p = model.potential(this.sequence, t + 1);
			Ngram suffix = transition.subgram(1);
			double b = beta.value(t + 1, suffix);
			Ngram prefix = transition.subgram(0, transition.size() - 1);
			beta.valueInc(t, prefix, p * b);
		}
	}

	private Chart initializeBackward(HiddenMarkovModel hmm) {
		Chart beta = new Chart(this.sequence.length());
		Set<Ngram> transitions = hmm.transitions(this.sequence, this.sequence.length());
		for (Ngram transition : transitions) {
			Ngram prefix = transition.subgram(0, transition.size() - 1);
			beta.valueIs(this.sequence.length() - 1, prefix, 1.0);
		}
		return beta;
	}

	private Chart initializeBackward(ConditionalRandomField crf) {
		Chart beta = new Chart(this.sequence.length());
		Set<Ngram> transitions = crf.transitions(this.sequence, this.sequence.length());
		for (Ngram transition : transitions) {
			this.sequence.labelTransitionIs(this.sequence.length(), transition);
			double p = crf.potential(this.sequence, this.sequence.length());
			Ngram prefix = transition.subgram(0, transition.size() - 1);
			beta.valueIs(this.sequence.length() - 1, prefix, p);
		}
		return beta;
	}

	private void forwardRecursion(LinearChainSequenceLabelModel model, int t, Chart alpha) {
		Set<Ngram> transitions = model.transitions(this.sequence, t);
		for (Ngram transition : transitions) {
			this.sequence.labelTransitionIs(t, transition);
			double p = model.potential(this.sequence, t);
			Ngram prefix = transition.subgram(0, transition.size() - 1);
			double a = alpha.value(t - 1, prefix);
			Ngram suffix = transition.subgram(1);
			while (suffix.size() > 0) {
				alpha.valueInc(t, suffix, a * p);
				suffix = suffix.subgram(1);
			}
		}
	}

	private Chart initializeForward(LinearChainSequenceLabelModel model) {
		Chart alpha = new Chart(this.sequence.length());
		Set<Ngram> transitions = model.transitions(this.sequence, 0);
		for (Ngram transition : transitions) {
			this.sequence.labelTransitionIs(0, transition);
			double p = model.potential(this.sequence, 0);
			Ngram suffix = transition.subgram(1);
			alpha.valueInc(0, suffix, p);
		}
		return alpha;
	}
}