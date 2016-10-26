package aseker00.tlp.estimation;

import aseker00.tlp.io.Corpus;
import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
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