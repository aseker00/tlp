package aseker00.tlp.ark.io;

import java.util.ArrayList;
import java.util.Iterator;

import aseker00.tlp.io.Corpus;
import aseker00.tlp.io.CorpusReader;
import aseker00.tlp.io.Tweet;
import aseker00.tlp.ling.Tag;
import aseker00.tlp.ling.Token;
import aseker00.tlp.pos.SpecialToken;
import aseker00.tlp.pos.Tagger;
import cmu.arktweetnlp.impl.Sentence;

public class ArkCorpusReader implements CorpusReader {
	private String name;
	private ArkCorpus corpus;
	private ArrayList<Tweet> tweets;
	private Iterator<Tweet> tweetsIter;
	private Tagger tagger;

	public ArkCorpusReader(String name, ArkCorpus corpus, Tagger tagger) {
		this.name = name;
		this.corpus = corpus;
		this.tagger = tagger;
		this.tweets = new ArrayList<Tweet>();
		for (Sentence sentence : corpus.sentences())
			this.tweets.add(this.tweetify(sentence));
		this.tweetsIter = this.tweets.iterator();
	}

	@Override
	public Corpus corpus() {
		return this.corpus;
	}

	@Override
	public Tweet next() {
		if (!this.tweetsIter.hasNext())
			return null;
		return this.tweetsIter.next();
	}

	private Tweet tweetify(Sentence sentence) {
		Tweet tweet = new Tweet() {
			@Override
			public int tokens() {
				return sentence.T();
			}

			@Override
			public Token token(int index) {
				String str = sentence.tokens.get(index);
				if (str.equals("https")) {
					if (index + 2 == sentence.tokens.size() && sentence.tokens.get(index + 1).equals("…") || sentence.tokens.get(index + 1).equals("...")) {
						Token token = tagger.token(str + "://");
						SpecialToken special = new SpecialToken(str, ((SpecialToken) token).normalized());
						return special;
					}
				} else if (str.equals("https:")) {
					if (index + 2 == sentence.tokens.size() && sentence.tokens.get(index + 1).equals("…") || sentence.tokens.get(index + 1).equals("...")) {
						Token token = tagger.token(str + "//");
						SpecialToken special = new SpecialToken(str, ((SpecialToken) token).normalized());
						return special;
					}
				} else if (str.equals("https:/")) {
					if (index + 2 == sentence.tokens.size() && sentence.tokens.get(index + 1).equals("…") || sentence.tokens.get(index + 1).equals("...")) {
						Token token = tagger.token(str + "/");
						SpecialToken special = new SpecialToken(str, ((SpecialToken) token).normalized());
						return special;
					}
				} else if (str.equals("http")) {
					if (index + 2 == sentence.tokens.size() && sentence.tokens.get(index + 1).equals("…") || sentence.tokens.get(index + 1).equals("...")) {
						Token token = tagger.token(str + "://");
						SpecialToken special = new SpecialToken(str, ((SpecialToken) token).normalized());
						return special;
					}
				} else if (str.equals("http:")) {
					if (index + 2 == sentence.tokens.size() && (sentence.tokens.get(index + 1).equals("…") || sentence.tokens.get(index + 1).equals("..."))) {
						Token token = tagger.token(str + "//");
						SpecialToken special = new SpecialToken(str, ((SpecialToken) token).normalized());
						return special;
					}
				} else if (str.equals("http:/")) {
					if (index + 2 == sentence.tokens.size() && sentence.tokens.get(index + 1).equals("…") || sentence.tokens.get(index + 1).equals("...")) {
						Token token = tagger.token(str + "/");
						SpecialToken special = new SpecialToken(str, ((SpecialToken) token).normalized());
						return special;
					}
				}
				return tagger.token(str);
			}

			@Override
			public Tag tag(int index) {
				return new Tag(sentence.labels.get(index));
			}

			@Override
			public String toString() {
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < this.tokens(); i++) {
					sb.append(this.tag(i)).append("/").append(this.token(i));
					if (i < this.tokens())
						sb.append(", ");
				}
				return sb.toString();
			}
		};
		return tweet;
	}
}