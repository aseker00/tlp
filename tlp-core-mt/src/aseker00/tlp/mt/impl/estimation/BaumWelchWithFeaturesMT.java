package aseker00.tlp.mt.impl.estimation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import aseker00.tlp.estimation.BaumWelchWithFeatures;
import aseker00.tlp.estimation.MaximumLikelihoodEstimation;
import aseker00.tlp.estimation.RelativeFrequencyLogLikelihood;
import aseker00.tlp.io.Corpus;
import aseker00.tlp.io.CorpusReader;
import aseker00.tlp.io.Tweet;
import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
import aseker00.tlp.model.LabeledSequence;
import aseker00.tlp.model.LabeledSequenceImpl;
import aseker00.tlp.model.disc.FeatureVector;
import aseker00.tlp.model.disc.MaximumEntropyMarkovModel;
import aseker00.tlp.model.gen.HiddenMarkovModel;
import aseker00.tlp.pos.Tagger;

public class BaumWelchWithFeaturesMT extends BaumWelchWithFeatures {
	private BlockingQueue<PreFunctionThread> preQueue;
	private BlockingQueue<NormFunctionThread> normQueue;
	private int threads;
	private MaximumLikelihoodEstimation mle;

	public BaumWelchWithFeaturesMT(String name, Tagger tagger, MaximumLikelihoodEstimation mle, int threads) {
		super(name, tagger);
		this.threads = threads;
		this.mle = mle;
		this.preQueue = new LinkedBlockingQueue<PreFunctionThread>();
		this.normQueue = new LinkedBlockingQueue<NormFunctionThread>();
	}
	
	@Override
	public void modelIs(HiddenMarkovModel hmm) {
		this.mle.modelIs(hmm);
	}
	
	@Override
	public HiddenMarkovModel model() {
		return this.mle.model();
	}
	
	@Override
	public RelativeFrequencyLogLikelihood logLikelihoodFunction() {
		return this.mle.logLikelihoodFunction();
	}
	
	@Override
	public void notifieeIs(Notifiee key, Notifiee value) {
		this.mle.notifieeIs(key, value);
	}

	class NullSequence extends LabeledSequenceImpl {
		public NullSequence() {
			super(new ArrayList<Element>());
		}
	}
	
	class NullElement implements Element {
		@Override
		public String type() {
			return null;
		}
	}

	class PreFunctionThread extends Thread {
		HashSet<Element> decisions;
		BlockingQueue<LabeledSequence> sequences;
		HiddenMarkovModel hmm;

		PreFunctionThread(String name, HiddenMarkovModel hmm) {
			super(name);
			this.hmm = hmm;
			this.decisions = new HashSet<Element>();
			this.sequences = new LinkedBlockingQueue<LabeledSequence>();
		}

		 HashSet<Element> decisions() {
			return this.decisions;
		}

		@Override
		public void run() {
			while (true) {
				try {
					LabeledSequence sequence = this.sequences.take();
					if (sequence instanceof NullSequence)
						break;
					this.hmm.logLikelihoodPreStep(sequence, BaumWelchWithFeaturesMT.this, decisions);
					BaumWelchWithFeaturesMT.this.onPreFunctionThread(this);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	class NormFunctionThread extends Thread {
		BlockingQueue<Element> conditions;
		MaximumEntropyMarkovModel eMemm;

		NormFunctionThread(String name, HiddenMarkovModel hmm) {
			super(name);
			this.eMemm = (MaximumEntropyMarkovModel)hmm.emissionProbabilityDistribution();
			this.conditions = new LinkedBlockingQueue<Element>();
		}

		@Override
		public void run() {
			while (true) {
				try {
					Element c = this.conditions.take();
					if (c instanceof NullElement)
						break;
					double norm = 0.0;
					for (Element d: this.eMemm.events(c)) {
						Ngram emission = eMemm.emission((Ngram)c, d);
						FeatureVector fv = eMemm.featureVector(null, emission);
						double value = Math.exp(eMemm.parameters().dotProduct(fv));
						norm += value;
					}
					eMemm.normIs(c, norm);
					BaumWelchWithFeaturesMT.this.onNormFunctionThread(this);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public synchronized void dataSetIs(Corpus data) {
		for (int i = 0; i < this.threads; i++) {
			PreFunctionThread thread = new PreFunctionThread(this.model().name() + "-" + i, this.model());
			thread.start();
			this.preQueue.add(thread);
		}
		CorpusReader stream = data.readerNew(data.name() + "::" + this.name());
		this.logLikelihood = this.model().logLikelihoodNew();
		Tweet tweet;
		while ((tweet = stream.next()) != null) {
			ArrayList<Element> entries = new ArrayList<Element>();
			for (int i = 0; i < tweet.tokens(); i++)
				entries.add(tweet.token(i));
			LabeledSequence sequence = this.model().sequenceNew(entries);
			try {
				PreFunctionThread thread = this.preQueue.take();
				thread.sequences.offer(sequence);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		HashSet<Element> eDecisions = new HashSet<Element>();
		NullSequence nullSequence = new NullSequence();
		for (int i = 0; i < this.threads; i++) {
			try {
				PreFunctionThread thread = this.preQueue.take();
				thread.sequences.offer(nullSequence);
				thread.join();
				eDecisions.addAll(thread.decisions());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		MaximumEntropyMarkovModel eMemm = (MaximumEntropyMarkovModel)this.model().emissionProbabilityDistribution();
		MaximumEntropyMarkovModel tMemm = (MaximumEntropyMarkovModel)this.model().transitionProbabilityDistribution();
		//for (Element c: eMemm.conditions())
		//	eMemm.events(c).addAll(eDecisions);
		for (Element c: eMemm.conditions()) {
			for (Element d: eDecisions) {
				Ngram emission = eMemm.emission((Ngram)c, d);
				eMemm.events(c).add(emission.entry(emission.size()-1));
			}
		}
		for (Element c: tMemm.conditions()) {
			double norm = 0.0;
			for (Element d: tMemm.events(c)) {
				Ngram transition = tMemm.transition((Ngram)c, d);
				FeatureVector fv = tMemm.featureVector(transition, null);
				double value = Math.exp(tMemm.parameters().dotProduct(fv));
				norm += value;
			}
			tMemm.normIs(c, norm);
		}
		for (int i = 0; i < this.threads; i++) {
			NormFunctionThread thread = new NormFunctionThread(this.model().name() + "-" + i, this.model());
			thread.start();
			this.normQueue.add(thread);
		}
		for (Element c: eMemm.conditions()) {
			try {
				NormFunctionThread thread = this.normQueue.take();
				thread.conditions.offer(c);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		NullElement nullElement = new NullElement();
		for (int i = 0; i < this.threads; i++) {
			try {
				NormFunctionThread thread = this.normQueue.take();
				thread.conditions.offer(nullElement);
				thread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.mle.dataSetIs(data);
	}

	private void onPreFunctionThread(PreFunctionThread thread) {
		this.preQueue.offer(thread);
	}
	
	private void onNormFunctionThread(NormFunctionThread thread) {
		this.normQueue.offer(thread);
	}
}