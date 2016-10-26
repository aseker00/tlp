package aseker00.tlp.model.gen;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import aseker00.tlp.ling.Element;

public class ConditionalFrequencyDistributionImpl2 implements ConditionalFrequencyDistribution {
	HashMap<Element, ConditionalFrequencyDistribution> distributions;
	
	public ConditionalFrequencyDistributionImpl2(HashMap<Element, ConditionalFrequencyDistribution> distributions) {
		this.distributions = distributions;
	}
	
	public ConditionalFrequencyDistribution distribution(Element condition) {
		return this.distributions.get(condition);
	}
	
	@Override
	public double probability(Element condition, Element event) {
		return this.distributions.get(condition).probability(condition, event);
	}
	
	@Override
	public double logProbability(Element condition, Element event) {
		return this.distributions.get(condition).logProbability(condition, event);
	}
	
	@Override
	public Set<Element> conditions() {
		return this.distributions.keySet();
	}
	
	@Override
	public Set<Element> events(Element condition) {
		return this.distributions.get(condition).events(condition);
	}
	
	@Override
	public List<Element> top(Element condition, int len) {
		return this.distributions.get(condition).top(condition, len);
	}
	@Override
	public double count(Element condition, Element event) {
		return this.distributions.get(condition).count(condition, event);
	}
	
	@Override
	public double conditionCount(Element condition) {
		return this.distributions.get(condition).conditionCount(condition);
	}
}