package aseker00.tlp.io;

public interface Corpus {
	public String name();
	public int size();
	public CorpusReader readerNew(String name);
}