package aseker00.tlp.ark.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import aseker00.tlp.io.Corpus;
import aseker00.tlp.io.CorpusReader;
import aseker00.tlp.pos.Tagger;
import cmu.arktweetnlp.impl.Sentence;
import cmu.arktweetnlp.io.CoNLLReader;

public class ArkCorpus implements Corpus {	
	private String name;
	private ArrayList<Sentence> sentences;
	private Tagger tagger;
	
	public ArkCorpus(String name, File file, Tagger tagger) throws IOException {
		this.name = name;
		this.sentences = CoNLLReader.readFile(file.getAbsolutePath());
		this.tagger = tagger;
	}
	
	@Override
	public String name() {
		return this.name;
	}
	
	@Override
	public int size() {
		return this.sentences.size();
	}
	
	@Override
	public CorpusReader readerNew(String name) {
		return new ArkCorpusReader(name, this, this.tagger);
	}
	
	public ArrayList<Sentence> sentences() {
		return this.sentences;
	}
}