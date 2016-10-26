package aseker00.tlp.mt.impl.estimation;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import aseker00.tlp.estimation.BaumWelch;
import aseker00.tlp.estimation.RelativeFrequencyLogLikelihood;
import aseker00.tlp.io.Corpus;
import aseker00.tlp.io.CorpusReader;
import aseker00.tlp.io.Tweet;
import aseker00.tlp.ling.Element;
import aseker00.tlp.model.LabeledSequence;
import aseker00.tlp.model.LabeledSequenceImpl;
import aseker00.tlp.model.gen.HiddenMarkovModel;
import aseker00.tlp.pos.Tagger;

public class BaumWelchMT extends BaumWelch {
	private BlockingQueue<FunctionThread> queue;
	protected int threads;

	public BaumWelchMT(String name, Tagger tagger, int threads) {
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
		RelativeFrequencyLogLikelihood logLikelihood;
		BlockingQueue<LabeledSequence> sequences;
		HiddenMarkovModel hmm;

		FunctionThread(String name, HiddenMarkovModel hmm) {
			super(name);
			this.hmm = hmm;
			this.logLikelihood = hmm.logLikelihoodNew();
			this.sequences = new LinkedBlockingQueue<LabeledSequence>();
		}

		RelativeFrequencyLogLikelihood logLikelihoodFunction() {
			return this.logLikelihood;
		}

		@Override
		public void run() {
			while (true) {
				try {
					LabeledSequence sequence = this.sequences.take();
					if (sequence instanceof NullSequence)
						break;
					RelativeFrequencyLogLikelihood step = this.hmm.logLikelihoodStep(sequence,
							BaumWelchMT.this);
					this.logLikelihood.additionInc(step);
					BaumWelchMT.this.onFunctionThread(this);
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
		for (Notifiee key : this.notifiees.keySet()) {
			Notifiee notifiee = this.notifiees.get(key);
			notifiee.onDataSet(data);
		}
	}

	private void onFunctionThread(FunctionThread thread) {
		this.queue.offer(thread);
	}
}