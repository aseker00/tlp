package aseker00.tlp.ling;

import java.util.Set;

public class Lemma implements Element {
	public static final String type = "LEMMA";
	private Lexeme value;
	private Set<Token> words;
	
	@Override
	public String type() {
		return Lemma.type;
	}
	
	@Override 
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (!(other instanceof Lemma))
			return false;
		Lemma that = (Lemma)other;
		return this.value.equals(that.value);
	}
	
	@Override 
	public int hashCode() {
		return this.value.hashCode();
	}
	
	@Override
	public String toString() {
		return this.value.toString();
	}
}