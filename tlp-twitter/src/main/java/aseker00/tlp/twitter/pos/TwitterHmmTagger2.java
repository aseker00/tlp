package aseker00.tlp.twitter.pos;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.cli.CommandLine;

import aseker00.tlp.ark.io.ArkCorpus;

import aseker00.tlp.ark.pos.ArkHmmTagger;
import aseker00.tlp.ark.pos.ArkSpecialPatternTokens;
import aseker00.tlp.ark.pos.model.ModelPrinter;
import aseker00.tlp.ark.pos.model.ModifiedKneserNeyModelFactory;
import aseker00.tlp.decoding.ViterbiDecoder;
import aseker00.tlp.estimation.MaximumLikelihoodEstimation;
import aseker00.tlp.evaluation.TagAccuracy;
import aseker00.tlp.evaluation.TagAccuracyEvaluationJob;
import aseker00.tlp.io.Corpus;
import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Tag;
import aseker00.tlp.ling.Token;
import aseker00.tlp.model.Counts;
import aseker00.tlp.model.gen.ConditionalFrequencyDistribution;
import aseker00.tlp.model.gen.ConditionalFrequencyDistributionImpl;
import aseker00.tlp.model.gen.FrequencyDistribution;
import aseker00.tlp.model.gen.HiddenMarkovModel;
import aseker00.tlp.model.gen.MaximumLikelihoodFrequencyDistribution;
import aseker00.tlp.mt.impl.estimation.BaumWelchMT;
import aseker00.tlp.mt.impl.evaluation.TagAccuracyEvaluationJobMT;
import aseker00.tlp.pos.TagSet;
import aseker00.tlp.proc.Estimator;
import aseker00.tlp.proc.EstimationThread;
import aseker00.tlp.proc.Processor;
import aseker00.tlp.twitter.kafka.KafkaCorpus;

public class TwitterHmmTagger2 extends ArkHmmTagger {

	public TwitterHmmTagger2(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws Exception {
		CommandLine cmd = initCli(args);
		File base;
		if (cmd.hasOption("home")) {
			base = new File(cmd.getOptionValue("home"));
		} else {
			base = new File("/home/amit/aseker00/dev/tlp/");
		}
		int chain = cmd.hasOption("chain") ? Integer.parseInt(cmd.getOptionValue("chain")) : 2;
		int size;
		if (cmd.hasOption("size")) {
			size = Integer.parseInt(cmd.getOptionValue("size"));
		} else {
			size = 1000;
		}
		int threads = cmd.hasOption("threads") ? Integer.parseInt(cmd.getOptionValue("threads")) : 4;
		TwitterHmmTagger2 tagger = new TwitterHmmTagger2("ark");
		tagger.specialPatternTokenIs(ArkSpecialPatternTokens.AT_MENTION, new Token("___MENTION___"));
		tagger.specialPatternTokenIs(ArkSpecialPatternTokens.HASHTAG, new Token("___HASHTAG___"));
		tagger.specialPatternTokenIs(ArkSpecialPatternTokens.URL, new Token("___URL___"));
		tagger.specialPatternTokenIs(ArkSpecialPatternTokens.EMAIL, new Token("___URL___"));
		tagger.specialPatternTokenIs(ArkSpecialPatternTokens.EMOTICON, new Token("___EMOTICON___"));
		tagger.specialPatternTokenIs(ArkSpecialPatternTokens.EMOJI, new Token("___EMOTICON___"));
		tagger.specialPatternTokenIs(ArkSpecialPatternTokens.HEARTS_AND_ARROWS, new Token("___EMOTICON___"));
		tagger.specialPatternTokenIs(ArkSpecialPatternTokens.NUMBER, new Token("___NUMBER___"));
		tagger.numberTokenIs(new Token("___NUMBER___"));
		tagger.startTagIs(new Tag("*"));
		tagger.stopTagIs(new Tag("STOP"));
		String[] openClassTagLabels = { "N", "V", "!", "S", "Z", "M", "A", "R", "G", "^" };
		String[] closedClassTagLabels = { "L", "O", "D", "P", "&", "T", "X", "Y", "~", "," };
		String[] specialTagLabels = { "#", "@", "U", "E", "$" };
		HashSet<Tag> openClassTags = new HashSet<Tag>();
		HashSet<Tag> closedClassTags = new HashSet<Tag>();
		HashSet<Tag> specialTags = new HashSet<Tag>();
		for (String label : openClassTagLabels)
			openClassTags.add(new Tag(label));
		for (String label : closedClassTagLabels)
			closedClassTags.add(new Tag(label));
		for (String label : specialTagLabels)
			specialTags.add(new Tag(label));
		tagger.openClassTagSetIs(new TagSet("open", openClassTags));
		tagger.closedClassTagSetIs(new TagSet("closed", closedClassTags));
		tagger.specialTagSetIs(new TagSet("special", specialTags));
		ArkCorpus testCorpus = new ArkCorpus("oct27.test", new File(base, "data/oct27.test"), tagger);
		// ArkCorpus arkTrainCorpus = new ArkCorpus("oct27.traindev.1_200", new
		// File(base, "data/oct27.traindev.1_200"), tagger);
		ArkCorpus arkTrainCorpus = new ArkCorpus("oct27.traindev", new File(base, "data/oct27.traindev"), tagger);
		KafkaCorpus trainCorpus = new KafkaCorpus("stream210", size, "twitter", size * 0, tagger);
		HiddenMarkovModel hmm = new HiddenMarkovModel("hmm", tagger, chain, chain);
		ConditionalFrequencyDistributionImpl emissions = getEmissionsFrequencyDistribution(tagger, hmm);
		ConditionalFrequencyDistributionImpl transitions = getTransitionsFrequencyDistribution(tagger, hmm);
		hmm.emissionProbabilityDistributionIs(emissions);
		hmm.transitionProbabilityDistributionIs(transitions);
		tagger.modelIs(hmm);
		
		TagAccuracy acc = new TagAccuracy(hmm.name(), tagger);
		TagAccuracyEvaluationJob evaluationJob = new TagAccuracyEvaluationJobMT(hmm, new ViterbiDecoder("viterbi", tagger), threads);
		acc.evaluationJobIs(evaluationJob);
		
		MaximumLikelihoodEstimation mle = new MaximumLikelihoodEstimation("mle_" + arkTrainCorpus.name(), tagger);
		mle.modelIs(hmm);
		
		EstimationThread estimation = new EstimationThread(tagger);
		Estimator.Notifiee mleNotifiee = new Estimator.Notifiee() {
			@Override
			public void onDataSet(Corpus data) {
				Calendar cal = Calendar.getInstance();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				System.out.println("[" + sdf.format(cal.getTime()) + "][" + "mleNotifiee" + "]\tonDataSet");

				ConditionalFrequencyDistributionImpl emissions = getEstimatedEmissionsFrequencyDistribution(tagger, mle);
				ConditionalFrequencyDistributionImpl transitions = getEstimatedTransitionsFrequencyDistribution(tagger, mle);
				try {
					ModelPrinter.printConditionalFrequency(emissions, new File("output/emissions.txt"));
					ModelPrinter.printConditionalFrequency(transitions, new File("output/transitions.txt"));
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				ConditionalFrequencyDistribution smoothedEmissions = ModifiedKneserNeyModelFactory
						.getModifiedKneserNeySmoothingEmissionFrequencyDistrubution(tagger, emissions);
				ConditionalFrequencyDistribution smoothedTransitions = ModifiedKneserNeyModelFactory
						.getModifiedKneserNeySmoothingTransitionFrequencyDistrubution(tagger, transitions);
				hmm.emissionProbabilityDistributionIs(smoothedEmissions);
				hmm.transitionProbabilityDistributionIs(smoothedTransitions);
				// hmm.transitionProbabilityDistributionIs(transitions);
				acc.dataSetIs(testCorpus);
				tagger.modelIs(hmm);
				synchronized (tagger) {
					tagger.notify();
				}
			}
		};
		mle.notifieeIs(mleNotifiee, mleNotifiee);
		tagger.estimatorIs(arkTrainCorpus, mle);
		estimation.dataSetIs(arkTrainCorpus);
		synchronized (tagger) {
			try {
				tagger.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		mle.notifieeIs(mleNotifiee, null);
		ConditionalFrequencyDistributionImpl mleEmissions = getEstimatedEmissionsFrequencyDistribution(tagger, mle);
		ConditionalFrequencyDistributionImpl mleTransitions = getEstimatedTransitionsFrequencyDistribution(tagger, mle);
		MaximumLikelihoodEstimation bw = new BaumWelchMT("bw", tagger, threads);
		bw.modelIs(hmm);
		Estimator.Notifiee bwNotifiee = new Estimator.Notifiee() {
			int round = 1;

			@Override
			public void onDataSet(Corpus data) {
				Calendar cal = Calendar.getInstance();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				System.out.println("[" + sdf.format(cal.getTime()) + "][" + "bwNotifiee" + "]\tround " + this.round
						+ " log likelihood value = " + bw.logLikelihoodFunction().value());

				ConditionalFrequencyDistributionImpl newEmissions = getEstimatedEmissionsFrequencyDistribution(tagger, bw);
				ConditionalFrequencyDistributionImpl newTransitions = getEstimatedTransitionsFrequencyDistribution(tagger, bw);
				HashSet<Element> tokens = new HashSet<Element>();
				for (Element condition: newEmissions.conditions())
					tokens.addAll(newEmissions.events(condition));
				try {
					ModelPrinter.printConditionalFrequency(newEmissions, new File("output/" + tagger.name() + "_" + hmm.name() + "_emissions_" + this.round + ".txt"));
					ModelPrinter.printConditionalFrequency(newTransitions, new File("output/" + tagger.name() + "_" + hmm.name() + "_transitions_" + this.round + ".txt"));
					ModelPrinter.printConditionalProbability(newEmissions, tokens, new File("output/" + tagger.name() + "_" + hmm.name() + "_emissions_prob_" + this.round + ".txt"));
					ModelPrinter.printConditionalProbability(newTransitions, new File("output/" + tagger.name() + "_" + hmm.name() + "_transitions_prob_" + this.round + ".txt"));
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				HashMap<Element, Counts> emissionCounts = new HashMap<Element, Counts>();
				HashSet<Element> emissionEvents = new HashSet<Element>();
				for (Element condition : newEmissions.conditions()) {
					emissionCounts.put(condition, new Counts());
					for (Element event : newEmissions.events(condition)) {
						double count = newEmissions.count(condition, event);
						DecimalFormat df = new DecimalFormat("#.#");
						df.setRoundingMode(RoundingMode.CEILING);
						double d = Double.valueOf(df.format(count));
						emissionCounts.get(condition).valueInc(event, Math.floor(d));
						emissionEvents.add(event);
					}
				}
				for (Element condition : mleEmissions.conditions()) {
					emissionCounts.putIfAbsent(condition, new Counts());
					for (Element event : mleEmissions.events(condition)) {
						double count = mleEmissions.count(condition, event);
						emissionCounts.get(condition).valueInc(event, count);
						emissionEvents.add(event);
					}
				}
				HashMap<Element, Counts> transitionCounts = new HashMap<Element, Counts>();
				HashSet<Element> transitionEvents = new HashSet<Element>();
				for (Element condition : newTransitions.conditions()) {
					transitionCounts.put(condition, new Counts());
					for (Element event : newTransitions.events(condition)) {
						double count = newTransitions.count(condition, event);
						DecimalFormat df = new DecimalFormat("#.#");
						df.setRoundingMode(RoundingMode.CEILING);
						double d = Double.valueOf(df.format(count));
						transitionCounts.get(condition).valueInc(event, Math.floor(d));
						transitionEvents.add(event);
					}
				}
				for (Element condition : mleTransitions.conditions()) {
					transitionCounts.putIfAbsent(condition, new Counts());
					for (Element event : mleTransitions.events(condition)) {
						double count = mleTransitions.count(condition, event);
						transitionCounts.get(condition).valueInc(event, count);
						transitionEvents.add(event);
					}
				}
				HashMap<Element, FrequencyDistribution> emissionDistributions = new HashMap<Element, FrequencyDistribution>();
				HashMap<Element, FrequencyDistribution> transitionDistributions = new HashMap<Element, FrequencyDistribution>();
				for (Element condition : emissionCounts.keySet()) {
					Counts counts = emissionCounts.get(condition);
					FrequencyDistribution dist = new MaximumLikelihoodFrequencyDistribution(emissionEvents, counts);
					emissionDistributions.put(condition, dist);
				}

				for (Element condition : transitionCounts.keySet()) {
					Counts counts = transitionCounts.get(condition);
					FrequencyDistribution dist = new MaximumLikelihoodFrequencyDistribution(transitionEvents, counts);
					transitionDistributions.put(condition, dist);
				}
				newEmissions = new ConditionalFrequencyDistributionImpl(emissionDistributions);
				newTransitions = new ConditionalFrequencyDistributionImpl(transitionDistributions);
				try {
					ModelPrinter.printConditionalFrequency(newEmissions, new File("output/" + tagger.name() + "_" + hmm.name() + "_smoothed_emissions_" + this.round + ".txt"));
					ModelPrinter.printConditionalFrequency(newTransitions, new File("output/" + tagger.name() + "_" + hmm.name() + "_smoothed_transitions_" + this.round + ".txt"));
					ModelPrinter.printConditionalProbability(newEmissions, tokens, new File("output/" + tagger.name() + "_" + hmm.name() + "_smoothed_emissions_prob_" + this.round + ".txt"));
					ModelPrinter.printConditionalProbability(newTransitions, new File("output/" + tagger.name() + "_" + hmm.name() + "_smoothed_transitions_prob_" + this.round + ".txt"));
					ModelPrinter.printModel(hmm, new File("output/" + tagger.name() + "_" + hmm.name() + "_" + this.round + ".txt"));
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				ConditionalFrequencyDistribution smoothedEmissions = ModifiedKneserNeyModelFactory
						.getModifiedKneserNeySmoothingEmissionFrequencyDistrubution(tagger, newEmissions);
				ConditionalFrequencyDistribution smoothedTransitions = ModifiedKneserNeyModelFactory
						.getModifiedKneserNeySmoothingTransitionFrequencyDistrubution(tagger, newTransitions);
				hmm.emissionProbabilityDistributionIs(smoothedEmissions);
				hmm.transitionProbabilityDistributionIs(smoothedTransitions);
				acc.dataSetIs(testCorpus);

				newEmissions = getEstimatedEmissionsFrequencyDistribution(tagger, bw);
				newTransitions = getEstimatedTransitionsFrequencyDistribution(tagger, bw);
				// hmm.emissionProbabilityDistributionIs(newEmissions);
				// hmm.transitionProbabilityDistributionIs(newTransitions);
				emissionCounts = new HashMap<Element, Counts>();
				emissionEvents = new HashSet<Element>();
				for (Element condition : newEmissions.conditions()) {
					emissionCounts.put(condition, new Counts());
					for (Element event : newEmissions.events(condition)) {
						double count = newEmissions.count(condition, event);
						DecimalFormat df = new DecimalFormat("#.#");
						df.setRoundingMode(RoundingMode.CEILING);
						double d = Double.valueOf(df.format(count));
						emissionCounts.get(condition).valueInc(event, Math.floor(d));
						emissionEvents.add(event);
					}
				}
				transitionCounts = new HashMap<Element, Counts>();
				transitionEvents = new HashSet<Element>();
				for (Element condition : newTransitions.conditions()) {
					transitionCounts.put(condition, new Counts());
					for (Element event : newTransitions.events(condition)) {
						double count = newTransitions.count(condition, event);
						DecimalFormat df = new DecimalFormat("#.#");
						df.setRoundingMode(RoundingMode.CEILING);
						double d = Double.valueOf(df.format(count));
						transitionCounts.get(condition).valueInc(event, Math.floor(d));
						transitionEvents.add(event);
					}
				}
				emissionDistributions = new HashMap<Element, FrequencyDistribution>();
				transitionDistributions = new HashMap<Element, FrequencyDistribution>();
				for (Element condition : emissionCounts.keySet()) {
					Counts counts = emissionCounts.get(condition);
					FrequencyDistribution dist = new MaximumLikelihoodFrequencyDistribution(emissionEvents, counts);
					emissionDistributions.put(condition, dist);
				}
				for (Element condition : transitionCounts.keySet()) {
					Counts counts = transitionCounts.get(condition);
					FrequencyDistribution dist = new MaximumLikelihoodFrequencyDistribution(transitionEvents, counts);
					transitionDistributions.put(condition, dist);
				}
				newEmissions = new ConditionalFrequencyDistributionImpl(emissionDistributions);
				newTransitions = new ConditionalFrequencyDistributionImpl(transitionDistributions);
				smoothedEmissions = ModifiedKneserNeyModelFactory
						.getModifiedKneserNeySmoothingEmissionFrequencyDistrubution(tagger, newEmissions);
				smoothedTransitions = ModifiedKneserNeyModelFactory
						.getModifiedKneserNeySmoothingTransitionFrequencyDistrubution(tagger, newTransitions);
				// smoothedEmissions =
				// getAdditiveSmoothingEmissionsFrequencyDistrubution(tagger,
				// newEmissions);
				// smoothedTransitions =
				// getAdditiveSmoothingTransitionsFrequencyDistrubution(tagger,
				// newTransitions);
				// hmm.emissionProbabilityDistributionIs(smoothedEmissions);
				// hmm.transitionProbabilityDistributionIs(smoothedTransitions);
				// hmm.emissionProbabilityDistributionIs(newEmissions);
				// hmm.transitionProbabilityDistributionIs(newTransitions);
				this.round++;
				estimation.dataSetIs(trainCorpus);
			}
		};
		Processor.Notifiee taggerNotifiee = new Processor.Notifiee() {
			@Override
			public void onModel() {
				Calendar cal = Calendar.getInstance();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				System.out.println("[" + sdf.format(cal.getTime()) + "][" + "taggerNotifiee" + "]\tonModel");
			}
		};
		bw.notifieeIs(bwNotifiee, bwNotifiee);
		tagger.notifieeIs(taggerNotifiee, taggerNotifiee);
		tagger.estimatorIs(trainCorpus, bw);
		estimation.dataSetIs(trainCorpus);
		estimation.start();
	}
}