package aseker00.tlp.model.gen;

import java.util.List;
import java.util.Set;

import aseker00.tlp.ling.Element;

public class AdditiveSmoothing implements FrequencyDistribution {
	FrequencyDistribution dist;
	double lambda;
	
	public AdditiveSmoothing(FrequencyDistribution dist) {
		this(dist, 1.0);
	}
	
	public AdditiveSmoothing(FrequencyDistribution dist, double lambda) {
		this.dist = dist;
		this.lambda = lambda;
	}
	
	public void lambdaIs(double value) {
		this.lambda = value;
	}
	
	@Override
	public double probability(Element event) {
		double count = this.count(event);
		double total = this.trials();
		double p = count/total;
		return p;
	}
	
	@Override
	public double logProbability(Element event) {
		return Math.log(this.probability(event));
	}
	
	@Override
	public Set<Element> events() {
		return this.dist.events();
	}
	
	@Override
	public List<Element> top(int len) {
		return this.dist.top(len);
	}
	
	@Override
	public double count(Element event) {
		double value = this.dist.count(event);
		value += this.lambda;
		return value;
	}
	
	@Override
	public double trials() {
		double value = this.dist.trials();
		value += this.lambda*this.events().size();
		return value;
	}
}