package aseker00.tlp.evaluation;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import aseker00.tlp.io.Corpus;
import aseker00.tlp.ling.Element;
import aseker00.tlp.proc.Evaluator;
import aseker00.tlp.proc.Processor;

public class TagAccuracy implements Evaluator {
	private String name;
	private Processor processor;
	private double value;
	private HashMap<Notifiee, Notifiee> notifiees;
	private TagAccuracyEvaluationJob job;
	
	public TagAccuracy(String name, Processor processor) {
		this.name = name;
		this.processor = processor;
		this.notifiees = new HashMap<Notifiee, Notifiee>();
	}
	
	public void evaluationJobIs(TagAccuracyEvaluationJob job) {
		this.job = job;
	}
	
	@Override
	public String name() {
		return this.name;
	}
	
	@Override
	public Processor processor() {
		return this.processor;
	}
	
	@Override
	public synchronized void dataSetIs(Corpus data) {
		if (data == null)
			return;
		this.job.dataSetIs(data);
		int total = job.total();
		int match = job.match();
		Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		this.value = ((double)match)/total;
		HashMap<Element, Integer> annotations = this.job.annotations();
		HashMap<Element, Integer> predictions = this.job.predictions();
		HashMap<Element, Integer> matches = this.job.matches();
		for (Element e: annotations.keySet()) {
			int annotationValue = annotations.get(e);
			int predictionValue = predictions.getOrDefault(e, 0);
			int matchValue = matches.getOrDefault(e, 0);
			System.out.printf("[" + sdf.format(cal.getTime()) + "][" + this.getClass().getSimpleName() + "]\t" + e + ": %d/%d precision = %.2f, %d/%d recall = %.2f\n", matchValue, predictionValue, ((double)matchValue)/predictionValue, matchValue, annotationValue, ((double)matchValue)/annotationValue);
		}
		System.out.printf("[" + sdf.format(cal.getTime()) + "][" + this.getClass().getSimpleName() + "]\tvalue: " + this.processor.model().name() + " %d/%d correct = %.2f acc, %.2f err\n", match, total, ((double)match)/total, ((double)(total-match))/total);
		for (Notifiee key: this.notifiees.keySet()) {
			Notifiee notifiee = this.notifiees.get(key);
			notifiee.onDataSet(data);
		}
	}
	
	public void notifieeIs(Notifiee key, Notifiee value) {
		if (value == null) {
			this.notifiees.remove(key);
			return;
		}
		this.notifiees.put(key, value);
	}
	
	public double value() {
		return this.value;
	}
}