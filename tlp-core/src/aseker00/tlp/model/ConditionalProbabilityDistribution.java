package aseker00.tlp.model;

import java.util.List;
import java.util.Set;

import aseker00.tlp.ling.Element;

public interface ConditionalProbabilityDistribution {
	public double probability(Element condition, Element event);
	public double logProbability(Element condition, Element event);
	public Set<Element> conditions();
	public Set<Element> events(Element condition);
	public List<Element> top(Element condition, int len);
}