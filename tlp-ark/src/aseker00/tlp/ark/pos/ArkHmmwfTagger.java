package aseker00.tlp.ark.pos;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;

import aseker00.tlp.ark.io.ArkCorpus;
import aseker00.tlp.ark.pos.model.ModelPrinter;
import aseker00.tlp.decoding.ViterbiDecoder;
import aseker00.tlp.estimation.Lbfgs;
import aseker00.tlp.estimation.MaximumLikelihoodEstimation;
import aseker00.tlp.estimation.MaximumLikelihoodEstimationWithFeatures;
import aseker00.tlp.evaluation.TagAccuracy;
import aseker00.tlp.evaluation.TagAccuracyEvaluationJob;
import aseker00.tlp.io.Corpus;
import aseker00.tlp.io.CorpusReader;
import aseker00.tlp.io.Tweet;
import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
import aseker00.tlp.ling.Tag;
import aseker00.tlp.ling.Token;
import aseker00.tlp.model.LabeledSequence;
import aseker00.tlp.model.LabeledSequenceImpl;
import aseker00.tlp.model.disc.EmissionFeatureTemplate;
import aseker00.tlp.model.disc.Feature;
import aseker00.tlp.model.disc.FeatureFunction;
import aseker00.tlp.model.disc.FeatureTemplate;
import aseker00.tlp.model.disc.FeatureVector;
import aseker00.tlp.model.disc.L2Regularization;
import aseker00.tlp.model.disc.MaximumEntropyMarkovModel;
import aseker00.tlp.model.disc.TransitionFeatureTemplate;
import aseker00.tlp.mt.impl.estimation.LbfgsWithFeaturesMT;
import aseker00.tlp.mt.impl.evaluation.TagAccuracyEvaluationJobMT;
import aseker00.tlp.model.gen.HiddenMarkovModel;

import aseker00.tlp.pos.TagSet;
import aseker00.tlp.proc.Estimator;
import aseker00.tlp.proc.EstimationThread;
import aseker00.tlp.proc.Processor;

public class ArkHmmwfTagger extends ArkHmmTagger {

	public ArkHmmwfTagger(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws Exception {
		CommandLine cmd = ArkHmmTagger.initCli(args);
		File base;
		if (cmd.hasOption("home")) {
			base = new File(cmd.getOptionValue("home"));
		} else {
			base = new File("/home/amit/aseker00/dev/tlp/");
		}
		int chain = cmd.hasOption("chain") ? Integer.parseInt(cmd.getOptionValue("chain")) : 2;
		int threads = cmd.hasOption("threads") ? Integer.parseInt(cmd.getOptionValue("threads")) : 4;
		ArkHmmwfTagger tagger = new ArkHmmwfTagger("ark");
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
		
		FeatureFunction ff = new FeatureFunction();
		MaximumEntropyMarkovModel memm = new MaximumEntropyMarkovModel("memm", tagger, chain, chain, ff);
		FeatureTemplate eTemplate = new EmissionFeatureTemplate(memm);
		FeatureTemplate tTemplate = new TransitionFeatureTemplate(memm);
		memm.featureFunctionTemplateIs(eTemplate, eTemplate);
		memm.featureFunctionTemplateIs(tTemplate, tTemplate);
		FeatureVector weights = memm.featureVectorNew();
		HashMap<Element, Set<Element>> tContextDecisions = new HashMap<Element, Set<Element>>();
		HashMap<Element, Set<Element>> eContextDecisions = new HashMap<Element, Set<Element>>();
		CorpusReader stream = trainCorpus.readerNew(trainCorpus.name() + "::" + tagger.name());
		Tweet tweet;
		while ((tweet = stream.next()) != null) {
			ArrayList<Element> entries = new ArrayList<Element>();
			ArrayList<Element> labels = new ArrayList<Element>();
			for (int i = 0; i < tweet.tokens(); i++) {
				entries.add(tweet.token(i));
				labels.add(tweet.tag(i));
			}
			LabeledSequence sequence = new LabeledSequenceImpl(entries, labels);
			for (int i = 0; i < sequence.length(); i++) {
				memm.featureVector(sequence, i);
				Ngram transition = memm.transition(sequence, i);
				Ngram emission = memm.emission(sequence, i);
				Ngram tCondition = transition.subgram(0, transition.size()-1);
				Ngram eCondition = emission.subgram(0, emission.size()-1);
				tContextDecisions.putIfAbsent(tCondition, new HashSet<Element>());
				eContextDecisions.putIfAbsent(eCondition, new HashSet<Element>());
				tContextDecisions.get(tCondition).add(sequence.label(i));
				eContextDecisions.get(eCondition).add(sequence.entry(i));
			}
			memm.featureVector(sequence, sequence.length());
			Ngram transition = memm.transition(sequence, sequence.length());
			Ngram tCondition = transition.subgram(0, transition.size()-1);
			tContextDecisions.putIfAbsent(tCondition, new HashSet<Element>());
			tContextDecisions.get(tCondition).add(tagger.stopTag());
		}
		List<Feature> features = memm.function().features();
		for (Feature feature : features) {
			double value = 1.0 - Math.random();
			Feature weight = new Feature(ff, feature.index());
			weight.valueIs(value);
			weights.entryIs(weight.index(), weight);
		}
		memm.weightsVectorIs(weights);
		MaximumEntropyMarkovModel eMemm = new MaximumEntropyMarkovModel("eMemm", tagger, chain, chain, ff) {
			@Override
			public double conditionalProbability(LabeledSequence sequence, int pos) {
				Ngram emission = this.emission(sequence, pos);
				Ngram c = emission.subgram(0, emission.size()-1);
				Element d = emission.entry(emission.size()-1);
				return this.probability(c, d);
			}
			@Override
			public double probability(Element condition, Element event) {
				Ngram emission = this.emission((Ngram)condition, event);
				FeatureVector fv = this.featureVector(null, emission);
				double p = this.parameters().dotProduct(fv);
				double e = Math.exp(p);
				double z = this.getNorm(condition);
				double value = e / z;
				return value;
			}
		};
		MaximumEntropyMarkovModel tMemm = new MaximumEntropyMarkovModel("tMemm", tagger, chain, chain, ff) {
			@Override
			public double conditionalProbability(LabeledSequence sequence, int pos) {
				Ngram transition = this.transition(sequence, pos);
				Ngram c = transition.subgram(0, transition.size()-1);
				Element d = transition.entry(transition.size()-1);
				return this.probability(c, d);
			}
			@Override
			public double probability(Element condition, Element event) {
				Ngram transition = this.transition((Ngram)condition, event);
				FeatureVector fv = this.featureVector(transition, null);
				double p = this.parameters().dotProduct(fv);
				double e = Math.exp(p);
				double z = this.getNorm(condition);
				double value = e / z;
				return value;
			}
		};
		FeatureVector eWeights = memm.featureVectorNew();
		FeatureVector tWeights = memm.featureVectorNew();
		for (Feature weight: weights.features()) {
			if (memm.function().feature(eTemplate, weight.index()) != null)
				eWeights.entryIs(weight.index(), weight);
			else if (memm.function().feature(tTemplate, weight.index()) != null)
				tWeights.entryIs(weight.index(), weight);
		}
		eMemm.weightsVectorIs(eWeights);
		tMemm.weightsVectorIs(tWeights);
		HashSet<Element> tDecisions = new HashSet<Element>();
		HashSet<Element> eDecisions = new HashSet<Element>();
		for (Element c: tContextDecisions.keySet()) 
			tDecisions.addAll(tContextDecisions.get(c));
		for (Element c: eContextDecisions.keySet()) 
			eDecisions.addAll(eContextDecisions.get(c));
		for (Element c: tContextDecisions.keySet()) {
			tMemm.eventSetIs(c, new HashSet<Element>());
			for (Element d: tDecisions) {
				Ngram transition = tMemm.transition((Ngram)c, d);
				tMemm.events(c).add(transition.entry(transition.size()-1));
			}
		}
		for (Element c: eContextDecisions.keySet()) {
			eMemm.eventSetIs(c, new HashSet<Element>());
			for (Element d: eDecisions) {
				Ngram emission = eMemm.emission((Ngram)c, d);
				eMemm.events(c).add(emission.entry(emission.size()-1));
			}
		}
		HiddenMarkovModel hmm = new HiddenMarkovModel("hmm", tagger, chain, chain);
		hmm.emissionProbabilityDistributionIs(eMemm);
		hmm.transitionProbabilityDistributionIs(tMemm);
		tagger.modelIs(hmm);
		MaximumLikelihoodEstimation mle = new MaximumLikelihoodEstimationWithFeatures("mle", tagger);
		mle.modelIs(hmm);
		Lbfgs lbfgs2 = new LbfgsWithFeaturesMT("lbfgs", tagger, mle, threads);
		lbfgs2.modelIs(memm);
		L2Regularization regularization = new L2Regularization();
		regularization.lambdaIs(0.1);
		lbfgs2.featureRegularizationIs(regularization);
		TagAccuracy acc = new TagAccuracy(tagger.model().name(), tagger);
		TagAccuracyEvaluationJob evaluationJob = new TagAccuracyEvaluationJobMT(memm, new ViterbiDecoder("viterbi", tagger), threads);
		acc.evaluationJobIs(evaluationJob);
		EstimationThread estimation = new EstimationThread(tagger);
		Estimator.Notifiee lbfgs2Notifiee = new Estimator.Notifiee() {
			int round = 1;
			@Override
			public void onDataSet(Corpus data) {
				Calendar cal = Calendar.getInstance();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				System.out.println("[" + sdf.format(cal.getTime()) + "][" + "lbfgsNotifiee" + "]\tonDataSet");
				memm.weightsVectorIs(lbfgs2.featureVector());
				FeatureVector eWeights = memm.featureVectorNew();
				FeatureVector tWeights = memm.featureVectorNew();
				for (Feature weight: memm.parameters().features()) {
					if (memm.function().feature(eTemplate, weight.index()) != null)
						eWeights.entryIs(weight.index(), weight);
					else if (memm.function().feature(tTemplate, weight.index()) != null)
						tWeights.entryIs(weight.index(), weight);
				}
				eMemm.weightsVectorIs(eWeights);
				tMemm.weightsVectorIs(tWeights);
				try {
					//ModelPrinter.printConditionalProbability(memm, new File("output/" + tagger.name() + "_" + memm.name() + "_prob" + "_" + round + ".txt"));
					ModelPrinter.printModel(memm, new File("output/" + tagger.name() + "_" + hmm.name() + "_" + this.round + ".txt"));
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				acc.dataSetIs(testCorpus);
				this.round++;
				tagger.estimatorIs(data, mle);
				estimation.dataSetIs(data);
			}
		};
		Estimator.Notifiee mleNotifiee = new Estimator.Notifiee() {
			@Override
			public void onDataSet(Corpus data) {
				Calendar cal = Calendar.getInstance();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				System.out.println("[" + sdf.format(cal.getTime()) + "][" + "mleNotifiee" + "]: log likelihood value = " + mle.logLikelihoodFunction().value());
				tagger.estimatorIs(data, lbfgs2);
				estimation.dataSetIs(data);
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
		lbfgs2.notifieeIs(lbfgs2Notifiee, lbfgs2Notifiee);
		mle.notifieeIs(mleNotifiee, mleNotifiee);
		tagger.notifieeIs(taggerNotifiee, taggerNotifiee);
		tagger.estimatorIs(trainCorpus, mle);
		estimation.dataSetIs(trainCorpus);
		estimation.start();
	}
}