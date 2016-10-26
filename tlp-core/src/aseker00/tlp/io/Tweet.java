package aseker00.tlp.io;

import aseker00.tlp.ling.Token;
import aseker00.tlp.ling.Tag;

public interface Tweet {
	public int tokens();
	public Token token(int index);
	public Tag tag(int index);
}
