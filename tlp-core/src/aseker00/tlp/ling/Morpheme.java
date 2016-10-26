package aseker00.tlp.ling;

// The smallest meaningful unit in a language 
// Cannot be divided without altering or destroying its meaning
// For example: kind is a morepheme, if the d is removed the result - kin - has a different meaning
// For example: unkindness consists of 3 morphemes: kind (the stem) + negative prefix un + noun forming suffix ness
// Some morphemse have grammatical functions: s could show a verb to be third-person singular present tense form.
public class Morpheme implements Element {
	public static final String type = "MORPHEME";
	private String value;
	private Lexeme lexeme; // The containing lexeme
	
	@Override
	public String type() {
		return Morpheme.type;
	}
	
	@Override 
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (!(other instanceof Morpheme))
			return false;
		Morpheme that = (Morpheme)other;
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