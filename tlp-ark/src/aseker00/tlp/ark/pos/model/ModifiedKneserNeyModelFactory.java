package aseker00.tlp.ark.pos.model;

import java.util.HashMap;
import java.util.Map;

import aseker00.tlp.ark.pos.ArkHmmTagger;
import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
import aseker00.tlp.ling.Tag;
import aseker00.tlp.model.Counts;
import aseker00.tlp.model.gen.ConditionalFrequencyDistribution;
import aseker00.tlp.model.gen.ConditionalFrequencyDistributionImpl;
import aseker00.tlp.model.gen.ConditionalFrequencyDistributionImpl2;
import aseker00.tlp.model.gen.FrequencyDistribution;
import aseker00.tlp.model.gen.KneserNeySmoothing;

public class ModifiedKneserNeyModelFactory {

	public static ConditionalFrequencyDistributionImpl2 getModifiedKneserNeySmoothingEmissionFrequencyDistrubution(ArkHmmTagger tagger, ConditionalFrequencyDistributionImpl original) {
		HashMap<Double, Counts> openClassFreqCounts = new HashMap<Double, Counts>();
		HashMap<Double, Counts> closedClassFreqCounts = new HashMap<Double, Counts>();
		HashMap<Double, Counts> specialFreqCounts = new HashMap<Double, Counts>();
		HashMap<Element, Counts> openClassCounts = new HashMap<Element, Counts>();
		HashMap<Element, Counts> closedClassCounts = new HashMap<Element, Counts>();
		HashMap<Element, Counts> specialCounts = new HashMap<Element, Counts>();
		for (double d = 1.0; d <= 5.0; d += 1.0) {
			openClassFreqCounts.putIfAbsent(d, new Counts());
			closedClassFreqCounts.putIfAbsent(d, new Counts());
			specialFreqCounts.putIfAbsent(d, new Counts());
		}
		for (Element condition: original.conditions()) {
			Ngram ngram = (Ngram)condition;
			Tag tag = (Tag)ngram.entry(ngram.size()-1);
			if (tagger.openClassTagSet().contains(tag)) {
				Counts counts = new Counts();
				for (Element event: original.events(condition)) {
					double count = original.count(condition, event);
					if (count == 0.0)
						continue;
					counts.valueInc(event, count);
					openClassFreqCounts.get(Math.min(count, 5.0)).valueInc(condition, 1.0);
				}
				openClassCounts.put(condition, counts);
			}
			else if (tagger.closedClassTagSet().contains(tag)) {
				Counts counts = new Counts();
				for (Element event: original.events(condition)) {
					double count = original.count(condition, event);
					if (count == 0.0)
						continue;
					counts.valueInc(event, count);
					closedClassFreqCounts.get(Math.min(count, 5.0)).valueInc(condition, 1.0);
				}
				closedClassCounts.put(condition, counts);
			}
			else if (tagger.specialTagSet().contains(tag)) {
				Counts counts = new Counts();
				for (Element event: original.events(condition)) {
					double count = original.count(condition, event);
					if (count == 0.0)
						continue;
					counts.valueInc(event, count);
					specialFreqCounts.get(Math.min(count, 5.0)).valueInc(condition, 1.0);
				}
				specialCounts.put(condition, counts);
			}
		}
		HashMap<Element, FrequencyDistribution> openClassDistributions = new HashMap<Element, FrequencyDistribution>();
		HashMap<Element, FrequencyDistribution> closedClassDistributions = new HashMap<Element, FrequencyDistribution>();
		HashMap<Element, FrequencyDistribution> specialDistributions = new HashMap<Element, FrequencyDistribution>();
		for (Element condition: openClassCounts.keySet())
			openClassDistributions.put(condition, original.distribution(condition));
		for (Element condition: closedClassCounts.keySet())
			closedClassDistributions.put(condition, original.distribution(condition));
		for (Element condition: specialCounts.keySet())
			specialDistributions.put(condition, original.distribution(condition));
		ConditionalFrequencyDistribution openClassConditionalDist = new ConditionalFrequencyDistributionImpl(openClassDistributions);
		ConditionalFrequencyDistribution closedClassConditionalDist = new ConditionalFrequencyDistributionImpl(closedClassDistributions);
		ConditionalFrequencyDistribution specialConditionalDist = new ConditionalFrequencyDistributionImpl(specialDistributions);
		double n1 = openClassFreqCounts.get(1.0).total();
		double n2 = openClassFreqCounts.get(2.0).total();
		double n3 = openClassFreqCounts.get(3.0).total();
		double n4 = openClassFreqCounts.get(4.0).total();
		double Y = n1/(n1+2*n2);
		double D1 = 1-2*Y*n2/n1;
		double D2 = 2-3*Y*n3/(n2);
		double D3plus = 3-4*Y*n4/(n3);
		D1 = 0.0001;
		D2 = 0.0001;
		D3plus = 0.001;
		Map<Double, Double> discount = new HashMap<Double, Double>();
		discount.put(1.0, D1);
		discount.put(2.0, D2);
		discount.put(3.0, D3plus);
		KneserNeySmoothing openClassKneserNey = new KneserNeySmoothing(openClassConditionalDist);
		openClassKneserNey.discountIs(discount);
		if (openClassKneserNey.lowerOrderDist() instanceof KneserNeySmoothing) {
			openClassFreqCounts = new HashMap<Double, Counts>();
			for (double d = 1.0; d <= 5.0; d += 1.0)
				openClassFreqCounts.putIfAbsent(d, new Counts());
			for (Element condition: openClassKneserNey.lowerOrderDist().conditions()) {
				Counts counts = new Counts();
				for (Element event: openClassKneserNey.lowerOrderDist().events(condition)) {
					double count = openClassKneserNey.lowerOrderDist().count(condition, event);
					if (count == 0.0)
						continue;
					counts.valueInc(event, count);
					openClassFreqCounts.get(Math.min(count, 5.0)).valueInc(condition, 1.0);
				}
			}
			n1 = openClassFreqCounts.get(1.0).total();
			n2 = openClassFreqCounts.get(2.0).total();
			n3 = openClassFreqCounts.get(3.0).total();
			n4 = openClassFreqCounts.get(4.0).total();
			Y = n1/(n1+2*n2);
			D1 = 1-2*Y*n2/n1;
			D2 = 2-3*Y*n3/(n2);
			D3plus = 3-4*Y*n4/(n3);
			//D1 = 0.1;
			//D2 = 0.1;
			//D3plus = 0.1;
			discount = new HashMap<Double, Double>();
			discount.put(1.0, D1);
			discount.put(2.0, D2);
			discount.put(3.0, D3plus);
			((KneserNeySmoothing)openClassKneserNey.lowerOrderDist()).discountIs(discount);
		}
		n1 = closedClassFreqCounts.get(1.0).total();
		n2 = closedClassFreqCounts.get(2.0).total();
		n3 = closedClassFreqCounts.get(3.0).total();
		n4 = closedClassFreqCounts.get(4.0).total();
		Y = n1/(n1+2*n2);
		D1 = 1-2*Y*n2/n1;
		D2 = 2-3*Y*n3/(n2);
		D3plus = 3-4*Y*n4/(n3);
		D1 = 0.0001;
		D2 = 0.0001;
		D3plus = 0.00001;
		discount = new HashMap<Double, Double>();
		discount.put(1.0, D1);
		discount.put(2.0, D2);
		discount.put(3.0, D3plus);
		KneserNeySmoothing closedClassKneserNey = new KneserNeySmoothing(closedClassConditionalDist);
		closedClassKneserNey.discountIs(discount);
		if (closedClassKneserNey.lowerOrderDist() instanceof KneserNeySmoothing) {
			closedClassFreqCounts = new HashMap<Double, Counts>();
			for (double d = 1.0; d <= 5.0; d += 1.0)
				closedClassFreqCounts.putIfAbsent(d, new Counts());
			for (Element condition: closedClassKneserNey.lowerOrderDist().conditions()) {
				Counts counts = new Counts();
				for (Element event: closedClassKneserNey.lowerOrderDist().events(condition)) {
					double count = closedClassKneserNey.lowerOrderDist().count(condition, event);
					if (count == 0.0)
						continue;
					counts.valueInc(event, count);
					closedClassFreqCounts.get(Math.min(count, 5.0)).valueInc(condition, 1.0);
				}
			}
			n1 = closedClassFreqCounts.get(1.0).total();
			n2 = closedClassFreqCounts.get(2.0).total();
			n3 = closedClassFreqCounts.get(3.0).total();
			n4 = closedClassFreqCounts.get(4.0).total();
			Y = n1/(n1+2*n2);
			D1 = 1-2*Y*n2/n1;
			D2 = 2-3*Y*n3/(n2);
			D3plus = 3-4*Y*n4/(n3);
			//D1 = 0.1;
			//D2 = 0.1;
			//D3plus = 0.1;
			discount = new HashMap<Double, Double>();
			discount.put(1.0, D1);
			discount.put(2.0, D2);
			discount.put(3.0, D3plus);
			((KneserNeySmoothing)closedClassKneserNey.lowerOrderDist()).discountIs(discount);
		}
		n1 = specialFreqCounts.get(1.0).total();
		n2 = specialFreqCounts.get(2.0).total();
		n3 = specialFreqCounts.get(3.0).total();
		n4 = specialFreqCounts.get(4.0).total();
		Y = n1/(n1+2*n2);
		D1 = 1-2*Y*n2/n1;
		D2 = 2-3*Y*n3/(n2);
		D3plus = 3-4*Y*n4/(n3);
		D1 = 0.0;
		D2 = 0.0;
		D3plus = 0.0;
		discount = new HashMap<Double, Double>();
		discount.put(1.0, D1);
		discount.put(2.0, D2);
		discount.put(3.0, D3plus);
		KneserNeySmoothing specialKneserNey = new KneserNeySmoothing(specialConditionalDist);
		specialKneserNey.discountIs(discount);
		if (specialKneserNey.lowerOrderDist() instanceof KneserNeySmoothing) {
			specialFreqCounts = new HashMap<Double, Counts>();
			for (double d = 1.0; d <= 5.0; d += 1.0)
				specialFreqCounts.putIfAbsent(d, new Counts());
			for (Element condition: specialKneserNey.lowerOrderDist().conditions()) {
				Counts counts = new Counts();
				for (Element event: specialKneserNey.lowerOrderDist().events(condition)) {
					double count = specialKneserNey.lowerOrderDist().count(condition, event);
					if (count == 0.0)
						continue;
					counts.valueInc(event, count);
					specialFreqCounts.get(Math.min(count, 5.0)).valueInc(condition, 1.0);
				}
			}
			n1 = specialFreqCounts.get(1.0).total();
			n2 = specialFreqCounts.get(2.0).total();
			n3 = specialFreqCounts.get(3.0).total();
			n4 = specialFreqCounts.get(4.0).total();
			Y = n1/(n1+2*n2);
			D1 = 1-2*Y*n2/n1;
			D2 = 2-3*Y*n3/(n2);
			D3plus = 3-4*Y*n4/(n3);
			D1 = 0.0;
			D2 = 0.0;
			D3plus = 0.0;
			discount = new HashMap<Double, Double>();
			discount.put(1.0, D1);
			discount.put(2.0, D2);
			discount.put(3.0, D3plus);
			((KneserNeySmoothing)specialKneserNey.lowerOrderDist()).discountIs(discount);
		}
		HashMap<Element, ConditionalFrequencyDistribution> conditionalDistributions = new HashMap<Element, ConditionalFrequencyDistribution>();
		for (Element condition: original.conditions()) {
			Ngram ngram = (Ngram)condition;
			Tag tag = (Tag)ngram.entry(ngram.size()-1);
			if (tagger.openClassTagSet().contains(tag))
				conditionalDistributions.put(condition, openClassKneserNey);
			else if (tagger.closedClassTagSet().contains(tag))
				conditionalDistributions.put(condition, closedClassKneserNey);
			else if (tagger.specialTagSet().contains(tag))
				conditionalDistributions.put(condition, specialKneserNey);
		}
		return new ConditionalFrequencyDistributionImpl2(conditionalDistributions);
	}
	
	public static KneserNeySmoothing getModifiedKneserNeySmoothingTransitionFrequencyDistrubution(ArkHmmTagger tagger, ConditionalFrequencyDistribution original) {
		HashMap<Double, Counts> freqCounts = new HashMap<Double, Counts>();
		for (double d = 1.0; d <= 5.0; d += 1.0)
			freqCounts.putIfAbsent(d, new Counts());
		for (Element condition: original.conditions()) {
			Counts counts = new Counts();
			for (Element event: original.events(condition)) {
				double count = original.count(condition, event);
				if (count == 0.0)
					continue;
				counts.valueInc(event, count);
				freqCounts.get(Math.min(count, 5.0)).valueInc(condition, 1.0);
			}
		}
		double n1 = freqCounts.get(1.0).total();
		double n2 = freqCounts.get(2.0).total();
		double n3 = freqCounts.get(3.0).total();
		double n4 = freqCounts.get(4.0).total();
		double Y = n1/(n1+2*n2);
		double D1 = 1-2*Y*n2/n1;
		double D2 = 2-3*Y*n3/(n2);
		double D3plus = 3-4*Y*n4/(n3);
		D1 = 0.0001;
		D2 = 0.0001;
		D3plus = 0.00001;
		Map<Double, Double> discount = new HashMap<Double, Double>();
		discount.put(1.0, D1);
		discount.put(2.0, D2);
		discount.put(3.0, D3plus);
		KneserNeySmoothing kn = new KneserNeySmoothing(original);
		kn.discountIs(discount);
		if (kn.lowerOrderDist() instanceof KneserNeySmoothing) {
			freqCounts = new HashMap<Double, Counts>();
			for (double d = 1.0; d <= 5.0; d += 1.0)
				freqCounts.putIfAbsent(d, new Counts());
			for (Element condition: kn.lowerOrderDist().conditions()) {
				Counts counts = new Counts();
				for (Element event: kn.lowerOrderDist().events(condition)) {
					double count = kn.lowerOrderDist().count(condition, event);
					if (count == 0.0)
						continue;
					counts.valueInc(event, count);
					freqCounts.get(Math.min(count, 5.0)).valueInc(condition, 1.0);
				}
			}
			n1 = freqCounts.get(1.0).total();
			n2 = freqCounts.get(2.0).total();
			n3 = freqCounts.get(3.0).total();
			n4 = freqCounts.get(4.0).total();
			Y = n1/(n1+2*n2);
			D1 = 1-2*Y*n2/n1;
			D2 = 2-3*Y*n3/(n2);
			D3plus = 3-4*Y*n4/(n3);
			D1 = 0.1;
			D2 = 0.1;
			D3plus = 0.1;
			discount = new HashMap<Double, Double>();
			discount.put(1.0, D1);
			discount.put(2.0, D2);
			discount.put(3.0, D3plus);
			((KneserNeySmoothing)kn.lowerOrderDist()).discountIs(discount);
		}
		return kn;
	}
}
