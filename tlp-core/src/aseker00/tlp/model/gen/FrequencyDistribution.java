package aseker00.tlp.model.gen;

import aseker00.tlp.ling.Element;
import aseker00.tlp.model.ProbabilityDistribution;

public interface FrequencyDistribution extends ProbabilityDistribution {
	public double count(Element event);
	public double trials();
}