package aseker00.tlp.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import aseker00.tlp.ling.Element;
import aseker00.tlp.ling.Ngram;
import aseker00.tlp.ling.Tag;
import aseker00.tlp.ling.Token;
import aseker00.tlp.pos.SpecialToken;
import aseker00.tlp.pos.TagSet;
import aseker00.tlp.pos.Tagger;

public abstract class LinearChainSequenceLabelModelImpl implements LinearChainSequenceLabelModel {

	private String name;
	private int tChain;
	private int eChain;
	protected Tagger tagger;

	public LinearChainSequenceLabelModelImpl(String name, Tagger tagger, int tChain, int eChain) {
		this.name = name;
		this.tChain = tChain;
		this.eChain = eChain;
		this.tagger = tagger;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public int chain() {
		return this.tChain;
	}

	@Override
	public int eChain() {
		return this.eChain;
	}

	@Override
	public double decodedPotential(Viterbi viterbi, LabeledSequence sequence, int pos) {
		double p = this.potential(sequence, pos);
		Ngram transition = this.transition(sequence, pos);
		Ngram prefix = transition.subgram(0, this.chain() - 1);
		double value = viterbi.chartValue(pos, prefix);
		return value * p;
	}
	
	@Override
	public Set<Ngram> transitions() {
		Set<Ngram> result = new HashSet<Ngram>();
		ArrayList<Tag> tagList = new ArrayList<Tag>();
		for (Tag tag: tagger.tagSet())
			tagList.add(tag);
		int total = (int)Math.pow(tagList.size(), this.chain());
		for (int i = 0; i < total; i++) {
			String baseRep = Integer.toString(i, tagList.size());
			while (baseRep.length() < this.chain())
				baseRep = "0" + baseRep;
			Element[] elements = new Element[this.chain()];
			for (int j = 0; j < elements.length; j++) {
				int index = Integer.parseInt(baseRep.substring(j, j+1), tagList.size());
				Tag tag = tagList.get(index);
				elements[j] = tag;
			}
			Ngram transition = new Ngram(elements);
			result.add(transition);
		}
		//tagList.add(tagger.startTag());
		total = (int)Math.pow(tagList.size(), this.chain()-1);
		for (int i = 0; i < total; i++) {
			String baseRep = Integer.toString(i, tagList.size());
			while (baseRep.length() < this.chain()-1)
				baseRep = "0" + baseRep;
			Element[] elements = new Element[this.chain()];
			elements[0] = tagger.startTag();
			for (int j = 1; j < elements.length; j++) {
				int index = Integer.parseInt(baseRep.substring(j-1, j), tagList.size());
				Tag tag = tagList.get(index);
				elements[j] = tag;
			}
			Ngram transition = new Ngram(elements);
			result.add(transition);
		}
		return result;
	}

	@Override
	public Set<Ngram> transitions(LabeledSequence sequence, int pos) {
		Set<Ngram> result = new HashSet<Ngram>();
		List<Set<Tag>> positionTags = new ArrayList<Set<Tag>>();
		for (int i = 0; i < this.chain(); i++) {
			Set<Tag> tags = new HashSet<Tag>();
			if (pos - (this.chain() - 1) + i < 0) {
				tags.add(this.tagger.startTag());
			} else if (pos - (this.chain() - 1) + i == sequence.length()) {
				tags.add(this.tagger.stopTag());
			} else {
				TagSet ts = tagger.tags((Token) sequence.entry(pos-(this.chain()-1)+i));
				for (Tag tag : ts)
					tags.add(tag);
			}
			positionTags.add(tags);
		}
		Element[] elements = new Element[this.chain()];
		buildTransitions(0, positionTags, elements, result);
		return result;
	}

	@Override
	public Ngram transition(LabeledSequence sequence, int pos) {
		Element[] elements = new Element[this.chain()];
		for (int i = 0; i < this.chain(); i++) {
			if (pos - (this.chain() - 1) + i < 0)
				elements[i] = this.tagger.startTag();
			else if (pos - (this.chain() - 1) + i == sequence.length())
				elements[i] = this.tagger.stopTag();
			else
				elements[i] = sequence.label(pos - (this.chain()-1) + i);
		}
		return new Ngram(elements);
	}

	@Override
	public Ngram emission(LabeledSequence sequence, int pos) {
		Element[] elements = new Element[this.eChain];
		for (int i = 0; i < elements.length - 1; i++) {
			if (pos - (elements.length - 2) + i < 0)
				elements[i] = this.tagger.startTag();
			else if (i < elements.length - 1)
				elements[i] = sequence.label(pos - (elements.length-2) + i);
		}
		Element token = sequence.entry(pos);
		if (token instanceof SpecialToken) {
			if (tagger.specialTagSet().contains((Tag) elements[elements.length-2]))
				token = ((SpecialToken) token).normalized();
		}
		elements[elements.length - 1] = token;
		return new Ngram(elements);
	}
	
	@Override
	public Ngram transition(String str) {
		String[] strElements = str.substring(1, str.length()-1).split(", ");
		Element[] elements = new Element[strElements.length];
		for (int i = 0; i < strElements.length; i++) {
			Tag tag = tagger.tag(strElements[i]);
			elements[i] = tag;
		}
		Ngram transition = new Ngram(elements);
		return transition;
	}
	
	@Override
	public Ngram emission(String str) {
		String[] strElements = str.substring(1, str.length()-1).split(", ");
		Element[] elements = new Element[strElements.length];
		for (int i = 0; i < strElements.length-1; i++) {
			Tag tag = tagger.tag(strElements[i]);
			elements[i] = tag;
		}
		Token token = tagger.token(strElements[strElements.length-1]);
		if (token instanceof SpecialToken) {
			if (tagger.specialTagSet().contains((Tag) elements[elements.length-2]))
				token = ((SpecialToken) token).normalized();
		}
		elements[elements.length-1] = token;
		Ngram emission = new Ngram(elements);
		return emission;
	}
	
	@Override
	public Ngram transition(Ngram condition, Element event) {
		Tag tag = (Tag)event;
		return condition.add(tag);
	}
	
	@Override
	public Ngram emission(Ngram condition, Element event) {
		Token token = (Token)event;
		if (token instanceof SpecialToken) {
			if (tagger.specialTagSet().contains((Tag) condition.entry(condition.size()-1)))
				token = ((SpecialToken) token).normalized();
		}
		return condition.add(token);
	}

	private void buildTransitions(int position, List<Set<Tag>> positionTags, Element[] elements, Set<Ngram> result) {
		Set<Tag> tags = positionTags.get(position);
		if (position == this.chain() - 1) {
			for (Tag tag : tags) {
				elements[position] = tag;
				Ngram transition = new Ngram(elements);
				result.add(transition);
			}
			return;
		}
		for (Tag tag : tags) {
			elements[position] = tag;
			buildTransitions(position + 1, positionTags, elements, result);
		}
	}
}