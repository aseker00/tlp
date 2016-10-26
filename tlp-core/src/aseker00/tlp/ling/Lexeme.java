package aseker00.tlp.ling;

// smallest unit of meaning
// occur in many different forms
// For example: give, gives, give giving, gave belong to the lexeme give.
// It corresponds to an entry in a dictionry
public class Lexeme implements Element {
	public static final String type = "Lexeme";
	private Token value;
	private Lemma lemma;
	
	@Override
	public String type() {
		return Lexeme.type;
	}
	
	@Override 
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (!(other instanceof Lexeme))
			return false;
		Lexeme that = (Lexeme)other;
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