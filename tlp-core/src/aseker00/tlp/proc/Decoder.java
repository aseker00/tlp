package aseker00.tlp.proc;

import aseker00.tlp.io.Tweet;

public interface Decoder {
	public String name();
	public Tweet tweet(Model model, Tweet input);
	public Processor processor();
}
