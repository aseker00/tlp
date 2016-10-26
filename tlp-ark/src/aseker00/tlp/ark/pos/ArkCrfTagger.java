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
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang.math.NumberUtils;

import aseker00.tlp.ark.io.ArkCorpus;
import aseker00.tlp.ark.pos.model.ModelPrinter;
import aseker00.tlp.decoding.ViterbiDecoder;
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
import aseker00.tlp.model.disc.ConditionalRandomField;
import aseker00.tlp.model.disc.EmissionFeatureTemplate;
import aseker00.tlp.model.disc.Feature;
import aseker00.tlp.model.disc.FeatureFunction;
import aseker00.tlp.model.disc.FeatureTemplate;
import aseker00.tlp.model.disc.FeatureVector;
import aseker00.tlp.model.disc.L2Regularization;
import aseker00.tlp.model.disc.TransitionFeatureTemplate;
import aseker00.tlp.mt.impl.estimation.LbfgsMT;
import aseker00.tlp.mt.impl.evaluation.TagAccuracyEvaluationJobMT;
import aseker00.tlp.pos.SpecialToken;
import aseker00.tlp.pos.TagSet;
import aseker00.tlp.pos.Tagger;
import aseker00.tlp.proc.Estimator;
import aseker00.tlp.proc.EstimationThread;
import aseker00.tlp.proc.Processor;

public class ArkCrfTagger extends Tagger {

	private HashMap<Pattern, Token> specialPatternTokens;
	private Token numberToken;

	public ArkCrfTagger(String name) {
		super(name);
		this.specialPatternTokens = new HashMap<Pattern, Token>();
	}

	public void specialPatternTokenIs(Pattern p, Token t) {
		this.specialPatternTokens.put(p, t);
	}

	public void numberTokenIs(Token t) {
		this.numberToken = t;
	}

	@Override
	public Token token(String str) {
		for (Pattern p : this.specialPatternTokens.keySet()) {
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
		ArkCrfTagger tagger = new ArkCrfTagger("ark");
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
		ArkCorpus testCorpus = new ArkCorpus("oct27.test", new File(base, "data/oct27.test"), tagger);
		ArkCorpus trainCorpus = new ArkCorpus("oct27.traindev", new File(base, "data/oct27.traindev"), tagger);
		FeatureFunction ff = new FeatureFunction();
		ConditionalRandomField crf = new ConditionalRandomField("crf", tagger, chain, chain, ff);
		FeatureTemplate eTemplate = new EmissionFeatureTemplate(crf);
		FeatureTemplate tTemplate = new TransitionFeatureTemplate(crf);
		crf.featureFunctionTemplateIs(eTemplate, eTemplate);
		crf.featureFunctionTemplateIs(tTemplate, tTemplate);
		FeatureVector weights = crf.featureVectorNew();
		CorpusReader stream = trainCorpus.readerNew(trainCorpus.name() + "::" + tagger.name());
		Tweet tweet;
		while ((tweet = stream.next()) != null) {
			ArrayList<Element> entries = new ArrayList<Element>();
			for (int i = 0; i < tweet.tokens(); i++)
				entries.add(tweet.token(i));
			LabeledSequence sequence = new LabeledSequenceImpl(entries);
			for (int i = 0; i <= sequence.length(); i++) {
				Set<Ngram> transitions = crf.transitions(sequence, i);
				for (Ngram transition : transitions) {
					sequence.labelTransitionIs(i, transition);
					crf.featureVector(sequence, i);
				}
			}
		}
		List<Feature> features = crf.function().features();
		for (Feature feature : features) {
			double value = 1.0 - Math.random();
			Feature weight = new Feature(ff, feature.index());
			weight.valueIs(value);
			weights.entryIs(weight.index(), weight);
		}
		crf.weightsVectorIs(weights);
		tagger.modelIs(crf);

		Lbfgs lbfgs = new LbfgsMT("lbfgs", tagger, threads);
		lbfgs.modelIs(crf);
		L2Regularization regularization = new L2Regularization();
		regularization.lambdaIs(0.1);
		lbfgs.featureRegularizationIs(regularization);

		TagAccuracy acc = new TagAccuracy(tagger.model().name(), tagger);
		TagAccuracyEvaluationJob evaluationJob = new TagAccuracyEvaluationJobMT(crf, new ViterbiDecoder("viterbi", tagger), threads);
		acc.evaluationJobIs(evaluationJob);

		EstimationThread estimation = new EstimationThread(tagger);
		Estimator.Notifiee lbfgsNotifiee = new Estimator.Notifiee() {
			int round = 1;
			@Override
			public void onDataSet(Corpus data) {
				Calendar cal = Calendar.getInstance();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				System.out.println("[" + sdf.format(cal.getTime()) + "][" + "lbfgsNotifiee" + "]\tonDataSet");
				FeatureVector modelParams = lbfgs.featureVector();
				crf.weightsVectorIs(modelParams);
				try {
					//ModelPrinter.printConditionalProbability(crf, new File("output/" + tagger.name() + "_" + crf.name() + "_prob_" + round + ".txt"));
					ModelPrinter.printModel(crf, new File("output/" + tagger.name() + "_" + crf.name() + "_" + this.round + ".txt"));
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				acc.dataSetIs(testCorpus);
				this.round++;
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
		lbfgs.notifieeIs(lbfgsNotifiee, lbfgsNotifiee);
		tagger.notifieeIs(taggerNotifiee, taggerNotifiee);
		tagger.estimatorIs(trainCorpus, lbfgs);
		tagger.modelIs(crf);
		estimation.dataSetIs(trainCorpus);
		estimation.start();
	}
}