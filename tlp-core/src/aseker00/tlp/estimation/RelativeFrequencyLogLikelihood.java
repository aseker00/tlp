package aseker00.tlp.estimation;

import java.util.Set;

import aseker00.tlp.ling.Element;
import aseker00.tlp.model.Counts;

public class RelativeFrequencyLogLikelihood implements LogLikelihoodFunction {
	private double value;
	private Counts emissionCounts;
	private Counts transitionCounts;
	private Counts labelCounts;

	public RelativeFrequencyLogLikelihood() {
		this.emissionCounts = new Counts();
		this.transitionCounts = new Counts();
		this.labelCounts = new Counts();
	}

	@Override
	public double value() {
		return this.value;
	}

	public void valueInc(double value) {
		if (Double.isNaN(this.value + value))
			return;
		if (Double.isInfinite(this.value + value))
			return;
		this.value += value;
	}

	public LogLikelihoodFunction additionInc(LogLikelihoodFunction other) {
		RelativeFrequencyLogLikelihood rfllf = (RelativeFrequencyLogLikelihood) other;
		this.valueInc(rfllf.value);
		this.emissionCounts.valuesInc(rfllf.emissionCounts);
		this.transitionCounts.valuesInc(rfllf.transitionCounts);
		this.labelCounts.valuesInc(rfllf.labelCounts);
		return this;
	}

	public Set<Element> emissions() {
		return this.emissionCounts.elements();
	}

	public Set<Element> transitions() {
		return this.transitionCounts.elements();
	}

	public Set<Element> labels() {
		return this.labelCounts.elements();
	}

	public double emissionCount(Element emission) {
		return this.emissionCounts.value(emission);
	}

	public void emissionCountInc(Element emission) {
		this.emissionCountInc(emission, 1.0);
	}

	public void emissionCountInc(Element emission, double value) {
		this.emissionCounts.valueInc(emission, value);
	}

	public double transitionCount(Element transition) {
		return this.transitionCounts.value(transition);
	}

	public void transitionCountInc(Element transition) {
		this.transitionCountInc(transition, 1.0);
	}

	public void transitionCountInc(Element transition, double value) {
		this.transitionCounts.valueInc(transition, value);
	}

	public double labelCount(Element label) {
		return this.labelCounts.value(label);
	}

	public void labelCountInc(Element label) {
		this.labelCountInc(label, 1.0);
	}

	public void labelCountInc(Element label, double value) {
		this.labelCounts.valueInc(label, value);
	}
}