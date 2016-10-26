package aseker00.tlp.ark.pos.model;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
import aseker00.tlp.model.ConditionalProbabilityDistribution;
import aseker00.tlp.model.disc.Feature;
import aseker00.tlp.model.disc.MaximumEntropyMarkovModel;
import aseker00.tlp.model.gen.ConditionalFrequencyDistribution;
import aseker00.tlp.model.gen.HiddenMarkovModel;

public class ModelPrinter {

	public static void printConditionalFrequency(ConditionalFrequencyDistribution dist, File file) throws IOException {
		PrintWriter writer = new PrintWriter(file);
		for (Element condition : dist.conditions()) {
			Set<Element> events = dist.events(condition);
			for (Element event : events) {
				double c = dist.count(condition, event);
				writer.println(condition + "\t" + event + "\t" + c);
			}
		}
		writer.close();
	}

	public static void printConditionalProbability(ConditionalProbabilityDistribution dist, File file)
			throws IOException {
		PrintWriter writer = new PrintWriter(file);
		for (Element condition : dist.conditions()) {
			Set<Element> events = dist.events(condition);
			for (Element event : events) {
				double p = dist.probability(condition, event);
				writer.println(condition + "\t" + event + "\t" + p);
			}
		}
		writer.close();
	}
	
	public static void printConditionalProbability(ConditionalProbabilityDistribution dist, Set<Element> events, File file)
			throws IOException {
		PrintWriter writer = new PrintWriter(file);
		for (Element condition : dist.conditions()) {
			for (Element event : events) {
				double p = dist.probability(condition, event);
				writer.println(condition + "\t" + event + "\t" + p);
			}
		}
		writer.close();
	}

	public static void printModel(HiddenMarkovModel hmm, File file) throws IOException {
		PrintWriter writer = new PrintWriter(file);
		Set<Element> conditions = hmm.emissionProbabilityDistribution().conditions();
		for (Element condition : conditions) {
			Set<Element> events = hmm.emissionProbabilityDistribution().events(condition);
			for (Element observation : events) {
				double value = ((ConditionalFrequencyDistribution) hmm.emissionProbabilityDistribution()).count(condition, observation);
				if (value == 0.0)
					continue;
				Ngram emission = hmm.emission((Ngram)condition, observation);
				writer.println("WORDTAG\t" + emission + "\t" + value);
			}
		}
		conditions = hmm.transitionProbabilityDistribution().conditions();
		for (Element condition : conditions) {
			Set<Element> tags = hmm.transitionProbabilityDistribution().events(condition);
			for (Element label : tags) {
				double value = ((ConditionalFrequencyDistribution) hmm.transitionProbabilityDistribution()).count(condition, label);
				if (value == 0.0)
					continue;
				Ngram transition = hmm.transition((Ngram)condition, label);
				writer.println(transition.size() + "-GRAM\t" + transition + "\t" + value);
			}
		}
		writer.close();
	}

	public static void printModel(MaximumEntropyMarkovModel memm, File file) throws IOException {
		PrintWriter writer = new PrintWriter(file);
		for (Feature feature: memm.function().features()) {
            int index = feature.index();
            double value = memm.parameters().entryValue(index);
            if (value != 0)
            	writer.println("FEATUREWEIGHT\t" + index + "\t" + feature.name() + "\t" + value);
		}
		writer.close();
    }
}