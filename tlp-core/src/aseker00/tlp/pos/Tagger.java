package aseker00.tlp.pos;

import java.util.HashSet;

import aseker00.tlp.ling.Tag;
import aseker00.tlp.ling.Token;
import aseker00.tlp.proc.ProcessorImpl;

public class Tagger extends ProcessorImpl {
	//private HashSet<Tag> openClassTags;
	//private HashSet<Tag> closedClassTags;
	//private HashSet<Tag> specialTags;
	//private HashSet<Tag> allTags; 
	private TagSet openClassTags;
	private TagSet closedClassTags;
	private TagSet specialTags;
	private TagSet allTags;
	private Tag startTag;
	private Tag stopTag;
	private Lexicon lexicon;
	
	public Tagger(String name) {
		super(name);
	}
	
	public Token token(String str) {
		return new Token(str);
	}
	
	public Tag tag(String str) {
		Tag tag = new Tag(str);
		if (this.allTags.contains(tag))
			return tag;
		if (tag.equals(this.startTag))
			return tag;
		if (tag.equals(this.stopTag))
			return tag;
		return null;
	}
	
	public void lexiconIs(Lexicon lexicon) {
		this.lexicon = lexicon;
	}
	
	public Lexicon lexicon() {
		return this.lexicon;
	}
	
	public void openClassTagSetIs(TagSet tags) {
		this.openClassTags = tags;
		this.allTags = this.getTagSet();
	}
	
	public TagSet openClassTagSet() {
		return this.openClassTags;
	}
	
	public void closedClassTagSetIs(TagSet tags) {
		this.closedClassTags = tags;
		this.allTags = this.getTagSet();
	}
	
	public TagSet closedClassTagSet() {
		return this.closedClassTags;
	}
	
	public void specialTagSetIs(TagSet tags) {
		this.specialTags = tags;
		this.allTags = this.getTagSet();
	}
	
	public TagSet specialTagSet() {
		return this.specialTags;
	}
	
	public TagSet tagSet() {
		return this.allTags;
	}
	
	public void startTagIs(Tag tag) {
		this.startTag = tag;
	}
	
	public Tag startTag() {
		return this.startTag;
	}
	
	public void stopTagIs(Tag tag) {
		this.stopTag = tag;
	}
	
	public Tag stopTag() {
		return this.stopTag;
	}
	
	public TagSet tags(Token token) {
		return this.tagSet();
	}
	
	private TagSet getTagSet() {
		HashSet<Tag> tags = new HashSet<Tag>();
		if (this.openClassTags != null)
			tags.addAll(this.openClassTags.tags);
		if (this.closedClassTags != null)
			tags.addAll(this.closedClassTags.tags);
		if (this.specialTags != null)
			tags.addAll(this.specialTags.tags);
		return new TagSet("all", tags);
	}
}