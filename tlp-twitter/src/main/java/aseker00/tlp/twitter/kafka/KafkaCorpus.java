package aseker00.tlp.twitter.kafka;

import aseker00.tlp.io.Corpus;
import aseker00.tlp.io.CorpusReader;
import aseker00.tlp.pos.Tagger;

public class KafkaCorpus implements Corpus {
	private String name;
	private int size;
	private String topicName;
	private int offset;
	private Tagger tagger;
	
	public KafkaCorpus(String name, int size, String topicName, int offset, Tagger tagger) {
		this.name = name;
		this.size = size;
		this.topicName = topicName;
		this.offset = offset;
		this.tagger = tagger;
	}
	
	@Override
	public String name() {
		return this.name;
	}
	
	@Override
	public int size() {
		return this.size;
	}
	
	@Override
	public CorpusReader readerNew(String name) {
		return new KafkaCorpusReader(name, this, this.tagger);
	}
	
	public String topicName() {
		return this.topicName;
	}
	
	public int offset() {
		return this.offset;
	}
}