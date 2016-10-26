package aseker00.tlp.ling;

import java.util.List;

public class Token implements Element {
	public static final String type = "WORD";
	private String value;
	private Morpheme stem;	// the main meaning
	private List<Morpheme> affixes;	// prefixes (before the stem), suffixes (after the stem), circumfixes (both beofer and after the stem), infixes (within the stem)

	public Token(String value) {
		this.value = value;
	}
	
	@Override
	public String type() {
		return Token.type;
	}
	
	@Override 
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (!(other instanceof Token))
			return false;
		Token that = (Token)other;
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