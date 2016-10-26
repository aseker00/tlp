package aseker00.tlp.model.gen;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import aseker00.tlp.ling.Element;
import aseker00.tlp.model.Counts;

public class MaximumLikelihoodFrequencyDistribution implements FrequencyDistribution {
	protected Counts counts;
	protected Set<Element> events;
	
	public MaximumLikelihoodFrequencyDistribution(Set<Element> events, Counts counts) {
		this.counts = new Counts(counts);
		this.events = new HashSet<Element>(events);
	}
	
	@Override
	public double probability(Element event) {
		double total = this.trials();
		if (total == 0.0)
			return 0.0;
		double count = this.count(event);
		double p = count/total;
		return p;
	}
	
	@Override
	public double logProbability(Element event) {
		return Math.log(this.probability(event));
	}
	
	@Override
	public Set<Element> events() {
		return this.events;
	}
	
	@Override
	public List<Element> top(int len) {
		return this.counts.top(len);
	}
	
	@Override
	public double count(Element event) {
		return this.counts.value(event);
	}
	
	@Override
	public double trials() {
		return this.counts.total();
	}
}