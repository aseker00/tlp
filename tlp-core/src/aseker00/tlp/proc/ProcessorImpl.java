package aseker00.tlp.proc;

import java.util.HashMap;

import aseker00.tlp.io.Corpus;

public abstract class ProcessorImpl implements Processor {
	String name;
	protected Model model;
	protected Decoder decoder;
	HashMap<Corpus, Estimator> estimators;
	HashMap<Corpus, Evaluator> evaluators;
	HashMap<Notifiee, Notifiee> notifiees;
	
	public ProcessorImpl(String name) {
		this.name = name;
		this.notifiees = new HashMap<Notifiee, Notifiee>();
		this.estimators = new HashMap<Corpus, Estimator>();
		this.evaluators = new HashMap<Corpus, Evaluator>();
	}
	@Override
	public String name() {
		return this.name;
	}
	@Override
	public void modelIs(Model model) {
		this.model = model;
		for (Notifiee key: this.notifiees.keySet()) {
			Notifiee notifiee = this.notifiees.get(key);
			if (notifiee == null)
				continue;
			notifiee.onModel();
		}
	}
	@Override
	public Model model() {
		return this.model;
	}
	@Override
	public void decoderIs(Decoder decoder) {
		this.decoder = decoder;
	}
	@Override
	public void estimatorIs(Corpus data, Estimator estimator) {
		if (estimator == null)
			this.estimators.remove(data);
		else
			this.estimators.put(data, estimator);
	}
	@Override
	public Estimator estimator(Corpus data) {
		return this.estimators.get(data);
	}
	@Override
	public void evaluatorIs(Corpus data, Evaluator evaluator) {
		if (evaluator == null)
			this.evaluators.remove(data);
		else
			this.evaluators.put(data, evaluator);
	}
	@Override
	public Evaluator evaluator(Corpus data) {
		return this.evaluators.get(data);
	}
	@Override
	public void notifieeIs(Notifiee key, Notifiee value) {
		if (value == null) {
			this.notifiees.remove(key);
			return;
		}
		this.notifiees.put(key, value);
		
	}
}