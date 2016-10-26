package aseker00.tlp.model.gen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
import aseker00.tlp.model.Counts;

public class DiscountSmoothing implements ConditionalFrequencyDistribution {
	ConditionalFrequencyDistribution higherOrderDist;
	ConditionalFrequencyDistribution lowerOrderDist;
	HashMap<Element, Double> lambdas;
	TreeMap<Double, Double> discounts;
	
	public DiscountSmoothing(ConditionalFrequencyDistribution higherOrderDist, ConditionalFrequencyDistribution lowerOrderDist) {
		this.higherOrderDist = higherOrderDist;
		this.lowerOrderDist = lowerOrderDist;
		this.lambdas = new HashMap<Element, Double>();
		this.discounts = new TreeMap<Double, Double>();
	}
	
	public ConditionalFrequencyDistribution higherOrderDist() {
		return this.higherOrderDist;
	}
	
	public ConditionalFrequencyDistribution lowerOrderDist() {
		return this.lowerOrderDist;
	}
	
	public void discountIs(Map<Double, Double> discount) {
		this.discounts = new TreeMap<Double, Double>(discount);
		this.lambdas = this.initLambdas();
	}

	@Override
	public double probability(Element condition, Element event) {
		double count = this.higherOrderDist.count(condition, event);
		double D = this.getDiscount(count);
		double cHigh = Math.max(count-D, 0);
		double cCondition = this.higherOrderDist.conditionCount(condition);
		double pHigh = cHigh == 0.0 ? 0.0 : cHigh/cCondition;
		double l = this.lambdas.get(condition);
		double pLow = this.lowerOrderDist.probability(((Ngram)condition).subgram(1), event);
		double p = pHigh + l*pLow;
		return p;
	}

	@Override
	public double logProbability(Element condition, Element event) {
		return Math.log(this.probability(condition, event));
	}

	@Override
	public Set<Element> conditions() {
		return this.higherOrderDist.conditions();
	}

	@Override
	public Set<Element> events(Element condition) {
		return this.higherOrderDist.events(condition);
	}

	@Override
	public List<Element> top(Element condition, int len) {
		return this.higherOrderDist.top(condition, len);
	}

	@Override
	public double count(Element condition, Element event) {
		return this.higherOrderDist.count(condition, event);
	}

	@Override
	public double conditionCount(Element condition) {
		return this.higherOrderDist.conditionCount(condition);
	}
	
	private double getDiscount(double count) {
		if (count == 0.0)
			return 0.0;
		Double d = this.discounts.get(count);
		if (d != null)
			return d;
		return this.discounts.get(this.discounts.lastKey());
	}
	
	private HashMap<Element, Double> initLambdas() {
		HashMap<Element, Double> lambdas = new HashMap<Element, Double>();
		double maxFreq = this.discounts.lastKey();
		HashMap<Double, Counts> frequencies = new HashMap<Double, Counts>();
		for (double d: this.discounts.keySet())
			frequencies.put(d, new Counts());
		for (Element condition: this.higherOrderDist.conditions()) {
			for (Element event: this.higherOrderDist.events(condition)) {
				double count = this.higherOrderDist.count(condition, event);
				if (count == 0.0)
					continue;
				frequencies.get(Math.min(count, maxFreq)).valueInc(condition, 1.0);
			}
		}
		for (Element condition: this.higherOrderDist.conditions()) {
			double l = 0.0;
			for (double f: frequencies.keySet()) {
				double N = frequencies.get(f).value(condition);
				double D = this.discounts.get(f);
				l += N*D;
			} 
			double N1plusConditionCount = this.higherOrderDist.conditionCount(condition);
			if (l != 0.0)
				l /= N1plusConditionCount;
			lambdas.put(condition, l);
		}
		return lambdas;
	}
}