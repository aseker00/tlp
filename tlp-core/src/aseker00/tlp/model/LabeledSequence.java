package aseker00.tlp.model;

import java.util.List;

import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
import aseker00.tlp.model.disc.ConditionalRandomField;
import aseker00.tlp.model.disc.MaximumEntropyMarkovModel;
import aseker00.tlp.model.gen.HiddenMarkovModel;
import aseker00.tlp.proc.Sequence;

public interface LabeledSequence extends Sequence {
	public Element label(int pos);
	public void labelIs(int pos, Element label);
	public List<Element> labelList();
	public void labelListIs(List<Element> labels);
	public void labelTransitionIs(int pos, Ngram transition);
	
	public double potential(HiddenMarkovModel hmm, int pos);
	public double logPotential(HiddenMarkovModel hmm, int pos);
	public double potential(MaximumEntropyMarkovModel memm, int pos);
	public double logPotential(MaximumEntropyMarkovModel memm, int pos);
	public double potential(ConditionalRandomField crf, int pos);
	public double logPotential(ConditionalRandomField crf, int pos);
	
	public double conditionalLabelProbability(HiddenMarkovModel hmm);
	public double conditionalLabelLogProbability(HiddenMarkovModel hmm);
	public double conditionalLabelProbability(MaximumEntropyMarkovModel memm);
	public double conditionalLabelLogProbability(MaximumEntropyMarkovModel memm);
	public double conditionalLabelProbability(ConditionalRandomField crf);
	public double conditionalLabelLogProbability(ConditionalRandomField crf);
}