package aseker00.tlp.decoding;

import java.util.ArrayList;
import java.util.Set;

import aseker00.tlp.io.Tweet;
import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
import aseker00.tlp.ling.Tag;
import aseker00.tlp.ling.Token;
import aseker00.tlp.model.LabeledSequence;
import aseker00.tlp.model.LinearChainSequenceLabelModel;
import aseker00.tlp.model.Viterbi;
import aseker00.tlp.model.disc.FeatureLabeledSequenceImpl;
import aseker00.tlp.model.disc.FeatureVector;
import aseker00.tlp.model.disc.MaximumEntropyMarkovModel;
import aseker00.tlp.pos.Tagger;
import aseker00.tlp.proc.Decoder;
import aseker00.tlp.proc.Model;
import aseker00.tlp.proc.Processor;

public class ViterbiDecoder implements Decoder {
	private String name;
	private Tagger tagger;

	public ViterbiDecoder(String name, Tagger tagger) {
		this.name = name;
		this.tagger = tagger;
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public Processor processor() {
		return this.tagger;
	}

	@Override
	public Tweet tweet(Model model, Tweet input) {
		return this.tweet((LinearChainSequenceLabelModel) model, input);
	}

	public Tweet tweet(LinearChainSequenceLabelModel model, Tweet input) {
		ArrayList<Element> entries = new ArrayList<Element>();
		for (int i = 0; i < input.tokens(); i++)
			entries.add(input.token(i));
		LabeledSequence sequence = model.sequenceNew(entries);
		return this.tweet(model, sequence);
	}

	private Tweet tweet(LinearChainSequenceLabelModel model, LabeledSequence sequence) {
		if (model instanceof MaximumEntropyMarkovModel) {
			MaximumEntropyMarkovModel memm = (MaximumEntropyMarkovModel) model;
			for (int i = 0; i <= sequence.length(); i++) {
				double value = 0.0;
				Set<Ngram> transitions = memm.transitions(sequence, i);
				for (Ngram transition : transitions) {
					sequence.labelTransitionIs(i, transition);
					FeatureVector features = memm.featureVector(sequence, i);
					double p = Math.exp(memm.parameters().dotProduct(features));
					value += p;
				}
				((FeatureLabeledSequenceImpl) sequence).normIs(i, value);
			}
		}
		Viterbi viterbi = new Viterbi(sequence);
		viterbi.modelIs(model);
		ArrayList<Element> labels = viterbi.maxLabels(model);
		Tweet decodedTweet = new Tweet() {
			@Override
			public int tokens() {
				return sequence.length();
			}

			@Override
			public Token token(int index) {
				return (Token) sequence.entry(index);
			}

			@Override
			public Tag tag(int index) {
				return (Tag) labels.get(index);
			}

			@Override
			public String toString() {
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < this.tokens(); i++) {
					sb.append(this.tag(i)).append("/").append(this.token(i));
					if (i < this.tokens())
						sb.append(", ");
				}
				return sb.toString();
			}
		};
		return decodedTweet;
	}
}