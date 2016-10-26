package aseker00.tlp.model.disc;

public class Feature {
	int index;
	double value;
	FeatureFunction function;
	
	public Feature(FeatureFunction function, int index) {
		this.value = 0.0;
		this.index = index;
		this.function = function;
	}
	
	public int index() {
		return this.index;
	}
	
	public double value() {
		return this.value;
	}
	
	public void valueIs(double value) {
		this.value = value;
	}
	
	public String name() {
		return this.function.featureName(this);
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (!(other instanceof Feature))
			return false;
		Feature that = (Feature)other;
		return this.index == that.index && this.function.equals(that.function);
	}
	
	@Override
	public int hashCode() {
		int result = 1;
		result += 37 * this.function.hashCode();
		result += 37 * this.index;
		return result;
	}
	
	@Override
	public String toString() {
		return String.valueOf(this.value);
	}
}