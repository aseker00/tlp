package aseker00.tlp.twitter.kafka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import aseker00.tlp.io.Corpus;
import aseker00.tlp.io.CorpusReader;
import aseker00.tlp.io.Tweet;
import aseker00.tlp.ling.Tag;
import aseker00.tlp.ling.Token;
import aseker00.tlp.pos.SpecialToken;
import aseker00.tlp.pos.Tagger;
import cmu.arktweetnlp.Twokenize;
import twitter4j.Status;
import twitter4j.TwitterObjectFactory;

public class KafkaCorpusReader implements CorpusReader {
	private String name;
	private KafkaCorpus corpus;
	private List<Status> currentStatuses;
	private Iterator<Status> currentStatusesIter;
	private int maxBufferSize;
	private Tagger tagger;
	private KafkaConsumer<String, String> consumer;
	private TopicPartition partition;
	private int current;
		
	public KafkaCorpusReader(String name, KafkaCorpus corpus, Tagger tagger) {
		this.name = name;
		this.corpus = corpus;
		this.tagger = tagger;
		this.currentStatuses = new ArrayList<Status>();
		this.currentStatusesIter = this.currentStatuses.iterator();
		this.maxBufferSize = Integer.MAX_VALUE;
		Properties props = new Properties();
		props.put("bootstrap.servers", "localhost:9092");
		props.put("enable.auto.commit", "false");
		//props.put("auto.commit.interval.ms", "1000");
		props.put("session.timeout.ms", "300000");
		props.put("fetch.max.wait.ms", "300000");
		props.put("request.timeout.ms", "300001");
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("group.id", corpus.name() + "::" + this.name);
		this.consumer = new KafkaConsumer<>(props);
		this.consumer.subscribe(Arrays.asList(corpus.topicName()));
		this.partition = new TopicPartition(corpus.topicName(), 0);
		this.consumer.commitSync(Collections.singletonMap(partition, new OffsetAndMetadata(this.corpus.offset())));
	}
	
	public void maxBufferSizeIs(int size) {
		this.maxBufferSize = size;
	}
	
	private List<Status> pollStatuses(int pollAmount) {
		List<Status> statuses = new ArrayList<Status>();
		HashSet<String> texts = new HashSet<String>();
		do {
			ConsumerRecords<String, String> records = consumer.poll(pollAmount);
			for (ConsumerRecord<String, String> topicRecord: records.records(corpus.topicName())) {
	 			String rawJSON = topicRecord.value();
				try {
					Status status = TwitterObjectFactory.createStatus(rawJSON);
					if (status.getUser().getFollowersCount() < 100 || status.getUser().getFriendsCount() < 100)
						continue;
					if (!texts.add(status.getText()))
						continue;
					statuses.add(status);
				} catch (Throwable e) {
					//System.out.println(topicRecord.offset() + ": " + topicRecord.value());
					System.out.println(e.getMessage());
				}
	 		}
		} while (statuses.isEmpty());
		this.consumer.commitSync();
		return statuses;
	}
	
	@Override
	public Corpus corpus() {
		return this.corpus;
	}
	
	public Status statusNext() {
		if (this.current == this.corpus.size())
			return null;
		if (!this.currentStatusesIter.hasNext()) {
			this.currentStatuses = this.pollStatuses(this.maxBufferSize);
			this.currentStatusesIter = this.currentStatuses.iterator();
		}
		this.current++;
		if (this.current == this.corpus.size()) {
			this.consumer.unsubscribe();
			this.consumer.close();
		}
		return this.currentStatusesIter.next();
	}

	@Override
	public Tweet next() {
		Status status = this.statusNext();
		if (status == null)
			return null;
		Tweet tweet = tweetify(status);
		if (tweet.tokens() > 40)
			return next();
		return tweet;
	}

	private Tweet tweetify(Status status) {
		List<String> tokens = Twokenize.tokenizeRawTweetText(status.getText());
		Tweet tweet = new Tweet() {
			{
				if (tokens.get(tokens.size()-1).equals("…") && (tokens.get(tokens.size()-2).equals(":") || tokens.get(tokens.size()-2).equals(":/")) && tokens.get(tokens.size()-3).equals("https")) {
					tokens.set(tokens.size()-3, tokens.get(tokens.size()-3) + tokens.get(tokens.size()-2));
					tokens.set(tokens.size()-2, "…");
					tokens.remove(tokens.size()-1);
				}
			}
			@Override
			public int tokens() {
				return tokens.size();
			}
			@Override
			public Token token(int index) {
				String str = tokens.get(index);
				if (str.equals("https")) {
					if (index+2 == tokens.size() && tokens.get(index+1).equals("…") || tokens.get(index + 1).equals("...")) {
						Token token = tagger.token(str + "://");
						SpecialToken special = new SpecialToken(str, ((SpecialToken)token).normalized());
						return special;
					}
				}
				else if (str.equals("https:")) {
					if (index+2 == tokens.size() && tokens.get(index+1).equals("…") || tokens.get(index + 1).equals("...")) {
						Token token = tagger.token(str + "//");
						SpecialToken special = new SpecialToken(str, ((SpecialToken)token).normalized());
						return special;
					}
				}
				else if (str.equals("https:/")) {
					if (index+2 == tokens.size() && tokens.get(index+1).equals("…") || tokens.get(index + 1).equals("...")) {
						Token token = tagger.token(str + "/");
						SpecialToken special = new SpecialToken(str, ((SpecialToken)token).normalized());
						return special;
					}
				}
				else if (str.equals("http")) {
					if (index+2 == tokens.size() && tokens.get(index+1).equals("…") || tokens.get(index + 1).equals("...")) {
						Token token = tagger.token(str + "://");
						SpecialToken special = new SpecialToken(str, ((SpecialToken)token).normalized());
						return special;
					}
				}
				else if (str.equals("http:")) {
					if (index+2 == tokens.size() && tokens.get(index+1).equals("…") || tokens.get(index + 1).equals("...")) {
						Token token = tagger.token(str + "//");
						SpecialToken special = new SpecialToken(str, ((SpecialToken)token).normalized());
						return special;
					}
				}
				else if (str.equals("http:/")) {
					if (index+2 == tokens.size() && tokens.get(index+1).equals("…") || tokens.get(index + 1).equals("...")) {
						Token token = tagger.token(str + "/");
						SpecialToken special = new SpecialToken(str, ((SpecialToken)token).normalized());
						return special;
					}
				}
				return tagger.token(str);
			}
			@Override
			public Tag tag(int index) {
				return null;
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