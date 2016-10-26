package aseker00.tlp.model.disc;

import java.util.Collection;
import java.util.HashMap;
import java.util.TreeSet;

public class FeatureVector {
	FeatureFunction function;
	HashMap<Integer, Feature> entries;
	int dimension;
	
	FeatureVector(FeatureFunction function) {
		this.function = function;
		this.entries = new HashMap<Integer, Feature>();
		this.dimension = 0;
	}
	
	FeatureVector(FeatureVector other) {
		this(other.function);
		for (int i: other.entries.keySet()) {
			Feature feature = other.entries.get(i);
			Feature newFeature = this.function.featureNew(i);
			newFeature.valueIs(feature.value());
			this.entryIs(i, newFeature);
		}
		this.dimension = other.dimension;
	}
	
	public Collection<Feature> features() {
		return this.entries.values();
	}
	
	public int dimension() {
		return this.dimension;
	}
	
	public void entryIs(int index, Feature entry) {
		if (entry == null) {
			this.entries.remove(index);
			if (index == dimension) {
				TreeSet<Integer> sortedIndices = new TreeSet<Integer>(this.entries.keySet());
				dimension = sortedIndices.last();
			}
			return;
		}
		this.entries.put(index, entry);
		if (index > dimension)
			dimension = index;
	}
	
	public void entryValueIs(int index, double value) {
		Feature feature = this.entries.get(index);
		if (feature == null)
			return;
		feature.valueIs(value);
	}
	
	public double entryValue(int index) {
		Feature feature = this.entries.get(index);
		if (feature == null)
			return 0.0;
		return feature.value();
	}
	
	public double dotProduct(FeatureVector other) {
		double result = 0;
		if (this.entries.size() < other.entries.size()) {
			for (int i: this.entries.keySet())
				result += this.entryValue(i) * other.entryValue(i);
		}
		else {
			for (int i: other.entries.keySet())
				result += other.entryValue(i) * this.entryValue(i);
		}
		return result;
	}
		
	public FeatureVector additionInc(FeatureVector other) {
		for (int i: other.entries.keySet()) {
			Feature feature = other.entries.get(i);
			Feature newFeature = this.function.featureNew(i);
			double existingValue = this.entryValue(i);
			newFeature.valueIs(existingValue+feature.value());
			this.entryIs(i, newFeature);
		}
		return this;
	}
	public FeatureVector addition(FeatureVector other) {
		FeatureVector result = new FeatureVector(this);
		result.additionInc(other);
		return result;
	}

	public FeatureVector subtractionDec(FeatureVector other) {
		for (int i: other.entries.keySet()) {
			Feature feature = other.entries.get(i);
			Feature newFeature = this.function.featureNew(i);
			double existingValue = this.entryValue(i);
			newFeature.valueIs(existingValue-feature.value());
			this.entryIs(i, newFeature);
		}
		return this;
	}
	
	public FeatureVector subtraction(FeatureVector other) {
		FeatureVector result = new FeatureVector(this);
		result.subtractionDec(other);
		return result;
	}
	
	public FeatureVector intersection(FeatureVector other) {
		FeatureVector result = new FeatureVector(this.function);
		for (int i: other.entries.keySet()) {
			if (this.entries.containsKey(i)) {
				Feature feature = other.entries.get(i);
				result.entryIs(feature.index(), feature);
			}
		}
		return result;
	}

	public FeatureVector product(double d) {
		FeatureVector result = this.function.featureVectorNew();
		if (d != 0) {
			for (int i: this.entries.keySet()) {
				Feature feature = this.function.featureNew(i);
				feature.valueIs(this.entryValue(i)*d);
				result.entryIs(i, feature);
			}
		}
		return result;
	}
	
	public double length() {
		double d = this.dotProduct(this);
		return Math.sqrt(d);
	}
	
	public double norm() {
		double result = 0;
		for (int i: this.entries.keySet())
			result += this.entryValue(i);
		return result;
	}
	
	public double norm2() {
		return this.dotProduct(this);
	}
	
	public int hashCode() {
		return this.entries.hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof FeatureVector))
			return false;
		FeatureVector that = (FeatureVector)other;
		return this.entries.equals(that.entries);
	}
	
	@Override
	public String toString() {
		return this.entries.toString();
	}
}