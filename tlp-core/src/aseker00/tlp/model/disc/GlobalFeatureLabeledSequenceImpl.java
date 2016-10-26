package aseker00.tlp.model.disc;

import java.util.List;

import aseker00.tlp.ling.Element;

public class GlobalFeatureLabeledSequenceImpl extends FeatureLabeledSequenceImpl {
	private double norm;

	public GlobalFeatureLabeledSequenceImpl(List<Element> elements) {
		super(elements);
	}

	public GlobalFeatureLabeledSequenceImpl(List<Element> elements, List<Element> labels) {
		super(elements, labels);
	}

	public void normIs(double value) {
		this.norm = value;
	}

	public double norm() {
		return this.norm;
	}
}
