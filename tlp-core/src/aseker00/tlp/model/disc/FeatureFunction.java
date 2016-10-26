package aseker00.tlp.model.disc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import aseker00.tlp.ling.Ngram;
import aseker00.tlp.model.LabeledSequence;

public class FeatureFunction {
	ArrayList<FeatureTemplate> templates;
	protected HashMap<String, Integer> featureNameIndex;
	protected HashMap<Integer, String> indexFeatureName;
	
	public FeatureFunction() {
		this.templates = new ArrayList<FeatureTemplate>();
		this.featureNameIndex = new HashMap<String, Integer>();
		this.indexFeatureName = new HashMap<Integer, String>();
	}
	
	public List<Feature> features() {
		List<Feature> features = new ArrayList<Feature>();
		for (Integer index: this.indexFeatureName.keySet()) {
			Feature feature = this.featureNew(index);
			features.add(feature);
		}
		return features;
	}
	
	public Feature feature(int index) {
		String featureName = this.indexFeatureName.get(index);
		if (featureName == null)
			return null;
		return this.featureNew(index);
	}
	
	public synchronized void featureIs(int index, Feature feature) {
		String featureName = this.indexFeatureName.get(index);
		if (feature == null) {
			this.indexFeatureName.remove(index);
			this.featureNameIndex.remove(featureName);
		}
		else {
			this.featureNameIndex.put(featureName, index);
			this.indexFeatureName.put(index, featureName);
		}
	}
	
	void templateIs(FeatureTemplate key, FeatureTemplate template) {
		if (template == null) {
			this.templates.remove(key);
			return;
		}
		this.templates.add(key);
	}
	
	FeatureVector featureVectorNew() {
		FeatureVector featureVector = new FeatureVector(this);
		return featureVector;
	}
	
	Feature featureNew(int index) {
		Feature feature = new Feature(this, index);
		return feature;
	}
	
	FeatureVector featureVector(Ngram transition, Ngram emission, double value) {
		FeatureVector featureVector = this.featureVectorNew();
		for (FeatureTemplate template: this.templates) {
			if (template instanceof EmissionFeatureTemplate && emission != null) {
				Feature feature = template.feature(this, emission.toString());
				feature.valueIs(value);
				featureVector.entryIs(feature.index(), feature);
			}
			else if (template instanceof TransitionFeatureTemplate && transition != null) {
				Feature feature = template.feature(this, transition.toString());
				feature.valueIs(value);
				featureVector.entryIs(feature.index(), feature);
			}
			
		}
		return featureVector;
	}
	
	FeatureVector featureVector(LabeledSequence sequence, int pos, double value) {
		FeatureVector featureVector = this.featureVectorNew();
		for (FeatureTemplate template: this.templates) {
			Feature feature = template.feature(this, sequence, pos);
			if (feature != null) {
				feature.valueIs(value);
				featureVector.entryIs(feature.index(), feature);
			}
		}
		return featureVector;
	}
	
	public synchronized Feature featureNew(String featureName, int index, double value) {
		for (FeatureTemplate template: this.templates) {
			if (featureName.startsWith(template.name() + "=")) {
				this.featureNameIndex.put(featureName, index);
				this.indexFeatureName.put(index, featureName);
				Feature feature = template.feature(this, featureName.replace(template.name() + "=", ""));
				feature.valueIs(value);
				return feature;
			}
		}
		return null;
	}
	
	public Ngram ngram(Feature feature) {
		String featureName = this.featureName(feature);
		for (FeatureTemplate template: this.templates) {
			if (featureName.startsWith(template.name() + "=")) {
				if (template instanceof EmissionFeatureTemplate)
					return ((EmissionFeatureTemplate)template).emission(featureName.replace(template.name() + "=", ""));
				else if (template instanceof TransitionFeatureTemplate)
					return ((TransitionFeatureTemplate)template).transition(featureName.replace(template.name() + "=", ""));
			}
		}
		return null;
	}
	
	public Ngram emission(String templateFeatureName) {
		for (FeatureTemplate template: this.templates) {
			if (template instanceof EmissionFeatureTemplate) {
				String str = templateFeatureName.replace(template.name() + "=", "");
				return ((EmissionFeatureTemplate)template).emission(str);
			}
		}
		return null;
	}
	
	public Feature feature(FeatureTemplate template, int index) {
		String featureName = this.indexFeatureName.get(index);
		if (featureName == null)
			return null;
		if (!featureName.startsWith(template.name() + "="))
			return null;
		return template.feature(this, featureName.replace(template.name() + "=", ""));
	}
	
	public int index(FeatureTemplate template, String featureName) {
		String templateFeatureName = template.name() + "=" + featureName;
		return this.getIndex(templateFeatureName);
	}
	
	public String name(int index) {
		return this.indexFeatureName.get(index);
	}
	
	String featureName(Feature feature) {
		int index = feature.index();
		return this.indexFeatureName.get(index); 
	}
	
	protected int getIndex(String featureName) {
		Integer index = this.featureNameIndex.get(featureName);
		if (index != null)
			return index;
		synchronized (this) {
			index = this.featureNameIndex.get(featureName);
			if (index != null)
				return index;
			index = this.featureNameIndex.size()+1;
			this.featureNameIndex.put(featureName, index);
			this.indexFeatureName.put(index, featureName);
		}
		return index;
	}
//	public void validate() {
//		for (String featureName: this.featureNameIndex.keySet()) {
//			int featureIndex = this.featureNameIndex.get(featureName);
//			String indexName = this.indexFeatureName.get(featureIndex);
//			if (!featureName.equals(indexName))
//				System.out.println("feature name mismatch: feature name = " + featureName + "; feature index = " + featureIndex + "; index name = " + indexName);
//		}
//		for (int featureIndex: this.indexFeatureName.keySet()) {
//			String indexName = this.indexFeatureName.get(featureIndex);
//			int nameIndex = this.featureNameIndex.get(indexName);
//			if (featureIndex != nameIndex)
//				System.out.println("feature index mismatch: feature index = " + featureIndex + "; index name = " + indexName + "; name index = " + nameIndex);
//		}
//	}
}