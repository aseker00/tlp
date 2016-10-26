package aseker00.tlp.model.disc;

import aseker00.tlp.model.LabeledSequence;

public interface FeatureTemplate {
	public String name();
	public Feature feature(FeatureFunction ff, String featureName);
	public Feature feature(FeatureFunction ff, LabeledSequence sequence, int pos);
}