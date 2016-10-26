package aseker00.tlp.ling;

// Syntactic Part of Speech Tag
public class Tag implements Element {
	public static final String type = "TAG";
	private String value;
	
	public Tag(String value) {
		this.value = value;
	}
	
	@Override
	public String type() {
		return Tag.type;
	}
	
	@Override 
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (!(other instanceof Tag))
			return false;
		Tag that = (Tag)other;
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