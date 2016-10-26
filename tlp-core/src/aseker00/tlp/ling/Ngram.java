package aseker00.tlp.ling;

import java.util.Arrays;

public class Ngram implements Element {
	public static final String type = "-GRAM";
	private Element[] elements;
	
	public Ngram() {
		this.elements = new Element[0];
	}
	
	public Ngram(Element[] elements) {
		this.elements = Arrays.copyOf(elements, elements.length);
	}
	
	public Ngram(Element e) {
		this(new Element[]{e});
	}
	
	public Ngram(Ngram other) {
		this(other.elements);
	}
	
	public int size() {
		return this.elements.length;
	}
	
	public Element[] elements() {
		return Arrays.copyOf(this.elements, this.size());
	}
	
	public Ngram add(Element e) {
		if (e instanceof Ngram)
			return this.add((Ngram)e);
		Element[] newElements = Arrays.copyOf(this.elements, this.size()+1);
		newElements[this.size()] = e;
		Ngram result = new Ngram(newElements);
		return result;
	}
	
	public Ngram add(Ngram that) {
		Element[] newElements = Arrays.copyOf(this.elements, this.size()+that.size());
		for (int i = 0; i < that.size(); i++)
			newElements[this.size()+i] = that.elements[i];
		Ngram result = new Ngram(newElements);
		return result;
	}
	
	public Element entry(int index) {
		return this.elements[index];
	}
	
	public Ngram subgram(int fromIndex) {
		return this.subgram(fromIndex, this.size());
	}
	
	public Ngram subgram(int fromIndex, int toIndex) {
		Element[] newElements = Arrays.copyOfRange(this.elements, fromIndex, toIndex);
		Ngram result = new Ngram(newElements);
		return result;
	}
	
	@Override
	public String type() {
		return String.valueOf(this.elements.length) + Ngram.type;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (!(other instanceof Ngram))
			return false;
		Ngram that = (Ngram)other;
		return Arrays.equals(this.elements, that.elements);
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(this.elements);
	}
	
	@Override
	public String toString() {
		return Arrays.toString(this.elements);
	}
}