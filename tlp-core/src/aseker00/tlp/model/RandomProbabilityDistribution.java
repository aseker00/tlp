package aseker00.tlp.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import aseker00.tlp.ling.Element;

public class RandomProbabilityDistribution implements ProbabilityDistribution {
	private HashMap<Element, Double> probabilities;

	public RandomProbabilityDistribution(List<Element> samples) {
		this.probabilities = new HashMap<Element, Double>();
		this.init(samples);
	}

	@Override
	public double probability(Element element) {
		return this.probabilities.getOrDefault(element, 0.0);
	}

	@Override
	public double logProbability(Element element) {
		return Math.log(this.probability(element));
	}

	@Override
	public Set<Element> events() {
		return this.probabilities.keySet();
	}

	@Override
	public List<Element> top(int len) {
		Comparator<Map.Entry<Element, Double>> comp = new Comparator<Map.Entry<Element, Double>>() {
			@Override
			public int compare(Entry<Element, Double> o1, Entry<Element, Double> o2) {
				return o1.getValue().compareTo(o2.getValue());
			}
		};
		TreeSet<Map.Entry<Element, Double>> sorted = new TreeSet<Map.Entry<Element, Double>>(comp);
		sorted.addAll(this.probabilities.entrySet());
		ArrayList<Element> result = new ArrayList<Element>();
		for (Map.Entry<Element, Double> entry : sorted) {
			result.add(entry.getKey());
			if (result.size() == len)
				break;
		}
		return result;
	}

	private void init(List<Element> samples) {
		Random r = new Random();
		List<Double> rands = new ArrayList<Double>();
		double total = 0.0;
		for (int i = 0; i < samples.size(); i++) {
			double value = r.nextDouble();
			rands.add(value);
			total += value;
		}
		for (int i = 0; i < samples.size(); i++) {
			Element element = samples.get(i);
			double value = rands.get(i) / total;
			this.probabilities.put(element, value);
		}
	}
}