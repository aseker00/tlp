package aseker00.tlp.mt.impl.estimation;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import aseker00.tlp.estimation.Lbfgs;
import aseker00.tlp.estimation.LogLinearModelLogLikelihood;
import aseker00.tlp.io.Corpus;
import aseker00.tlp.io.CorpusReader;
import aseker00.tlp.io.Tweet;
import aseker00.tlp.ling.Element;
import aseker00.tlp.model.LabeledSequence;
import aseker00.tlp.model.LabeledSequenceImpl;
import aseker00.tlp.model.disc.FeatureVector;
import aseker00.tlp.model.disc.MaximumEntropyMarkovModel;
import aseker00.tlp.pos.Tagger;
import riso.numerical.LBFGS;
import riso.numerical.LBFGS.ExceptionWithIflag;

public class LbfgsMT extends Lbfgs {

	private BlockingQueue<FunctionThread> queue;
	private int threads;

	public LbfgsMT(String name, Tagger tagger, int threads) {
		super(name, tagger);
		this.threads = threads;
		this.queue = new LinkedBlockingQueue<FunctionThread>();
	}

	class NullSequence extends LabeledSequenceImpl {
		public NullSequence() {
			super(new ArrayList<Element>());
		}
	}

	class FunctionThread extends Thread {
		LogLinearModelLogLikelihood logLikelihood;
		BlockingQueue<LabeledSequence> sequences;
		MaximumEntropyMarkovModel memm;

		FunctionThread(String name, MaximumEntropyMarkovModel memm) {
			super(name);
			this.memm = memm;
			this.logLikelihood = memm.logLikelihoodNew();
			this.sequences = new LinkedBlockingQueue<LabeledSequence>();
		}

		LogLinearModelLogLikelihood logLikelihoodFunction() {
			return this.logLikelihood;
		}

		@Override
		public void run() {
			while (true) {
				try {
					LabeledSequence sequence = this.sequences.take();
					if (sequence instanceof NullSequence)
						break;
					LogLinearModelLogLikelihood step = memm.logLikelihoodStep(sequence, LbfgsMT.this);
					this.logLikelihood.additionInc(step);
					LbfgsMT.this.onFunctionThread(this);
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
			FunctionThread thread = new FunctionThread(this.model().name() + "-" + i, this.model());
			thread.start();
			this.queue.add(thread);
		}
		CorpusReader stream = data.readerNew(data.name() + "::" + this.name());
		this.logLikelihood = this.model().logLikelihoodNew();
		Tweet tweet;
		while ((tweet = stream.next()) != null) {
			ArrayList<Element> entries = new ArrayList<Element>();
			ArrayList<Element> labels = new ArrayList<Element>();
			for (int i = 0; i < tweet.tokens(); i++) {
				entries.add(tweet.token(i));
				labels.add(tweet.tag(i));
			}
			LabeledSequence sequence = this.model().sequenceNew(entries, labels);
			try {
				FunctionThread thread = this.queue.take();
				thread.sequences.offer(sequence);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		NullSequence nullSequence = new NullSequence();
		for (int i = 0; i < this.threads; i++) {
			try {
				FunctionThread thread = this.queue.take();
				thread.sequences.offer(nullSequence);
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