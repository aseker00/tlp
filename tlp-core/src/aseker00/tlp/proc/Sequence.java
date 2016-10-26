package aseker00.tlp.proc;

import java.util.List;

import aseker00.tlp.ling.Element;

public interface Sequence {
	public int length();
	public Element entry(int index);
	public List<Element> entriesList();
}