package aseker00.tlp.model.disc;

import java.util.ArrayList;
import java.util.List;

import aseker00.tlp.ling.Element;
import aseker00.tlp.model.LabeledSequenceImpl;

public class FeatureLabeledSequenceImpl extends LabeledSequenceImpl {
	private List<Double> norms;

	public FeatureLabeledSequenceImpl(List<Element> elements) {
		super(elements);
		this.norms = new ArrayList<Double>(this.length());
		for (int i = 0; i <= this.length(); i++)
			this.norms.add(0.0);
	}

	public FeatureLabeledSequenceImpl(List<Element> elements, List<Element> labels) {
		super(elements, labels);
		this.norms = new ArrayList<Double>(this.length());
		for (int i = 0; i <= this.length(); i++)
			this.norms.add(0.0);
	}

	public void normIs(int pos, double value) {
		this.norms.set(pos, value);
	}

	public double norm(int pos) {
		return this.norms.get(pos);
	}
}