package aseker00.tlp.twitter.kafka;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import twitter4j.Status;

public class KafkaCorpusWriter {
	KafkaCorpus corpus;
	KafkaProducer<String, String> producer;
	
	public KafkaCorpusWriter() {
		Properties props = new Properties();
		props.put("bootstrap.servers", "localhost:4242");
		props.put("acks", "all");
		props.put("retries", 0);
		props.put("batch.size", 16384);
		props.put("linger.ms", 1);
		props.put("buffer.memory", 33554432);
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		this.producer = new KafkaProducer<String, String>(props);
	}
	
	public void statusAdd(Status status) {
		producer.send(new ProducerRecord<String, String>(this.corpus.name(), Long.toString(status.getId()), status.getText()));
	}
	
	public void corpusIs(KafkaCorpus corpus) {
		this.corpus = corpus;
		if (corpus == null)
			this.producer.close();
	}
}