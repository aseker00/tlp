package aseker00.tlp.model.disc;

import aseker00.tlp.ling.Ngram;
import aseker00.tlp.model.LabeledSequence;
import aseker00.tlp.model.LinearChainSequenceLabelModel;

public class EmissionFeatureTemplate implements FeatureTemplate {
	LinearChainSequenceLabelModel model;
	
	public EmissionFeatureTemplate(LinearChainSequenceLabelModel model) {
		this.model = model;
	}
	
	@Override
	public String name() {
		return "EMIT";
	}
	
	@Override
	public Feature feature(FeatureFunction ff, LabeledSequence sequence, int pos) {
		if (pos == sequence.length())
			return null;
		Ngram emission = this.model.emission(sequence, pos);
		int index = ff.index(this, emission.toString());
		Feature feature = ff.featureNew(index);
		return feature;
	}
	
	public Ngram emission(String featureName) {
		return this.model.emission(featureName);
	}

	@Override
	public Feature feature(FeatureFunction ff, String featureName) {
		int index = ff.index(this, featureName);
		Feature feature = ff.featureNew(index);
		return feature;
	}
}