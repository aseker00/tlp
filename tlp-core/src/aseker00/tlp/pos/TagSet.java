package aseker00.tlp.pos;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import aseker00.tlp.ling.Tag;

public class TagSet implements Iterable<Tag> {
	private String name;
	protected HashSet<Tag> tags;
	public TagSet(String name, Set<Tag> tags) {
		this.name = name;
		this.tags = new HashSet<Tag>(tags);
	}
	public String name() {
		return name;
	}
	@Override
	public Iterator<Tag> iterator() {
		return this.tags.iterator();
	}
	public boolean contains(Tag tag) {
		return this.tags.contains(tag);
	}
}