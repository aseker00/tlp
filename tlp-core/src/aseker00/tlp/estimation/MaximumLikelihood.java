package aseker00.tlp.estimation;

import java.util.HashMap;

import aseker00.tlp.proc.Estimator;
import aseker00.tlp.proc.Processor;

public abstract class MaximumLikelihood implements Estimator {
	private String name;
	private Processor processor;
	protected HashMap<Notifiee, Notifiee> notifiees;
	
	public MaximumLikelihood(String name, Processor processor) {
		this.name = name;
		this.processor = processor;
		this.notifiees = new HashMap<Notifiee, Notifiee>();
	}
	
	@Override
	public String name() {
		return name;
	}
	
	@Override
	public Processor processor() {
		return this.processor;
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