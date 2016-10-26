package aseker00.tlp.estimation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import aseker00.tlp.io.Corpus;
import aseker00.tlp.io.CorpusReader;
import aseker00.tlp.io.Tweet;
import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
import aseker00.tlp.model.ForwardBackward;
import aseker00.tlp.model.LabeledSequence;
import aseker00.tlp.model.disc.ConditionalRandomField;
import aseker00.tlp.model.disc.Feature;
import aseker00.tlp.model.disc.FeatureLabeledSequenceImpl;
import aseker00.tlp.model.disc.FeatureRegularization;
import aseker00.tlp.model.disc.FeatureVector;
import aseker00.tlp.model.disc.GlobalFeatureLabeledSequenceImpl;
import aseker00.tlp.model.disc.MaximumEntropyMarkovModel;
import aseker00.tlp.pos.Tagger;
import riso.numerical.LBFGS;
import riso.numerical.LBFGS.ExceptionWithIflag;

public class Lbfgs extends MaximumLikelihood {
	protected int n;
	protected double x[];
	protected double g[];
	protected double diag[];
	protected int m;
	protected boolean diagco;
	protected int iprint[];
	protected double eps;
	protected double xtol;
	protected int iflag[];

	protected MaximumEntropyMarkovModel memm;
	protected FeatureRegularization regularization;
	protected LogLinearModelLogLikelihood logLikelihood;

	public Lbfgs(String name, Tagger tagger) {
		super(name, tagger);
		this.m = 5;
		this.diagco = false;
		this.iprint = new int[] { 1, 0 };
		this.eps = 0.01;
		this.xtol = 2e-16;
		this.iflag = new int[] { -1 };
	}

	public void modelIs(MaximumEntropyMarkovModel memm) {
		this.memm = memm;
	}

	public MaximumEntropyMarkovModel model() {
		return this.memm;
	}

	public LogLinearModelLogLikelihood logLikelihood() {
		return this.logLikelihood;
	}

	public boolean isConverged() {
		return this.iflag[0] == 0;
	}

	public boolean isFailed() {
		return this.iflag[0] < 0;
	}
	
	public void iflagIs(int flag) {
		this.iflag[0] = flag;
	}

	/*
	 * More formally, from a Bayesian standpoint the regularization term can be
	 * viewed as log p(w) where p(w) is a prior (specifically, p(w) is a
	 * Gaussian prior): the parameter estimates w are then MAP (Maximum A
	 * Priori) estimates. From a frequentist standpoint there have been a number
	 * of important results showing that finding parameters with a low norm
	 * (|w|^2) leads to better generalization guarantees
	 */
	public void featureRegularizationIs(FeatureRegularization regularization) {
		this.regularization = regularization;
	}

	@Override
	public synchronized void dataSetIs(Corpus data) {
		this.logLikelihood = this.model().logLikelihoodNew();
		CorpusReader stream = data.readerNew(data.name() + "::" + this.name());
		Tweet tweet;
		while ((tweet = stream.next()) != null) {
			ArrayList<Element> entries = new ArrayList<Element>();
			ArrayList<Element> labels = new ArrayList<Element>();
			for (int i = 0; i < tweet.tokens(); i++) {
				entries.add(tweet.token(i));
				labels.add(tweet.tag(i));
			}
			LabeledSequence sequence = this.model().sequenceNew(entries, labels);
			LogLinearModelLogLikelihood step = this.model().logLikelihoodStep(sequence, this);
			this.logLikelihood.additionInc(step);
		}
		if (this.iflag[0] < 0) {
			this.iflag[0] = 0;
			this.n = this.model().parameters().dimension();
			this.x = new double[n];
			for (int i = 1; i <= this.n; i++)
				this.x[i - 1] = this.model().parameters().entryValue(i);
			this.g = new double[n];
			this.diag = new double[n];
		}
		double f = this.regularization != null
				? this.regularization.featureFunctionValue(this.model(), this.logLikelihood)
				: this.logLikelihood.value();
		FeatureVector gradientVector = this.regularization != null
				? this.regularization.featureFunctionGradient(this.model(), this.logLikelihood)
				: this.logLikelihood.sumFeatures()
						.subtraction(this.logLikelihood.sumExpectedFeatures());
		for (int i = 1; i <= this.n; i++)
			this.g[i - 1] = gradientVector.entryValue(i) * -1;
		try {
			LBFGS.lbfgs(n, m, x, f * -1, g, diagco, diag, iprint, eps, xtol, iflag);
		} catch (ExceptionWithIflag e) {
			throw new RuntimeException(e);
		}
		for (Notifiee key : this.notifiees.keySet()) {
			Notifiee notifiee = this.notifiees.get(key);
			notifiee.onDataSet(data);
		}
	}

	public FeatureVector featureVector() {
		FeatureVector parameters = this.model().featureVectorNew();
		for (int i = 0; i < this.n; i++) {
			if (this.x[i] == 0)
				continue;
			Feature feature = this.model().function().feature(i+1);
			feature.valueIs(this.x[i]);
			parameters.entryIs(i + 1, feature);
		}
		return parameters;
	}

	public LogLinearModelLogLikelihood logLikelihoodStep(LabeledSequence sequence, MaximumEntropyMarkovModel memm) {
		LogLinearModelLogLikelihood llmllf = (LogLinearModelLogLikelihood) memm.logLikelihoodNew();
		List<Element> labels = new ArrayList<Element>(sequence.labelList());
		for (int i = 0; i <= sequence.length(); i++) {
			double norm = 0.0;
			Set<Ngram> transitions = memm.transitions(sequence, i);
			for (Ngram transition : transitions) {
				sequence.labelTransitionIs(i, transition);
				FeatureVector fv = memm.featureVector(sequence, i);
				double p = Math.exp(memm.parameters().dotProduct(fv));
				norm += p;
			}
			sequence.labelListIs(labels);
			((FeatureLabeledSequenceImpl) sequence).normIs(i, norm);
			FeatureVector fv = memm.featureVector(sequence, i);
			llmllf.sumFeaturesInc(fv);
			double p = memm.conditionalLogProbability(sequence, i);
			llmllf.valueInc(p);
			for (Ngram transition : transitions) {
				sequence.labelTransitionIs(i, transition);
				p = memm.potential(sequence, i);
				FeatureVector potentialFeatureVector = memm.featureVector(sequence, i, p);
				llmllf.sumExpectedFeaturesInc(potentialFeatureVector);
			}
		}
		sequence.labelListIs(labels);
		return llmllf;
	}

	public LogLinearModelLogLikelihood logLikelihoodStep(LabeledSequence sequence, ConditionalRandomField crf) {
		LogLinearModelLogLikelihood llmllf = (LogLinearModelLogLikelihood) crf.logLikelihoodNew();
		ForwardBackward fb = new ForwardBackward(sequence);
		fb.modelIs(crf);
		((GlobalFeatureLabeledSequenceImpl) sequence).normIs(fb.value());
		llmllf.valueInc(crf.conditionalLabelLogProbability(sequence));
		llmllf.sumFeaturesInc(crf.globalFeatureVector(sequence));
		List<Element> labels = new ArrayList<Element>(sequence.labelList());
		for (int i = 0; i < sequence.length(); i++) {
			Set<Ngram> transitions = crf.transitions(sequence, i);
			for (Ngram transition : transitions) {
				sequence.labelTransitionIs(i, transition);
				FeatureVector features;
				if (i == 0) {
					Ngram suffix = transition.subgram(1);
					double labelEmissionJointProb = fb.labelValue(i, suffix);
					double labelEmissionConditionalProb = labelEmissionJointProb / fb.value();
					features = crf.featureVector(sequence, i, labelEmissionConditionalProb);
				} else {
					double transitionEmissionJointProb = fb.transitionValue(i - 1, transition);
					double transitionEmissionConditionalProb = transitionEmissionJointProb / fb.value();
					features = crf.featureVector(sequence, i, transitionEmissionConditionalProb);
				}
				llmllf.sumExpectedFeaturesInc(features);
			}
		}
		Set<Ngram> transitions = crf.transitions(sequence, sequence.length());
		for (Ngram transition : transitions) {
			sequence.labelTransitionIs(sequence.length(), transition);
			Ngram prefix = transition.subgram(0, transition.size() - 1);
			double labelEmissionJointProb = fb.labelValue(sequence.length() - 1, prefix);
			double labelEmissionConditionalProb = labelEmissionJointProb / fb.value();
			FeatureVector features = crf.featureVector(sequence, sequence.length(), labelEmissionConditionalProb);
			llmllf.sumExpectedFeaturesInc(features);
		}
		sequence.labelListIs(labels);
		return llmllf;
	}
}