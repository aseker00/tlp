package aseker00.tlp.twitter.pos;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {

	private static CommandLine initCli(String args[]) throws ParseException {
		Options options = new Options();
		options.addOption("home", true, "workspace base directory");
		options.addOption("chain", true, "tagger linear chain size");
		options.addOption("size", true, "size of training set");
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse( options, args);
		return cmd;
	}
}
