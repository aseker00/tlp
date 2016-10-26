package aseker00.tlp.estimation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import aseker00.tlp.io.Corpus;
import aseker00.tlp.io.CorpusReader;
import aseker00.tlp.io.Tweet;
import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
import aseker00.tlp.model.ForwardBackward;
import aseker00.tlp.model.LabeledSequence;
import aseker00.tlp.model.gen.HiddenMarkovModel;
import aseker00.tlp.pos.Tagger;

public class BaumWelch extends MaximumLikelihoodEstimation {

	public BaumWelch(String name, Tagger tagger) {
		super(name, tagger);
	}

	@Override
	public synchronized void dataSetIs(Corpus data) {
		this.logLikelihood = this.model().logLikelihoodNew();
		CorpusReader stream = data.readerNew(data.name() + "::" + this.name());
		Tweet tweet;
		while ((tweet = stream.next()) != null) {
			ArrayList<Element> entries = new ArrayList<Element>();
			for (int i = 0; i < tweet.tokens(); i++)
				entries.add(tweet.token(i));
			LabeledSequence sequence = this.model().sequenceNew(entries);
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
		ForwardBackward fb = new ForwardBackward(sequence);
		fb.modelIs(hmm);
		if (fb.value() == 0 || Double.isNaN(Math.log(fb.value())) || Double.isInfinite(Math.log(fb.value())))
			throw new RuntimeException();
		rfllf.valueInc(Math.log(fb.value()));
		for (int i = 0; i < sequence.length(); i++) {
			Set<Ngram> transitions = hmm.transitions(sequence, i);
			Set<Ngram> labels = new HashSet<Ngram>();
			for (Ngram transition : transitions) {
				Ngram suffix = transition.subgram(1);
				if (labels.add(suffix)) {
					double labelEmissionJointProb = fb.labelValue(i, suffix);
					double labelEmissionConditionalProb = labelEmissionJointProb / fb.value();
					rfllf.labelCountInc(suffix, labelEmissionConditionalProb);
					sequence.labelTransitionIs(i, transition);
					Ngram emission = hmm.emission(sequence, i);
					rfllf.emissionCountInc(emission, labelEmissionConditionalProb);
				}
				if (i == 0) {
					double labelEmissionJointProb = fb.labelValue(i, suffix);
					double labelEmissionConditionalProb = labelEmissionJointProb / fb.value();
					rfllf.transitionCountInc(transition, labelEmissionConditionalProb);
				} else {
					double transitionEmissionJointProb = fb.transitionValue(i - 1, transition);
					double transitionEmissionConditionalProb = transitionEmissionJointProb / fb.value();
					rfllf.transitionCountInc(transition, transitionEmissionConditionalProb);
				}
			}
		}
		Set<Ngram> transitions = hmm.transitions(sequence, sequence.length());
		for (Ngram transition : transitions) {
			Ngram prefix = transition.subgram(0, transition.size() - 1);
			double labelEmissionJointProb = fb.labelValue(sequence.length() - 1, prefix);
			double labelEmissionConditionalProb = labelEmissionJointProb / fb.value();
			rfllf.transitionCountInc(transition, labelEmissionConditionalProb);
		}
		return rfllf;
	}
}