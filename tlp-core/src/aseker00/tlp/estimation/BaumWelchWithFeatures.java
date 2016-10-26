package aseker00.tlp.estimation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import aseker00.tlp.io.Corpus;
import aseker00.tlp.io.CorpusReader;
import aseker00.tlp.io.Tweet;
import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
import aseker00.tlp.model.LabeledSequence;
import aseker00.tlp.model.disc.FeatureVector;
import aseker00.tlp.model.disc.MaximumEntropyMarkovModel;
import aseker00.tlp.model.gen.HiddenMarkovModel;
import aseker00.tlp.pos.Tagger;

public class BaumWelchWithFeatures extends BaumWelch {

	public BaumWelchWithFeatures(String name, Tagger tagger) {
		super(name, tagger);
	}

	@Override
	public synchronized void dataSetIs(Corpus data) {
		HashSet<Element> eDecisions = new HashSet<Element>();
		CorpusReader stream = data.readerNew(data.name() + "::" + this.name());
		Tweet tweet;
		while ((tweet = stream.next()) != null) {
			ArrayList<Element> entries = new ArrayList<Element>();
			for (int i = 0; i < tweet.tokens(); i++)
				entries.add(tweet.token(i));
			LabeledSequence sequence = this.model().sequenceNew(entries);
			this.model().logLikelihoodPreStep(sequence, this, eDecisions);
		}
		MaximumEntropyMarkovModel eMemm = (MaximumEntropyMarkovModel)this.model().emissionProbabilityDistribution();
		MaximumEntropyMarkovModel tMemm = (MaximumEntropyMarkovModel)this.model().transitionProbabilityDistribution();
		for (Element c: eMemm.conditions()) {
			for (Element d: eDecisions) {
				Ngram emission = eMemm.emission((Ngram)c, d);
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
	
	public void logLikelihoodPreStep(LabeledSequence sequence, HiddenMarkovModel hmm, Set<Element> tokens) {
		for (int i = 0; i < sequence.length(); i++)
			tokens.add(sequence.entry(i));
	}
}