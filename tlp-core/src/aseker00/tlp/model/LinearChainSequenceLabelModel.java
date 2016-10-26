package aseker00.tlp.model;

import java.util.List;
import java.util.Set;

import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
import aseker00.tlp.proc.Model;

public interface LinearChainSequenceLabelModel extends Model {
	// linear chain attributes
	public int chain();
	public int eChain();
		
	// sequence model visitor design pattern attributes
	public double potential(LabeledSequence sequence, int pos);
	public double logPotential(LabeledSequence sequence, int pos);
	public double decodedPotential(Viterbi viterbi, LabeledSequence sequence, int pos);
	public double conditionalLabelProbability(LabeledSequence sequence);
	public double conditionalLabelLogProbability(LabeledSequence sequence);
	public Set<Ngram> transitions(LabeledSequence sequence, int pos);
	public Ngram transition(LabeledSequence sequence, int pos);
	public Ngram emission(LabeledSequence sequence, int pos);
	public Ngram transition(String str);
	public Ngram emission(String str);
	public Ngram transition(Ngram condition, Element event);
	public Ngram emission(Ngram condition, Element event);
	public Set<Ngram> transitions();
	
	public LabeledSequence sequenceNew(List<Element> entries);
	public LabeledSequence sequenceNew(List<Element> entries, List<Element> labels);
}