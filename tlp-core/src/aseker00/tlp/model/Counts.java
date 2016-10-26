package aseker00.tlp.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import aseker00.tlp.ling.Element;

public class Counts {
	private HashMap<Element, Double> values;
	private double total;
	
	public Counts() {
		this.values = new HashMap<Element, Double>();
		this.total = 0.0;
	}
	
	public Counts(Counts other) {
		this.values = new HashMap<Element, Double>(other.values);
		this.total = other.total;
	}
	
	public List<Element> top(int len) {
		Comparator<Map.Entry<Element, Double>> comp = new Comparator<Map.Entry<Element, Double>>() {
			@Override
			public int compare(Entry<Element, Double> o1, Entry<Element, Double> o2) {
				return o1.getValue().compareTo(o2.getValue());
			}
		};
		TreeSet<Map.Entry<Element, Double>> sorted = new TreeSet<Map.Entry<Element, Double>>(comp);
		sorted.addAll(this.values.entrySet());
		ArrayList<Element> result = new ArrayList<Element>();
		for (Map.Entry<Element, Double> entry: sorted) {
			result.add(entry.getKey());
			if (result.size() == len)
				break;
		}
		return result;
	} 
	
	public Set<Element> elements() {
		return this.values.keySet();
	}
	
	public double total() {
		return this.total;
	}
	
	public double value(Element element) {
		return this.values.getOrDefault(element, 0.0);
	}
	
	public void valueInc(Element element, double value) {
		if (value == 0)
			return;
		double cur = this.value(element);
		this.values.put(element, cur+value);
		this.total += value;
	}
	
	public void valuesInc(Counts other) {
		for (Element event: other.values.keySet()) {
			double value = other.value(event);
			this.valueInc(event, value);
		}
	}
	
	public TreeMap<Double, Set<Element>> frequencies() {
		TreeMap<Double, Set<Element>> result = new TreeMap<Double, Set<Element>>();
		for (Element element: this.elements()) {
			double value = this.value(element);
			result.putIfAbsent(value, new HashSet<Element>());
			result.get(value).add(element);
		}
		return result;
	}
	
	@Override
	public String toString() {
		return this.values.toString();
	}
}