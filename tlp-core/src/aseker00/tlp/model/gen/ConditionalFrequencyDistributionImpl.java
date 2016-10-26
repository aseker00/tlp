package aseker00.tlp.model.gen;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import aseker00.tlp.ling.Element;

public class ConditionalFrequencyDistributionImpl implements ConditionalFrequencyDistribution {
	HashMap<Element, FrequencyDistribution> distributions;
	
	public ConditionalFrequencyDistributionImpl(HashMap<Element, FrequencyDistribution> distributions) {
		this.distributions = distributions;
	}
	
	public FrequencyDistribution distribution(Element condition) {
		return this.distributions.get(condition);
	}
	
	@Override
	public double probability(Element condition, Element event) {
		return this.distributions.get(condition).probability(event);
	}
	
	@Override
	public double logProbability(Element condition, Element event) {
		return this.distributions.get(condition).logProbability(event);
	}
	
	@Override
	public Set<Element> conditions() {
		return this.distributions.keySet();
	}
	
	@Override
	public Set<Element> events(Element condition) {
		return this.distributions.get(condition).events();
	}
	
	@Override
	public List<Element> top(Element condition, int len) {
		return this.distributions.get(condition).top(len);
	}
	@Override
	public double count(Element condition, Element event) {
		return this.distributions.get(condition).count(event);
	}
	
	@Override
	public double conditionCount(Element condition) {
		return this.distributions.get(condition).trials();
	}
}