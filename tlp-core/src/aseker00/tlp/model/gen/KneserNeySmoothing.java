package aseker00.tlp.model.gen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
import aseker00.tlp.model.Counts;

public class KneserNeySmoothing implements ConditionalFrequencyDistribution {
	private DiscountSmoothing smoothing;
	
	public KneserNeySmoothing(ConditionalFrequencyDistribution higherOrderDist) {
		ConditionalFrequencyDistribution lowerOrderDist = this.getLowerOrderDist(higherOrderDist);
		this.smoothing = new DiscountSmoothing(higherOrderDist, lowerOrderDist);
	}
	
	public ConditionalFrequencyDistribution higherOrderDist() {
		return this.smoothing.higherOrderDist();
	}
	
	public ConditionalFrequencyDistribution lowerOrderDist() {
		return this.smoothing.lowerOrderDist();
	}
	
	public void discountIs(Map<Double, Double> discount) {
		this.smoothing.discountIs(discount);
	}
	
	@Override
	public double probability(Element condition, Element event) {
		return this.smoothing.probability(condition, event);
	}

	@Override
	public double logProbability(Element condition, Element event) {
		return this.smoothing.logProbability(condition, event);
	}

	@Override
	public Set<Element> conditions() {
		return this.smoothing.conditions();
	}

	@Override
	public Set<Element> events(Element condition) {
		return this.smoothing.events(condition);
	}

	@Override
	public List<Element> top(Element condition, int len) {
		return this.smoothing.top(condition, len);
	}

	@Override
	public double count(Element condition, Element event) {
		return this.smoothing.count(condition, event);
	}

	@Override
	public double conditionCount(Element condition) {
		return this.smoothing.conditionCount(condition);
	}

	private ConditionalFrequencyDistribution getLowerOrderDist(ConditionalFrequencyDistribution higherOrderDist) {
		HashMap<Element, Counts> conditionalCounts = new HashMap<Element, Counts>();
		HashSet<Element> events = new HashSet<Element>();
		for (Element condition: higherOrderDist.conditions()) {
			Ngram ngram = (Ngram)condition;
			Ngram lowerOrderCondition = ngram.subgram(0, ngram.size()-1);
			conditionalCounts.putIfAbsent(lowerOrderCondition, new Counts());
			for (Element event: higherOrderDist.events(condition)) {
				double count = higherOrderDist.count(condition, event);
				if (count == 0.0)
					continue;
				conditionalCounts.get(lowerOrderCondition).valueInc(event, 1.0);
				events.add(event);
			}
		}
		HashMap<Element, FrequencyDistribution> conditionalDistributions = new HashMap<Element, FrequencyDistribution>();
		for (Element condition: conditionalCounts.keySet()) {
			Counts counts = conditionalCounts.get(condition);
			FrequencyDistribution dist = new MaximumLikelihoodFrequencyDistribution(events, counts);
			conditionalDistributions.put(condition, dist);
		}
		ConditionalFrequencyDistributionImpl freqDist = new ConditionalFrequencyDistributionImpl(conditionalDistributions);
		if (freqDist.conditions().size() < 2) {
			HashMap<Element, FrequencyDistribution> distributions = new HashMap<Element, FrequencyDistribution>();
			for (Element condition: freqDist.conditions()) {
				FrequencyDistribution dist = freqDist.distribution(condition);
				AdditiveSmoothing smoothedDist = new AdditiveSmoothing(dist);
				distributions.put(condition, smoothedDist);
			}
			ConditionalFrequencyDistributionImpl lowerOrderDist = new ConditionalFrequencyDistributionImpl(distributions); 
			return lowerOrderDist;
		}
		KneserNeySmoothing lowerOrderDist = new KneserNeySmoothing(freqDist); 
		return lowerOrderDist;
	}
}