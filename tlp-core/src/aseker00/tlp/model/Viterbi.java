package aseker00.tlp.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;

/*
 * http://www.cs.columbia.edu/~mcollins/courses/nlp2011/notes/hmms.pdf
 */
public class Viterbi {
	class Chart {
		ArrayList<HashMap<Ngram, Double>> p;
		ArrayList<HashMap<Ngram, Ngram>> bp;

		Chart(int size) {
			this.p = new ArrayList<HashMap<Ngram, Double>>();
			this.bp = new ArrayList<HashMap<Ngram, Ngram>>();
			for (int i = 0; i <= size; i++) {
				this.p.add(new HashMap<Ngram, Double>());
				this.bp.add(new HashMap<Ngram, Ngram>());
			}
		}

		void valueIs(int len, Ngram ngram, double value) {
			this.p.get(len).put(ngram, value);
		}

		void valueInc(int len, Ngram ngram, double value) {
			double cur = this.p.get(len).getOrDefault(ngram, 0.0);
			this.valueIs(len, ngram, value + cur);
		}

		void backpointerIs(int len, Ngram ngram, Ngram bp) {
			this.bp.get(len).put(ngram, bp);
		}

		double value(int len, Ngram ngram) {
			return this.p.get(len).get(ngram);
		}

		Ngram backpointer(int len, Ngram ngram) {
			return this.bp.get(len).get(ngram);
		}
	}

	private Chart chart;
	private LabeledSequence sequence;

	public Viterbi(LabeledSequence sequence) {
		this.sequence = sequence;
		this.chart = new Chart(sequence.length());
	}

	public double chartValue(int len, Ngram ngram) {
		return this.chart.value(len, ngram);
	}

	public void modelIs(LinearChainSequenceLabelModel model) {
		Ngram prefix = model.transition(this.sequence, 0).subgram(0, model.chain() - 1);
		this.chart.valueIs(0, prefix, 1.0);
		for (int k = 0; k < this.sequence.length(); k++) {
			Set<Ngram> transitions = model.transitions(this.sequence, k);
			Map<Ngram, Double> maxValues = new HashMap<Ngram, Double>();
			Map<Ngram, Ngram> maxLabels = new HashMap<Ngram, Ngram>();
			for (Ngram transition : transitions) {
				Ngram suffix = transition.subgram(1);
				prefix = transition.subgram(0, model.chain() - 1);
				this.sequence.labelTransitionIs(k, transition);
				double value = model.decodedPotential(this, this.sequence, k);
				double maxValue = maxValues.getOrDefault(suffix, Double.NEGATIVE_INFINITY);
				if (value > maxValue) {
					maxValues.put(suffix, value);
					maxLabels.put(suffix, prefix);
				}
			}
			for (Ngram suffix : maxValues.keySet()) {
				double maxValue = maxValues.get(suffix);
				Ngram maxLabel = maxLabels.get(suffix);
				this.chart.valueIs(k + 1, suffix, maxValue);
				this.chart.backpointerIs(k + 1, suffix, maxLabel);
			}
		}
	}

	public ArrayList<Element> maxLabels(LinearChainSequenceLabelModel model) {
		ArrayList<Element> labels = new ArrayList<Element>();
		Set<Ngram> transitions = model.transitions(this.sequence, this.sequence.length());
		double maxValue = Double.NEGATIVE_INFINITY;
		Ngram maxTransition = null;
		for (Ngram transition : transitions) {
			this.sequence.labelTransitionIs(this.sequence.length(), transition);
			double value = model.decodedPotential(this, this.sequence, this.sequence.length());
			if (value > maxValue) {
				maxValue = value;
				maxTransition = transition.subgram(0, transition.size() - 1);
			}
		}
		for (int i = model.chain() - 2; i >= 0; i--)
			labels.add(maxTransition.entry(i));
		for (int i = this.sequence.length() - 1; i >= model.chain() - 1; i--) {
			maxTransition = this.chart.backpointer(i + 1, maxTransition);
			Element label = maxTransition.entry(0);
			labels.add(label);
		}
		Collections.reverse(labels);
		return labels;
	}
}