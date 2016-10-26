package aseker00.tlp.twitter;

import aseker00.tlp.twitter.kafka.KafkaCorpusWriter;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

public class Stream {
	private TwitterStream twitterStream;
	private KafkaCorpusWriter corpusWriter;

	public Stream(KafkaCorpusWriter corpusWriter) {
		this.corpusWriter = corpusWriter;
		ConfigurationBuilder builder = new ConfigurationBuilder().setOAuthConsumerKey("uSLHg4cBizx56yD4BGLGicHeM")
				.setOAuthConsumerSecret("6E2jFTc1UsAVnc1ywhQHu5AoBxnmia0I5Zo23d1nixfNhodZvb")
				.setOAuthAccessToken("3176145554-XkPezFuQXJiRZe3wypWDArVcJF9QdmPGtqzJ69B")
				.setOAuthAccessTokenSecret("loVrjPbp2Mhyd5Z7YSMQxx61nFxTaGo0ch2lyxZ0EpX99");
		this.twitterStream = new TwitterStreamFactory(builder.build()).getInstance();
	}

	public void sampleIs(boolean sample) {
		if (!sample) {
			this.twitterStream.cleanUp();
			this.twitterStream.clearListeners();
			return;
		}

		StatusListener listener = new StatusListener() {
			public void onStatus(final Status status) {
				System.out.println(status.getUser().getName() + " : " + status.getText());
				corpusWriter.statusAdd(status);
			}

			public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
			}

			public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
			}

			public void onException(Exception ex) {
				ex.printStackTrace();
			}

			public void onScrubGeo(long userId, long upToStatusId) {
				// TODO Auto-generated method stub

			}

			public void onStallWarning(StallWarning warning) {
				// TODO Auto-generated method stub

			}
		};
		this.twitterStream.addListener(listener);
	}
}