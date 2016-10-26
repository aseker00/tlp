package aseker00.tlp.model;

import java.util.List;
import java.util.Set;

import aseker00.tlp.ling.Element;

public interface ProbabilityDistribution {
	public double probability(Element event);
	public double logProbability(Element event);
	public Set<Element> events();
	public List<Element> top(int len);
}
