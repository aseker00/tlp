package aseker00.tlp.ark.pos;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.math.NumberUtils;

import aseker00.tlp.ark.io.ArkCorpus;
import aseker00.tlp.ark.pos.model.ModelPrinter;
import aseker00.tlp.ark.pos.model.ModifiedKneserNeyModelFactory;
import aseker00.tlp.decoding.ViterbiDecoder;
import aseker00.tlp.estimation.MaximumLikelihoodEstimation;
import aseker00.tlp.evaluation.TagAccuracy;
import aseker00.tlp.evaluation.TagAccuracyEvaluationJob;
import aseker00.tlp.io.Corpus;
import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
import aseker00.tlp.ling.Tag;
import aseker00.tlp.ling.Token;
import aseker00.tlp.model.Counts;
import aseker00.tlp.model.LinearChainSequenceLabelModel;
import aseker00.tlp.model.gen.ConditionalFrequencyDistribution;
import aseker00.tlp.model.gen.ConditionalFrequencyDistributionImpl;
import aseker00.tlp.model.gen.FrequencyDistribution;
import aseker00.tlp.model.gen.MaximumLikelihoodFrequencyDistribution;
import aseker00.tlp.mt.impl.estimation.MaximumLikelihoodEstimationMT;
import aseker00.tlp.mt.impl.evaluation.TagAccuracyEvaluationJobMT;
import aseker00.tlp.model.gen.HiddenMarkovModel;

import aseker00.tlp.pos.SpecialToken;
import aseker00.tlp.pos.TagSet;
import aseker00.tlp.pos.Tagger;
import aseker00.tlp.proc.Estimator;
import aseker00.tlp.proc.EstimationThread;
import aseker00.tlp.proc.Processor;

public class ArkHmmTagger extends Tagger {
	
	private HashMap<Pattern, Token> specialPatternTokens;
	private Token numberToken;

	public ArkHmmTagger(String name) {
		super(name);
		this.specialPatternTokens = new HashMap<Pattern, Token>();
	}
	
	public void specialPatternTokenIs(Pattern p, Token t) {
		this.specialPatternTokens.put(p, t);
	}
	
	public boolean isSpecial(String str) {
		Token token = new Token(str);
		return this.specialPatternTokens.values().contains(token);
	}
	
	public Token specialToken(String str) {
		Token token = new Token(str);
		if (this.specialPatternTokens.values().contains(token))
			return token;
		return null;
	}
	
	public void numberTokenIs(Token t) {
		this.numberToken = t;
	}
	
	@Override
	public Token token(String str) {
		for (Pattern p: this.specialPatternTokens.keySet()) {
			Token patternToken = this.specialPatternTokens.get(p);
			if (p.matcher(str).matches()) {
				Token special = new SpecialToken(str.toLowerCase(), patternToken);
				return special;
			}
			else if (str.equals(patternToken.toString())) {
				Token special = new SpecialToken(str, patternToken);
				return special;
			}
		}
		if (this.numberToken != null && NumberUtils.isNumber(str)) {
			Token special = new SpecialToken(str.toLowerCase(), this.numberToken);
			return special;
		}
		return super.token(str.toLowerCase());
	}
	
	@Override
	public TagSet tags(Token token) {
		return super.tags(token);
	}
	
	public static ConditionalFrequencyDistributionImpl getEmissionsFrequencyDistribution(ArkHmmTagger tagger, LinearChainSequenceLabelModel model) {
		HashMap<Element, FrequencyDistribution> conditionalDistributions = new HashMap<Element, FrequencyDistribution>();
		ArrayList<Tag> tagList = new ArrayList<Tag>();
		for (Tag tag: tagger.tagSet())
			tagList.add(tag);
		int total = (int)Math.pow(tagList.size(), model.eChain()-1);
		for (int i = 0; i < total; i++) {
			String baseRep = Integer.toString(i, tagList.size());
			while (baseRep.length() < model.eChain()-1)
				baseRep = "0" + baseRep;
			Element[] elements = new Element[model.eChain()-1];
			for (int j = 0; j < elements.length; j++) {
				int index = Integer.parseInt(baseRep.substring(j, j+1), tagList.size());
				Tag tag = tagList.get(index);
				elements[j] = tag;
			}
			Ngram condition = new Ngram(elements);
			FrequencyDistribution dist = new MaximumLikelihoodFrequencyDistribution(new HashSet<Element>(), new Counts());
			conditionalDistributions.put(condition, dist);
		}
		if (model.eChain() > 2) {
			tagList.add(tagger.startTag());
			total = (int)Math.pow(tagList.size(), model.eChain()-2);
			for (int i = 0; i < total; i++) {
				String baseRep = Integer.toString(i, tagList.size());
				while (baseRep.length() < model.eChain()-2)
					baseRep = "0" + baseRep;
				Element[] elements = new Element[model.eChain()-1];
				elements[0] = tagger.startTag();
				for (int j = 1; j < elements.length; j++) {
					int index = Integer.parseInt(baseRep.substring(j-1, j), tagList.size());
					Tag tag = tagList.get(index);
					elements[j] = tag;
				}
				Ngram condition = new Ngram(elements);
				FrequencyDistribution dist = new MaximumLikelihoodFrequencyDistribution(new HashSet<Element>(), new Counts());
				conditionalDistributions.put(condition, dist);
			}
		}
		return new ConditionalFrequencyDistributionImpl(conditionalDistributions);
	}
	
	public static ConditionalFrequencyDistributionImpl getTransitionsFrequencyDistribution(ArkHmmTagger tagger, LinearChainSequenceLabelModel model) {
		HashMap<Element, FrequencyDistribution> conditionalDistributions = new HashMap<Element, FrequencyDistribution>();
		ArrayList<Tag> tagList = new ArrayList<Tag>();
		for (Tag tag: tagger.tagSet())
			tagList.add(tag);
		int total = (int)Math.pow(tagList.size(), model.chain()-1);
		for (int i = 0; i < total; i++) {
			String baseRep = Integer.toString(i, tagList.size());
			while (baseRep.length() < model.chain()-1)
				baseRep = "0" + baseRep;
			Element[] elements = new Element[model.chain()-1];
			for (int j = 0; j < elements.length; j++) {
				int index = Integer.parseInt(baseRep.substring(j, j+1), tagList.size());
				Tag tag = tagList.get(index);
				elements[j] = tag;
			}
			Ngram condition = new Ngram(elements);
			FrequencyDistribution dist = new MaximumLikelihoodFrequencyDistribution(new HashSet<Element>(), new Counts());
			conditionalDistributions.put(condition, dist);
		}
		tagList.add(tagger.startTag());
		total = (int)Math.pow(tagList.size(), model.chain()-2);
		for (int i = 0; i < total; i++) {
			String baseRep = Integer.toString(i, tagList.size());
			while (baseRep.length() < model.chain()-2)
				baseRep = "0" + baseRep;
			Element[] elements = new Element[model.chain()-1];
			elements[0] = tagger.startTag();
			for (int j = 1; j < elements.length; j++) {
				int index = Integer.parseInt(baseRep.substring(j-1, j), tagList.size());
				Tag tag = tagList.get(index);
				elements[j] = tag;
			}
			Ngram condition = new Ngram(elements);
			FrequencyDistribution dist = new MaximumLikelihoodFrequencyDistribution(new HashSet<Element>(), new Counts());
			conditionalDistributions.put(condition, dist);
		}
		return new ConditionalFrequencyDistributionImpl(conditionalDistributions);
	}
	
	public static ConditionalFrequencyDistributionImpl getEstimatedEmissionsFrequencyDistribution(ArkHmmTagger tagger, MaximumLikelihoodEstimation mle) {
		HashMap<Element, Counts> conditionalCounts = new HashMap<Element, Counts>();
		ConditionalFrequencyDistributionImpl eDist = getEmissionsFrequencyDistribution(tagger, mle.model());
		for (Element condition: eDist.conditions())
			conditionalCounts.put(condition, new Counts());
		HashSet<Element> events = new HashSet<Element>();
		for (Element emission: mle.logLikelihoodFunction().emissions()) {
			double count = mle.logLikelihoodFunction().emissionCount(emission);
			Ngram ngram = (Ngram)emission;
			Element condition = ngram.subgram(0, ngram.size()-1);
			Element event = ngram.subgram(ngram.size()-1).entry(0);
			Counts counts = conditionalCounts.get(condition);
			counts.valueInc(event, count);
			events.add(event);
		}
		HashMap<Element, FrequencyDistribution> conditionalDistributions = new HashMap<Element, FrequencyDistribution>();
		for (Element condition: conditionalCounts.keySet()) {
			Counts counts = conditionalCounts.get(condition);
			FrequencyDistribution dist = new MaximumLikelihoodFrequencyDistribution(events, counts);
			conditionalDistributions.put(condition, dist);
		}
		return new ConditionalFrequencyDistributionImpl(conditionalDistributions);
	}
	
	public static ConditionalFrequencyDistributionImpl getEstimatedTransitionsFrequencyDistribution(ArkHmmTagger tagger, MaximumLikelihoodEstimation mle) {
		HashMap<Element, Counts> conditionalCounts = new HashMap<Element, Counts>();
		ConditionalFrequencyDistributionImpl tDist = getTransitionsFrequencyDistribution(tagger, mle.model());
		for (Element condition: tDist.conditions())
			conditionalCounts.put(condition, new Counts());
		HashSet<Element> events = new HashSet<Element>();
		for (Element transition: mle.logLikelihoodFunction().transitions()) {
			double count = mle.logLikelihoodFunction().transitionCount(transition);
			Ngram ngram = (Ngram)transition;
			Element condition = ngram.subgram(0, ngram.size()-1);
			Element event = ngram.subgram(ngram.size()-1).entry(0);
			Counts counts = conditionalCounts.get(condition);
			counts.valueInc(event, count);
			events.add(event);
		}
		HashMap<Element, FrequencyDistribution> conditionalDistributions = new HashMap<Element, FrequencyDistribution>();
		for (Element condition: conditionalCounts.keySet()) {
			Counts counts = conditionalCounts.get(condition);
			FrequencyDistribution dist = new MaximumLikelihoodFrequencyDistribution(events, counts);
			conditionalDistributions.put(condition, dist);
		}
		return new ConditionalFrequencyDistributionImpl(conditionalDistributions);
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
		int threads = cmd.hasOption("threads") ? Integer.parseInt(cmd.getOptionValue("threads")) : 4;
		ArkHmmTagger tagger = new ArkHmmTagger("ark");
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
		String[] openClassTagLabels = {"^", "N", "V", "!", "S", "Z", "M", "A", "R", "G"};
		String[] closedClassTagLabels = {"L", "O", "D", "P", "&", "T", "X", "Y", "~", ","};
		String[] specialTagLabels = {"#", "@", "U", "E", "$"};
		HashSet<Tag> openClassTags = new HashSet<Tag>();
		HashSet<Tag> closedClassTags = new HashSet<Tag>();
		HashSet<Tag> specialTags = new HashSet<Tag>();
		for (String label: openClassTagLabels)
			openClassTags.add(new Tag(label));
		for (String label: closedClassTagLabels)
			closedClassTags.add(new Tag(label));
		for (String label: specialTagLabels)
			specialTags.add(new Tag(label));
		tagger.openClassTagSetIs(new TagSet("open", openClassTags));
		tagger.closedClassTagSetIs(new TagSet("closed", closedClassTags));
		tagger.specialTagSetIs(new TagSet("special", specialTags));
		ArkCorpus testCorpus = new ArkCorpus("oct27.test", new File(base, "data/oct27.test"), tagger);
		ArkCorpus trainCorpus = new ArkCorpus("oct27.traindev", new File(base, "data/oct27.traindev"), tagger);
		HiddenMarkovModel hmm = new HiddenMarkovModel("hmm", tagger, chain, chain);
		ConditionalFrequencyDistributionImpl emissions = getEmissionsFrequencyDistribution(tagger, hmm);
		ConditionalFrequencyDistributionImpl transitions = getTransitionsFrequencyDistribution(tagger, hmm);
		hmm.emissionProbabilityDistributionIs(emissions);
		hmm.transitionProbabilityDistributionIs(transitions);
		tagger.modelIs(hmm);
		
		MaximumLikelihoodEstimation mle = new MaximumLikelihoodEstimationMT("mle_" + trainCorpus.name(), tagger, threads);
		mle.modelIs(hmm);
		
		TagAccuracy acc = new TagAccuracy(tagger.model().name(), tagger);
		TagAccuracyEvaluationJob evaluationJob = new TagAccuracyEvaluationJobMT(hmm, new ViterbiDecoder("viterbi", tagger), threads);
		acc.evaluationJobIs(evaluationJob);
		
		EstimationThread estimation = new EstimationThread(tagger);
		Estimator.Notifiee mleNotifiee = new Estimator.Notifiee() {
			@Override
			public void onDataSet(Corpus data) {
				Calendar cal = Calendar.getInstance();
		        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				System.out.println("[" + sdf.format(cal.getTime()) + "][" + "mleNotifiee" + "]\tonDataSet");
				
				ConditionalFrequencyDistributionImpl emissions = getEstimatedEmissionsFrequencyDistribution(tagger, mle);
				ConditionalFrequencyDistributionImpl transitions = getEstimatedTransitionsFrequencyDistribution(tagger, mle);
				HashSet<Element> tokens = new HashSet<Element>();
				for (Element condition: emissions.conditions()) {
					for (Element event: emissions.events(condition)) {
						Token token = (Token)event;
						tokens.add(token);
					}
				}
				try {
					ModelPrinter.printConditionalFrequency(emissions, new File("output/" + tagger.name() + "_" + hmm.name() + "_emissions.txt"));
					ModelPrinter.printConditionalFrequency(transitions, new File("output/" + tagger.name() + "_" + hmm.name() + "_transitions.txt"));
					ModelPrinter.printConditionalProbability(emissions, tokens, new File("output/" + tagger.name() + "_" + hmm.name() + "_emissions_prob.txt"));
					ModelPrinter.printConditionalProbability(transitions, new File("output/" + tagger.name() + "_" + hmm.name() + "_transitions_prob.txt"));
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				ConditionalFrequencyDistribution smoothedEmissions = ModifiedKneserNeyModelFactory.getModifiedKneserNeySmoothingEmissionFrequencyDistrubution(tagger, emissions);
				ConditionalFrequencyDistribution smoothedTransitions = ModifiedKneserNeyModelFactory.getModifiedKneserNeySmoothingTransitionFrequencyDistrubution(tagger, transitions);
				hmm.emissionProbabilityDistributionIs(smoothedEmissions);
				hmm.transitionProbabilityDistributionIs(smoothedTransitions);
				try {
					ModelPrinter.printConditionalFrequency(smoothedEmissions, new File("output/" + tagger.name() + "_" + hmm.name() + "_smoothed_emissions.txt"));
					ModelPrinter.printConditionalFrequency(smoothedTransitions, new File("output/" + tagger.name() + "_" + hmm.name() + "_smoothed_transitions.txt"));
					ModelPrinter.printConditionalProbability(smoothedEmissions, tokens, new File("output/" + tagger.name() + "_" + hmm.name() + "_smoothed_emissions_prob.txt"));
					ModelPrinter.printConditionalProbability(smoothedTransitions, new File("output/" + tagger.name() + "_" + hmm.name() + "_smoothed_transitions_prob.txt"));
					ModelPrinter.printModel(hmm, new File("output/" + tagger.name() + "_" + hmm.name() + ".txt"));
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				acc.dataSetIs(testCorpus);
				
				tagger.modelIs(hmm);
				estimation.dataSetIs(null);
				acc.dataSetIs(null);
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
		mle.notifieeIs(mleNotifiee, mleNotifiee);
		tagger.notifieeIs(taggerNotifiee, taggerNotifiee);
		tagger.estimatorIs(trainCorpus, mle);
		tagger.modelIs(hmm);
		estimation.dataSetIs(trainCorpus);
		estimation.start();
	}
	
	public static CommandLine initCli(String args[]) throws ParseException {
		Options options = new Options();
		options.addOption("home", true, "workspace base directory");
		options.addOption("chain", true, "tagger linear chain size");
		options.addOption("size", true, "size of training set");
		options.addOption("threads", true, "number of threads");
		options.addOption("step", true, "lbfgs2 step size");
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		return cmd;
	}
}