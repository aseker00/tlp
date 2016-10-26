package aseker00.tlp.model.disc;

import aseker00.tlp.ling.Ngram;
import aseker00.tlp.model.LabeledSequence;
import aseker00.tlp.model.LinearChainSequenceLabelModel;

public class TransitionFeatureTemplate implements FeatureTemplate {
LinearChainSequenceLabelModel model;
	
	public TransitionFeatureTemplate(LinearChainSequenceLabelModel model) {
		this.model = model;
	}
	
	@Override
	public String name() {
		return "TRAN";
	}
	
	@Override
	public Feature feature(FeatureFunction ff, LabeledSequence sequence, int pos) {
		Ngram transition = this.model.transition(sequence, pos);
		int index = ff.index(this, transition.toString());
		return new Feature(ff, index);
	}
	
	public Ngram transition(String featureName) {
		return this.model.transition(featureName);
	}
	
	@Override
	public Feature feature(FeatureFunction ff, String featureName) {
		int index = ff.index(this, featureName);
		Feature feature = ff.featureNew(index);
		return feature;
	}
}