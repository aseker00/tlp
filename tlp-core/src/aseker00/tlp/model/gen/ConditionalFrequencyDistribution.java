package aseker00.tlp.model.gen;

import aseker00.tlp.ling.Element;
import aseker00.tlp.model.ConditionalProbabilityDistribution;

public interface ConditionalFrequencyDistribution extends ConditionalProbabilityDistribution {
	public double count(Element condition, Element event);
	public double conditionCount(Element condition);
}