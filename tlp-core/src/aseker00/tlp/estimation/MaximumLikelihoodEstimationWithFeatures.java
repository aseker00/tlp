package aseker00.tlp.estimation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import aseker00.tlp.io.Corpus;
import aseker00.tlp.io.CorpusReader;
import aseker00.tlp.io.Tweet;
import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
import aseker00.tlp.ling.Tag;
import aseker00.tlp.model.LabeledSequence;
import aseker00.tlp.model.LabeledSequenceImpl;
import aseker00.tlp.model.disc.FeatureVector;
import aseker00.tlp.model.disc.MaximumEntropyMarkovModel;
import aseker00.tlp.pos.Tagger;

public class MaximumLikelihoodEstimationWithFeatures extends MaximumLikelihoodEstimation {

	public MaximumLikelihoodEstimationWithFeatures(String name, Tagger tagger) {
		super(name, tagger);
	}

	@Override
	public synchronized void dataSetIs(Corpus data) {
		MaximumEntropyMarkovModel eMemm = (MaximumEntropyMarkovModel)this.model().emissionProbabilityDistribution();
		MaximumEntropyMarkovModel tMemm = (MaximumEntropyMarkovModel)this.model().transitionProbabilityDistribution();
		HashSet<Ngram> eContext = new HashSet<Ngram>();
		HashSet<Ngram> tContext = new HashSet<Ngram>();
		HashSet<Element> eDecisions = new HashSet<Element>();
		HashSet<Element> tDecisions = new HashSet<Element>();
		for (Tag tag: ((Tagger)this.processor()).tagSet())
			tDecisions.add(tag);
		tDecisions.add(((Tagger)this.processor()).stopTag());
		CorpusReader stream = data.readerNew(data.name() + "::" + this.name());
		Tweet tweet;
		while ((tweet = stream.next()) != null) {
			ArrayList<Element> entries = new ArrayList<Element>();
			for (int i = 0; i < tweet.tokens(); i++)
				entries.add(tweet.token(i));
			LabeledSequence sequence = new LabeledSequenceImpl(entries);
			for (int i = 0; i < sequence.length(); i++) {
				Set<Ngram> transitions = this.model().transitions(sequence, i);
				for (Ngram transition : transitions) {
					sequence.labelTransitionIs(i, transition);
					Ngram emission = this.model().emission(sequence, i);
					Ngram tCondition = transition.subgram(0, transition.size()-1);
					Ngram eCondition = emission.subgram(0, emission.size()-1);
					tContext.add(tCondition);
					eContext.add(eCondition);
					eDecisions.add(sequence.entry(i));
				}
			}
			Set<Ngram> transitions = this.model().transitions(sequence, sequence.length());
			for (Ngram transition : transitions) {
				sequence.labelTransitionIs(sequence.length(), transition);
				Ngram tCondition = transition.subgram(0, transition.size()-1);
				tContext.add(tCondition);
			}
		}
		for (Ngram c: tContext) {
			tMemm.eventSetIs(c, new HashSet<Element>());
			for (Element d: tDecisions) {
				Ngram transition = tMemm.transition(c, d);
				tMemm.events(c).add(transition.entry(transition.size()-1));
			}
		}
		for (Ngram c: eContext) {
			eMemm.eventSetIs(c, new HashSet<Element>());
			for (Element d: eDecisions) {
				Ngram emission = eMemm.emission(c, d);
				eMemm.events(c).add(emission.entry(emission.size()-1));
			}
		}
		for (Element c: tMemm.conditions()) {
			double norm = 0.0;
			for (Element d: tMemm.events(c)) {
				Ngram transition = tMemm.transition((Ngram)c, d);
				FeatureVector fv = tMemm.featureVector(transition, null);
				double value = Math.exp(tMemm.parameters().dotProduct(fv));
				norm += value;
			}
			tMemm.normIs(c, norm);
		}
		for (Element c: eMemm.conditions()) {
			double norm = 0.0;
			for (Element d: eMemm.events(c)) {
				Ngram emission = eMemm.emission((Ngram)c, d);
				FeatureVector fv = eMemm.featureVector(null, emission);
				double value = Math.exp(eMemm.parameters().dotProduct(fv));
				norm += value;
			}
			eMemm.normIs(c, norm);
		}
		super.dataSetIs(data);
	}
}