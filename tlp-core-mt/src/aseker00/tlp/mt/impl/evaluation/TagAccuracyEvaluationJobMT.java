package aseker00.tlp.mt.impl.evaluation;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import aseker00.tlp.decoding.ViterbiDecoder;
import aseker00.tlp.evaluation.TagAccuracyEvaluationJob;
import aseker00.tlp.io.Corpus;
import aseker00.tlp.io.CorpusReader;
import aseker00.tlp.io.Tweet;
import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Tag;
import aseker00.tlp.ling.Token;
import aseker00.tlp.model.LinearChainSequenceLabelModel;

public class TagAccuracyEvaluationJobMT extends TagAccuracyEvaluationJob {
	private BlockingQueue<EvaluationStepThread> queue;
	private int threads;
	private Object monitor;

	class NullTweet implements Tweet {
		@Override
		public int tokens() {
			return 0;
		}

		@Override
		public Token token(int index) {
			return null;
		}

		@Override
		public Tag tag(int index) {
			return null;
		}
	}

	class EvaluationStepThread extends Thread {
		BlockingQueue<Tweet> tweets;
		HashMap<Element, Integer> annotations;
		HashMap<Element, Integer> predictions;
		HashMap<Element, Integer> matched;
		HashMap<Element, HashMap<Element, Integer>> unmatchedAnnotationPredictions;
		HashMap<Element, HashMap<Element, Integer>> unmatchedPredictionAnnotations;

		EvaluationStepThread(String name, HashMap<Element, Integer> annotations, HashMap<Element, Integer> predictions,
				HashMap<Element, Integer> matched,
				HashMap<Element, HashMap<Element, Integer>> unmatchedAnnotationPredictions,
				HashMap<Element, HashMap<Element, Integer>> unmatchedPredictionAnnotations) {
			super(name);
			this.tweets = new LinkedBlockingQueue<Tweet>();
			this.annotations = annotations;
			this.predictions = predictions;
			this.matched = matched;
			this.unmatchedAnnotationPredictions = unmatchedAnnotationPredictions;
			this.unmatchedPredictionAnnotations = unmatchedPredictionAnnotations;
		}

		@Override
		public void run() {
			while (true) {
				try {
					Tweet tweet = this.tweets.take();
					if (tweet instanceof NullTweet)
						break;
					StepUpdate update = TagAccuracyEvaluationJobMT.this.stepIs(tweet);
					TagAccuracyEvaluationJobMT.this.onStep(this);
					synchronized (TagAccuracyEvaluationJobMT.this.monitor) {
						TagAccuracyEvaluationJobMT.this.update(this.annotations, this.predictions, this.matched,
								this.unmatchedAnnotationPredictions, this.unmatchedPredictionAnnotations, update);
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public TagAccuracyEvaluationJobMT(LinearChainSequenceLabelModel model, ViterbiDecoder decoder, int threads) {
		super(model, decoder);
		this.threads = threads;
		this.queue = new LinkedBlockingQueue<EvaluationStepThread>();
		this.monitor = new Object();
	}

	@Override
	protected void dataSetIs(Corpus data) {
		HashMap<Element, Integer> annotations = new HashMap<Element, Integer>();
		HashMap<Element, Integer> predictions = new HashMap<Element, Integer>();
		HashMap<Element, Integer> matched = new HashMap<Element, Integer>();
		HashMap<Element, HashMap<Element, Integer>> unmatchedAnnotationPredictions = new HashMap<Element, HashMap<Element, Integer>>();
		HashMap<Element, HashMap<Element, Integer>> unmatchedPredictionAnnotations = new HashMap<Element, HashMap<Element, Integer>>();
		for (int i = 0; i < threads; i++) {
			EvaluationStepThread thread = new EvaluationStepThread(this.decoder.name() + "-" + i, annotations,
					predictions, matched, unmatchedAnnotationPredictions, unmatchedPredictionAnnotations);
			thread.start();
			this.queue.add(thread);
		}
		CorpusReader stream = data.readerNew(null);
		Tweet tweet;
		while ((tweet = stream.next()) != null) {
			try {
				EvaluationStepThread thread = this.queue.take();
				thread.tweets.offer(tweet);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		NullTweet nullTweet = new NullTweet();
		for (int i = 0; i < threads; i++) {
			try {
				EvaluationStepThread thread = this.queue.take();
				thread.tweets.offer(nullTweet);
				thread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.annotations = annotations;
		this.predictions = predictions;
		this.matched = matched;
		this.unmatchedAnnotationPredictions = unmatchedAnnotationPredictions;
		this.unmatchedPredictionAnnotations = unmatchedPredictionAnnotations;
	}

	void onStep(EvaluationStepThread thread) {
		this.queue.offer(thread);
	}
}
