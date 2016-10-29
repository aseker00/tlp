package aseker00.tlp.twitter.pos;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.cli.CommandLine;

import aseker00.tlp.ark.io.ArkCorpus;
import aseker00.tlp.ark.pos.ArkHmmwfTagger;
import aseker00.tlp.ark.pos.ArkSpecialPatternTokens;
import aseker00.tlp.ark.pos.model.ModelLoader;
import aseker00.tlp.ark.pos.model.ModelPrinter;
import aseker00.tlp.decoding.ViterbiDecoder;
import aseker00.tlp.estimation.BaumWelch;
import aseker00.tlp.estimation.Lbfgs;
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
import aseker00.tlp.model.gen.HiddenMarkovModel;
import aseker00.tlp.mt.impl.estimation.BaumWelchWithFeaturesMT;
import aseker00.tlp.mt.impl.estimation.BaumWelchMT;
import aseker00.tlp.mt.impl.estimation.LbfgsWithFeaturesMT;
import aseker00.tlp.mt.impl.evaluation.TagAccuracyEvaluationJobMT;
import aseker00.tlp.pos.TagSet;
import aseker00.tlp.proc.EstimationThread;
import aseker00.tlp.proc.Estimator;
import aseker00.tlp.proc.Processor;
import aseker00.tlp.twitter.kafka.KafkaCorpus;

public class TwitterHmmwfTagger2 extends ArkHmmwfTagger {

	public TwitterHmmwfTagger2(String name) {
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
		int stepSize = cmd.hasOption("step") ? Integer.parseInt(cmd.getOptionValue("step")) : 10;
		TwitterHmmwfTagger2 tagger = new TwitterHmmwfTagger2("ark");
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
		String[] openClassTagLabels = { "^", "N", "V", "!", "S", "Z", "M", "A", "R", "G" };
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
		ArkCorpus arkTestCorpus = new ArkCorpus("oct27.test", new File(base, "data/oct27.test"), tagger);
		ArkCorpus arkTrainCorpus = new ArkCorpus("oct27.traindev", new File(base, "data/oct27.traindev"), tagger);
		KafkaCorpus kafkaTrainCorpus = new KafkaCorpus("stream00", size, "twitter", size * 0, tagger);
		FeatureFunction ff = new FeatureFunction();
		MaximumEntropyMarkovModel memm = new MaximumEntropyMarkovModel("memm", tagger, chain, chain, ff);
		FeatureTemplate eTemplate = new EmissionFeatureTemplate(memm);
		FeatureTemplate tTemplate = new TransitionFeatureTemplate(memm);
		memm.featureFunctionTemplateIs(eTemplate, eTemplate);
		memm.featureFunctionTemplateIs(tTemplate, tTemplate);
		ModelLoader.loadModel(memm, new File("../tlp-ark/output/" + tagger.name() + "_model.txt"));

		HashSet<Ngram> eContext = new HashSet<Ngram>();
		HashSet<Ngram> tContext = new HashSet<Ngram>();
		HashSet<Element> eDecisions = new HashSet<Element>();
		HashSet<Element> tDecisions = new HashSet<Element>();
		for (Tag tag: tagger.tagSet())
			tDecisions.add(tag);
		tDecisions.add(tagger.stopTag());
		CorpusReader stream = arkTrainCorpus.readerNew(arkTrainCorpus.name() + "::" + tagger.name());
		Tweet tweet;
		while ((tweet = stream.next()) != null) {
			ArrayList<Element> entries = new ArrayList<Element>();
			for (int i = 0; i < tweet.tokens(); i++)
				entries.add(tweet.token(i));
			LabeledSequence sequence = new LabeledSequenceImpl(entries);
			for (int i = 0; i < sequence.length(); i++) {
				Set<Ngram> transitions = memm.transitions(sequence, i);
				for (Ngram transition : transitions) {
					sequence.labelTransitionIs(i, transition);
					Ngram emission = memm.emission(sequence, i);
					Ngram tCondition = transition.subgram(0, transition.size()-1);
					Ngram eCondition = emission.subgram(0, emission.size()-1);
					tContext.add(tCondition);
					eContext.add(eCondition);
					tDecisions.add(sequence.label(i));
					eDecisions.add(sequence.entry(i));
				}
			}
			Set<Ngram> transitions = memm.transitions(sequence, sequence.length());
			for (Ngram transition : transitions) {
				sequence.labelTransitionIs(sequence.length(), transition);
				Ngram tCondition = transition.subgram(0, transition.size()-1);
				tContext.add(tCondition);
			}
		}
		stream = kafkaTrainCorpus.readerNew(kafkaTrainCorpus.name() + "::" + tagger.name());
		while ((tweet = stream.next()) != null) {
			ArrayList<Element> entries = new ArrayList<Element>();
			for (int j = 0; j < tweet.tokens(); j++)
				entries.add(tweet.token(j));
			LabeledSequence sequence = new LabeledSequenceImpl(entries);
			for (int i = 0; i <= sequence.length(); i++) {
				Set<Ngram> transitions = memm.transitions(sequence, i);
				for (Ngram transition : transitions) {
					sequence.labelTransitionIs(i, transition);
					FeatureVector fv = memm.featureVector(sequence, i);
					for (Feature feature: fv.features()) {
						if (memm.parameters().entryValue(feature.index()) != 0.0)
							continue;
						Feature weight = new Feature(ff, feature.index());
						double value = 0.0;
						if (tagger.openClassTagSet().contains((Tag)sequence.label(i)))
							value = ThreadLocalRandom.current().nextDouble(0.01, 0.1);
						else if (tagger.closedClassTagSet().contains((Tag)sequence.label(i)))
							value = ThreadLocalRandom.current().nextDouble(0.001, 0.01);
						else if (tagger.specialTagSet().contains((Tag)sequence.label(i)))
							value = ThreadLocalRandom.current().nextDouble(0.001, 0.01);
						weight.valueIs(value);
						memm.parameters().entryIs(weight.index(), weight);
					}
				}
			}
		}
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
		for (Feature weight: memm.parameters().features()) {
			if (memm.function().feature(eTemplate, weight.index()) != null)
				eMemm.parameters().entryIs(weight.index(), weight);
			else if (memm.function().feature(tTemplate, weight.index()) != null)
				tMemm.parameters().entryIs(weight.index(), weight);
		}
		for (Ngram c: tContext) {
			tMemm.eventSetIs(c, new HashSet<Element>());
			for (Element d: tDecisions) {
				Ngram transition = tMemm.transition(c, d);
				tMemm.events(c).add(transition.entry(transition.size()-1));
			}
		}
		for (Ngram c: eContext) {
			eMemm.eventSetIs(c, new HashSet<Element>());
			for (Element d: eDecisions) {
				Ngram emission = eMemm.emission(c, d);
				eMemm.events(c).add(emission.entry(emission.size()-1));
			}
		}
		HiddenMarkovModel hmm = new HiddenMarkovModel("hmmwf", tagger, chain, chain);
		hmm.emissionProbabilityDistributionIs(eMemm);
		hmm.transitionProbabilityDistributionIs(tMemm);
		tagger.modelIs(hmm);
		try {
			ModelPrinter.printModel(eMemm, new File("output/" + tagger.name() + "_" + eMemm.name() + "_" + 0 + ".txt"));
			ModelPrinter.printModel(tMemm, new File("output/" + tagger.name() + "_" + tMemm.name() + "_" + 0 + ".txt"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		TagAccuracy acc = new TagAccuracy(tagger.model().name(), tagger);
		TagAccuracyEvaluationJob evaluationJob = new TagAccuracyEvaluationJobMT(hmm, new ViterbiDecoder("viterbi", tagger), threads);
		acc.evaluationJobIs(evaluationJob);
		BaumWelch bw = new BaumWelchMT("bw", tagger, threads);
		BaumWelch bw2 = new BaumWelchWithFeaturesMT("bw2", tagger, bw, threads);
		bw2.modelIs(hmm);
		Lbfgs lbfgs2 = new LbfgsWithFeaturesMT("lbfgs", tagger, bw2, threads);
		lbfgs2.modelIs(memm);
		L2Regularization regularization = new L2Regularization();
		regularization.lambdaIs(0.1);
		lbfgs2.featureRegularizationIs(regularization);
		EstimationThread estimation = new EstimationThread(tagger);
		Estimator.Notifiee lbfgs2Notifiee = new Estimator.Notifiee() {
			int round = 1;
			KafkaCorpus trainCorpus = kafkaTrainCorpus;
			@Override
			public void onDataSet(Corpus data) {
				Calendar cal = Calendar.getInstance();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				System.out.println("[" + sdf.format(cal.getTime()) + "][" + "lbfgsNotifiee" + "]\tround " + this.round + ": onDataSet");
				memm.weightsVectorIs(lbfgs2.featureVector());
				try {
					ModelPrinter.printModel(memm, new File("output/" + tagger.name() + "_" + memm.name() + "_" + this.round + ".txt"));
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
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
				acc.dataSetIs(arkTestCorpus);
				if (this.round % stepSize == 0) {
					List<Feature> features = memm.function().features();
					for (Feature feature : features) {
						if (feature.index() > memm.parameters().dimension())
							memm.function().featureIs(feature.index(), null);
					}
					this.trainCorpus = new KafkaCorpus("stream" + (this.round/stepSize), size, "twitter", (this.round/stepSize)*size, tagger);
					CorpusReader stream = this.trainCorpus.readerNew(this.trainCorpus.name() + "::" + tagger.name());
					Tweet tweet;
					while ((tweet = stream.next()) != null) {
						ArrayList<Element> entries = new ArrayList<Element>();
						for (int j = 0; j < tweet.tokens(); j++)
							entries.add(tweet.token(j));
						LabeledSequence sequence = new LabeledSequenceImpl(entries);
						for (int i = 0; i <= sequence.length(); i++) {
							Set<Ngram> transitions = memm.transitions(sequence, i);
							for (Ngram transition : transitions) {
								sequence.labelTransitionIs(i, transition);
								FeatureVector fv = memm.featureVector(sequence, i);
								for (Feature feature: fv.features()) {
									if (memm.parameters().entryValue(feature.index()) != 0.0)
										continue;
									Feature weight = new Feature(ff, feature.index());
									double value = 0.0;
									if (tagger.openClassTagSet().contains((Tag)sequence.label(i)))
										value = ThreadLocalRandom.current().nextDouble(0.01, 0.1);
									else if (tagger.closedClassTagSet().contains((Tag)sequence.label(i)))
										value = ThreadLocalRandom.current().nextDouble(0.001, 0.01);
									else if (tagger.specialTagSet().contains((Tag)sequence.label(i)))
										value = ThreadLocalRandom.current().nextDouble(0.001, 0.01);
									weight.valueIs((value));
									memm.parameters().entryIs(weight.index(), weight);
								}
							}
						}
					}	
					eWeights = memm.featureVectorNew();
					tWeights = memm.featureVectorNew();
					for (Feature weight: memm.parameters().features()) {
						if (memm.function().feature(eTemplate, weight.index()) != null)
							eWeights.entryIs(weight.index(), weight);
						else if (memm.function().feature(tTemplate, weight.index()) != null)
							tWeights.entryIs(weight.index(), weight);
					}
					eMemm.weightsVectorIs(eWeights);
					tMemm.weightsVectorIs(tWeights);
					tagger.estimatorIs(this.trainCorpus, bw2);
					estimation.dataSetIs(this.trainCorpus);
				}
				else {
					estimation.dataSetIs(data);
				}
				this.round++;
			}
		};
		Estimator.Notifiee bw2Notifiee = new Estimator.Notifiee() {
			int round = 1;
			@Override
			public void onDataSet(Corpus data) {
				Calendar cal = Calendar.getInstance();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				System.out.println("[" + sdf.format(cal.getTime()) + "][" + "bw2Notifiee" + "]\tround " + this.round
						+ ": log likelihood value = " + bw2.logLikelihoodFunction().value());
				this.round++;
				lbfgs2.iflagIs(-1);
				
				CorpusReader stream = arkTrainCorpus.readerNew(arkTrainCorpus.name() + "::" + tagger.name());
				Tweet tweet;
				while ((tweet = stream.next()) != null) {
					ArrayList<Element> entries = new ArrayList<Element>();
					ArrayList<Element> labels = new ArrayList<Element>();
					for (int j = 0; j < tweet.tokens(); j++) {
						entries.add(tweet.token(j));
						labels.add(tweet.tag(j));
					}
					LabeledSequence sequence = new LabeledSequenceImpl(entries, labels);
					for (int i = 0; i < sequence.length(); i++) {
						Ngram transition = hmm.transition(sequence, i);
						Ngram emission = hmm.emission(sequence, i);
						bw2.logLikelihoodFunction().transitionCountInc(transition);
						bw2.logLikelihoodFunction().emissionCountInc(emission);
					}
					Ngram transition = hmm.transition(sequence, sequence.length());
					bw2.logLikelihoodFunction().transitionCountInc(transition);
				}
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
		bw2.notifieeIs(bw2Notifiee, bw2Notifiee);
		tagger.notifieeIs(taggerNotifiee, taggerNotifiee);
		tagger.estimatorIs(kafkaTrainCorpus, bw2);
		estimation.dataSetIs(kafkaTrainCorpus);
		estimation.start();
	}
}