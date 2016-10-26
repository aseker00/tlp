package aseker00.tlp.estimation;

import java.util.ArrayList;

import aseker00.tlp.io.Corpus;
import aseker00.tlp.io.CorpusReader;
import aseker00.tlp.io.Tweet;
import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
import aseker00.tlp.model.LabeledSequence;
import aseker00.tlp.model.gen.HiddenMarkovModel;
import aseker00.tlp.pos.Tagger;

public class MaximumLikelihoodEstimation extends MaximumLikelihood {
	protected HiddenMarkovModel hmm;
	protected RelativeFrequencyLogLikelihood logLikelihood;

	public MaximumLikelihoodEstimation(String name, Tagger tagger) {
		super(name, tagger);
	}

	public void modelIs(HiddenMarkovModel hmm) {
		this.hmm = hmm;
	}

	public HiddenMarkovModel model() {
		return this.hmm;
	}

	public RelativeFrequencyLogLikelihood logLikelihoodFunction() {
		return this.logLikelihood;
	}

	@Override
	public synchronized void dataSetIs(Corpus data) {
		this.logLikelihood = this.hmm.logLikelihoodNew();
		CorpusReader stream = data.readerNew(data.name() + "::" + this.name());
		Tweet tweet;
		while ((tweet = stream.next()) != null) {
			ArrayList<Element> entries = new ArrayList<Element>();
			ArrayList<Element> labels = new ArrayList<Element>();
			for (int i = 0; i < tweet.tokens(); i++) {
				entries.add(tweet.token(i));
				labels.add(tweet.tag(i));
			}
			LabeledSequence sequence = this.model().sequenceNew(entries, labels);
			RelativeFrequencyLogLikelihood step = this.model().logLikelihoodStep(sequence, this);
			this.logLikelihood.additionInc(step);
		}
		for (Notifiee key : this.notifiees.keySet()) {
			Notifiee notifiee = this.notifiees.get(key);
			notifiee.onDataSet(data);
		}
	}

	public RelativeFrequencyLogLikelihood logLikelihoodStep(LabeledSequence sequence, HiddenMarkovModel hmm) {
		RelativeFrequencyLogLikelihood rfllf = hmm.logLikelihoodNew();
		rfllf.valueInc(this.hmm.jointLogProbability(sequence));
		for (int i = 0; i < sequence.length(); i++) {
			Ngram emission = this.hmm.emission(sequence, i);
			Ngram label = emission.subgram(0, emission.size() - 1);
			Ngram transition = this.hmm.transition(sequence, i);
			rfllf.emissionCountInc(emission);
			rfllf.labelCountInc(label);
			rfllf.transitionCountInc(transition);
		}
		Ngram transition = this.hmm.transition(sequence, sequence.length());
		rfllf.transitionCountInc(transition);
		return rfllf;
	}
}