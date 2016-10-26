package aseker00.tlp.pos;

import java.util.HashMap;
import java.util.Set;

import aseker00.tlp.ling.Tag;
import aseker00.tlp.ling.Token;

public class Lexicon {
	private String name;
	protected HashMap<Entry, Integer> counts;
	protected HashMap<Token, Set<Entry>> entries;
	
	public class Entry {
		protected Token form; 
		protected Tag tag;
		public Entry(Token form, Tag tag) {
			this.form = form;
			this.tag = tag;
		}
		public Token form() {
			return this.form;
		}
		public Tag tag() {
			return this.tag;
		}
		@Override
		public int hashCode() {
			int hash = 1;
			hash = hash*17 + this.form.hashCode();
			hash = hash*31 + this.tag.hashCode();
			return hash;
		}
		@Override
		public boolean equals(Object other) {
			if (other == null)
				return false;
			if (!(other instanceof Entry))
				return false;
			Entry that = (Entry)other;
			return this.form.equals(that.form) && this.tag.equals(that.tag);
		}
	}
	
	public Lexicon(String name) {
		this.name = name;
		this.counts = new HashMap<Entry, Integer>();
		this.entries = new HashMap<Token, Set<Entry>>();
	}
	
	public String name() {
		return this.name;
	}
		
	public Set<Entry> entries(Token token) {
		return this.entries.get(token);
	}
	
	public int count(Entry entry) {
		return this.counts.getOrDefault(entry, 0);
	}
}