package aseker00.tlp.ark.pos.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import aseker00.tlp.ark.pos.ArkHmmTagger;
import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
import aseker00.tlp.ling.Tag;
import aseker00.tlp.ling.Token;
import aseker00.tlp.model.Counts;
import aseker00.tlp.model.disc.Feature;
import aseker00.tlp.model.disc.FeatureVector;
import aseker00.tlp.model.disc.MaximumEntropyMarkovModel;
import aseker00.tlp.model.gen.ConditionalFrequencyDistributionImpl;
import aseker00.tlp.model.gen.FrequencyDistribution;
import aseker00.tlp.model.gen.HiddenMarkovModel;
import aseker00.tlp.model.gen.MaximumLikelihoodFrequencyDistribution;

public class ModelLoader {

	public static void loadModel(ArkHmmTagger tagger, HiddenMarkovModel hmm, File file) throws IOException {
		HashMap<Element, Counts> emissionCounts = new HashMap<Element, Counts>();
		HashMap<Element, Counts> transitionCounts = new HashMap<Element, Counts>();
		HashSet<Element> emissionEvents = new HashSet<Element>();
		HashSet<Element> transitionEvents = new HashSet<Element>();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		while ((line = reader.readLine()) != null) {
			String[] lineParts = line.split("\t");
			String type = lineParts[0];
			String str = lineParts[1];
			double value = Double.parseDouble(lineParts[2]);
			if (type.equals("WORDTAG")) {
				Element[] elements = new Element[hmm.eChain()-1];
				String[] elementParts = str.substring(1, str.length()-1).split(", ");
				for (int i = 0; i < elements.length; i++)
					elements[i] = tagger.tag(elementParts[i]);
				Ngram condition = new Ngram(elements);
				Token event;
//				if (tagger.isSpecial(elementParts[elements.length]))
//					event = tagger.specialToken(elementParts[elements.length]);
//				else
//					event = tagger.token(elementParts[elements.length]);
				event = tagger.token(elementParts[elements.length]);
				emissionCounts.putIfAbsent(condition, new Counts());
				emissionCounts.get(condition).valueInc(event, value);
				emissionEvents.add(event);
			} else if (type.endsWith("GRAM")) {
				// String[] gramParts = frequencyType.split("-");
				// String gramType = gramParts[0];
				// int gram = Integer.parseInt(gramType);
				Element[] elements = new Element[hmm.chain()-1];
				String[] elementParts = str.substring(1, str.length()-1).split(", ");
				for (int i = 0; i < elements.length; i++)
					elements[i] = tagger.tag(elementParts[i]);
				Ngram condition = new Ngram(elements);
				Tag event = tagger.tag(elementParts[elements.length]);
				transitionCounts.putIfAbsent(condition, new Counts());
				transitionCounts.get(condition).valueInc(event, value);
				transitionEvents.add(event);
			}
		}
		HashMap<Element, FrequencyDistribution> emissions = new HashMap<Element, FrequencyDistribution>();
		for (Element condition : emissionCounts.keySet()) {
			Counts counts = emissionCounts.get(condition);
			FrequencyDistribution dist = new MaximumLikelihoodFrequencyDistribution(emissionEvents, counts);
			emissions.put(condition, dist);
		}
		HashMap<Element, FrequencyDistribution> transitions = new HashMap<Element, FrequencyDistribution>();
		for (Element condition : transitionCounts.keySet()) {
			Counts counts = transitionCounts.get(condition);
			FrequencyDistribution dist = new MaximumLikelihoodFrequencyDistribution(transitionEvents, counts);
			transitions.put(condition, dist);
		}
		hmm.emissionProbabilityDistributionIs(new ConditionalFrequencyDistributionImpl(emissions));
		hmm.transitionProbabilityDistributionIs(new ConditionalFrequencyDistributionImpl(transitions));
		reader.close();
	}

	public static void loadModel(MaximumEntropyMarkovModel memm, File file) throws IOException {
		FeatureVector weights = memm.featureVectorNew();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		while ((line = reader.readLine()) != null) {
			String[] lineParts = line.split("\t");
			String type = lineParts[0];
			int index = Integer.valueOf(lineParts[1]);
			String str = lineParts[2];
			double value = Double.parseDouble(lineParts[3]);
			if (type.equals("FEATUREWEIGHT")) {
				Feature feature = memm.function().featureNew(str, index, value);
				weights.entryIs(feature.index(), feature);
			}
		}
		reader.close();
		memm.weightsVectorIs(weights);
	}
}