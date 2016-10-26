package aseker00.tlp.evaluation;

import java.util.HashMap;

import aseker00.tlp.decoding.ViterbiDecoder;
import aseker00.tlp.io.Corpus;
import aseker00.tlp.io.CorpusReader;
import aseker00.tlp.io.Tweet;
import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Tag;
import aseker00.tlp.model.LinearChainSequenceLabelModel;

public class TagAccuracyEvaluationJob {
	public class StepUpdate {
		HashMap<Element, Integer> annotations = new HashMap<Element, Integer>();  
		HashMap<Element, Integer> predictions = new HashMap<Element, Integer>();
		HashMap<Element, Integer> matched = new HashMap<Element, Integer>();
		HashMap<Element, HashMap<Element, Integer>> unmatchedAnnotationPredictions = new HashMap<Element, HashMap<Element, Integer>>();
		HashMap<Element, HashMap<Element, Integer>> unmatchedPredictionAnnotations = new HashMap<Element, HashMap<Element, Integer>>();
	}
	protected HashMap<Element, Integer> annotations;
	protected HashMap<Element, Integer> predictions;
	protected HashMap<Element, Integer> matched;
	protected HashMap<Element, HashMap<Element, Integer>> unmatchedAnnotationPredictions;
	protected HashMap<Element, HashMap<Element, Integer>> unmatchedPredictionAnnotations;
	protected ViterbiDecoder decoder;
	protected LinearChainSequenceLabelModel model;

	public TagAccuracyEvaluationJob(LinearChainSequenceLabelModel model, ViterbiDecoder decoder) {
		this.model = model;
		this.decoder = decoder;
	}
	
	public ViterbiDecoder decoder() {
		return this.decoder;
	}
	
	protected void dataSetIs(Corpus data) {
		HashMap<Element, Integer> annotations = new HashMap<Element, Integer>();
		HashMap<Element, Integer> predictions = new HashMap<Element, Integer>();
		HashMap<Element, Integer> matched = new HashMap<Element, Integer>();
		HashMap<Element, HashMap<Element, Integer>> unmatchedAnnotationPredictions = new HashMap<Element, HashMap<Element, Integer>>();
		HashMap<Element, HashMap<Element, Integer>> unmatchedPredictionAnnotations = new HashMap<Element, HashMap<Element, Integer>>();
		CorpusReader stream = data.readerNew(data.name());
		Tweet tweet;
		while ((tweet = stream.next()) != null) {
			StepUpdate update = this.stepIs(tweet);
			this.update(annotations, predictions, matched, unmatchedAnnotationPredictions, unmatchedPredictionAnnotations, update);
		}
		this.annotations = annotations;
		this.predictions = predictions;
		this.matched = matched;
		this.unmatchedAnnotationPredictions = unmatchedAnnotationPredictions;
		this.unmatchedPredictionAnnotations = unmatchedPredictionAnnotations;
	}
	int total() {
		int sum = 0;
		for (int value: this.annotations.values())
			sum += value;
		return sum;
	}
	int match() {
		int sum = 0;
		for (int value: this.matched.values())
			sum += value;
		return sum;
	}
	HashMap<Element, Integer> annotations() {
		return this.annotations;
	}
	HashMap<Element, Integer> predictions() {
		return this.predictions;
	}
	HashMap<Element, Integer> matches() {
		return this.matched;
	}
	protected StepUpdate stepIs(Tweet annotatedTweet) {
		StepUpdate update = new StepUpdate();
		Tweet decodedTweet = this.decoder.tweet(this.model, annotatedTweet);
		//System.out.println("ann: " + annotatedTweet);
		//System.out.println("dec: " + decodedTweet);
		for (int j = 0; j < annotatedTweet.tokens(); j++) {
			Tag annotation = annotatedTweet.tag(j);
			Tag prediction = decodedTweet.tag(j);
			Integer cur = update.annotations.putIfAbsent(annotation, 1);
			if (cur != null)
				update.annotations.put(annotation, cur+1);
			cur = update.predictions.putIfAbsent(prediction, 1);
			if (cur != null)
				update.predictions.put(prediction, cur+1);
			if (annotation.equals(prediction)) {
				cur = update.matched.putIfAbsent(prediction, 1);
				if (cur != null)
					update.matched.put(prediction, cur+1);
			}
			else {
				HashMap<Element, Integer> unmatchedAnnotations = update.unmatchedPredictionAnnotations.get(prediction);
				if (unmatchedAnnotations == null)
					unmatchedAnnotations = new HashMap<Element, Integer>();
				cur = unmatchedAnnotations.putIfAbsent(annotation, 1);
				if (cur != null)
					unmatchedAnnotations.put(annotation, cur+1);
				HashMap<Element, Integer> unmatchedPredictions = update.unmatchedAnnotationPredictions.get(annotation);
				if (unmatchedPredictions == null)
					unmatchedPredictions = new HashMap<Element, Integer>();
				cur = unmatchedPredictions.putIfAbsent(prediction, 1);
				if (cur != null)
					unmatchedPredictions.put(prediction, cur+1);
			}
		}
		return update;
	}
	protected void update(HashMap<Element, Integer> annotations,
			HashMap<Element, Integer> predictions,
			HashMap<Element, Integer> matched,
			HashMap<Element, HashMap<Element, Integer>> unmatchedAnnotationPredictions,
			HashMap<Element, HashMap<Element, Integer>> unmatchedPredictionAnnotations,
			StepUpdate update) {
		for (Element annotation: update.annotations.keySet()) {
			int curValue = annotations.getOrDefault(annotation, 0);
			annotations.put(annotation, curValue+update.annotations.get(annotation));
		}
		for (Element prediction: update.predictions.keySet()) {
			int curValue = predictions.getOrDefault(prediction, 0);
			predictions.put(prediction, curValue+update.predictions.get(prediction));
		}
		for (Element match: update.matched.keySet()) {
			int curValue = matched.getOrDefault(match, 0);
			matched.put(match, curValue+update.matched.get(match));
		}
		for (Element unmatchedAnnotation: update.unmatchedAnnotationPredictions.keySet()) {
			HashMap<Element, Integer> unmatchedPredictions = update.unmatchedAnnotationPredictions.get(unmatchedAnnotation);
			HashMap<Element, Integer> curPredictions = unmatchedAnnotationPredictions.get(unmatchedAnnotation);
			if (curPredictions == null)
				curPredictions = new HashMap<Element, Integer>();
			for (Element unmatchedPrediction: unmatchedPredictions.keySet()) {
				int curValue = unmatchedPredictions.getOrDefault(unmatchedPrediction, 0);
				curPredictions.put(unmatchedPrediction, curValue+unmatchedPredictions.get(unmatchedPrediction));
			}
		}
		for (Element unmatchedPrediction: update.unmatchedPredictionAnnotations.keySet()) {
			HashMap<Element, Integer> unmatchedAnnotations = update.unmatchedPredictionAnnotations.get(unmatchedPrediction);
			HashMap<Element, Integer> curAnnotations = unmatchedPredictionAnnotations.get(unmatchedPrediction);
			if (curAnnotations == null)
				curAnnotations = new HashMap<Element, Integer>();
			for (Element unmatchedAnnotation: unmatchedAnnotations.keySet()) {
				int curValue = unmatchedAnnotations.getOrDefault(unmatchedAnnotation, 0);
				curAnnotations.put(unmatchedAnnotation, curValue+unmatchedAnnotations.get(unmatchedAnnotation));
			}
		}
	}
}