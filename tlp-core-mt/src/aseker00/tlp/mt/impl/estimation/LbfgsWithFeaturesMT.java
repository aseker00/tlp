package aseker00.tlp.mt.impl.estimation;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import aseker00.tlp.estimation.LbfgsWithFeatures;
import aseker00.tlp.estimation.LogLinearModelLogLikelihood;
import aseker00.tlp.estimation.MaximumLikelihoodEstimation;
import aseker00.tlp.io.Corpus;
import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
import aseker00.tlp.model.disc.FeatureVector;
import aseker00.tlp.model.disc.MaximumEntropyMarkovModel;
import aseker00.tlp.model.gen.HiddenMarkovModel;
import aseker00.tlp.pos.Tagger;
import riso.numerical.LBFGS;
import riso.numerical.LBFGS.ExceptionWithIflag;

public class LbfgsWithFeaturesMT extends LbfgsWithFeatures {
	private BlockingQueue<FunctionThread> queue;
	private int threads;

	public LbfgsWithFeaturesMT(String name, Tagger tagger, MaximumLikelihoodEstimation mle, int threads) {
		super(name, tagger, mle);
		this.threads = threads;
		this.queue = new LinkedBlockingQueue<FunctionThread>();
	}

	class NullElement implements Element {
		@Override
		public String type() {
			return null;
		}
	}

	class FunctionThread extends Thread {
		LogLinearModelLogLikelihood logLikelihood;
		BlockingQueue<Element> conditions;
		MaximumEntropyMarkovModel eMemm;

		FunctionThread(String name, HiddenMarkovModel hmm) {
			super(name);
			this.eMemm = (MaximumEntropyMarkovModel)hmm.emissionProbabilityDistribution();
			this.logLikelihood = memm.logLikelihoodNew();
			this.conditions = new LinkedBlockingQueue<Element>();
		}

		LogLinearModelLogLikelihood logLikelihoodFunction() {
			return this.logLikelihood;
		}

		@Override
		public void run() {
			while (true) {
				try {
					Element c = this.conditions.take();
					if (c instanceof NullElement)
						break;
					HashMap<Element, Double> probabilities = new HashMap<Element, Double>();
					HashMap<Element, Double> expectedCounts = new HashMap<Element, Double>();
					for (Element d: this.eMemm.events(c)) {
						double p = this.eMemm.probability(c, d);
						probabilities.put(d, p);
						Ngram emission = eMemm.emission((Ngram)c, d);
						double e = LbfgsWithFeaturesMT.this.mle.logLikelihoodFunction().emissionCount(emission);
						expectedCounts.put(d, e);
						this.logLikelihood.valueInc(e * Math.log(p));
						FeatureVector fv = this.eMemm.featureVector(null, emission, e);
						this.logLikelihood.sumFeaturesInc(fv);
					}
					for (Element d: this.eMemm.events(c)) {
						double e = expectedCounts.get(d);
						for (Element d2: this.eMemm.events(c)) {
							double p2 = probabilities.get(d2);
							Ngram emission2 = eMemm.emission((Ngram)c, d2);
							FeatureVector fv2 = this.eMemm.featureVector(null, emission2, p2*e);
							this.logLikelihood.sumExpectedFeaturesInc(fv2);
						}
					}
					LbfgsWithFeaturesMT.this.onFunctionThread(this);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public synchronized void dataSetIs(Corpus data) {
		HiddenMarkovModel hmm = this.mle.model();
		MaximumEntropyMarkovModel tMemm = (MaximumEntropyMarkovModel)hmm.transitionProbabilityDistribution();
		MaximumEntropyMarkovModel eMemm = (MaximumEntropyMarkovModel)hmm.emissionProbabilityDistribution();
		this.logLikelihood = this.model().logLikelihoodNew();
		for (Element c: tMemm.conditions()) {
			for (Element d: tMemm.events(c)) {
				double p = tMemm.probability(c, d);
				Ngram transition = tMemm.transition((Ngram)c, d);
				double e = this.mle.logLikelihoodFunction().transitionCount(transition);
				this.logLikelihood.valueInc(e * Math.log(p));
				FeatureVector fv = tMemm.featureVector(transition, null, e);
				this.logLikelihood.sumFeaturesInc(fv);
				for (Element d2: tMemm.events(c)) {
					double p2 = tMemm.probability(c, d2);
					Ngram transition2 = tMemm.transition((Ngram)c, d2);
					FeatureVector fv2 = tMemm.featureVector(transition2, null, p2*e);
					this.logLikelihood.sumExpectedFeaturesInc(fv2);
				}
			}
		}
		for (int i = 0; i < this.threads; i++) {
			FunctionThread thread = new FunctionThread(eMemm.name() + "-" + i, hmm);
			thread.start();
			this.queue.add(thread);
		}
		for (Element c: eMemm.conditions()) {
			try {
				FunctionThread thread = this.queue.take();
				thread.conditions.offer(c);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		NullElement nullElement = new NullElement();
		for (int i = 0; i < this.threads; i++) {
			try {
				FunctionThread thread = this.queue.take();
				thread.conditions.offer(nullElement);
				thread.join();
				this.logLikelihood.additionInc(thread.logLikelihoodFunction());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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

	private void onFunctionThread(FunctionThread thread) {
		this.queue.offer(thread);
	}
}