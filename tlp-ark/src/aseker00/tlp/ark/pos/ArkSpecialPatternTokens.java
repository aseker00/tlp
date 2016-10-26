package aseker00.tlp.ark.pos;

import java.util.regex.Pattern;

import cmu.arktweetnlp.Twokenize;

public class ArkSpecialPatternTokens {
	public static final Pattern NUMBER = Pattern.compile("(\\d+(?::\\d+){1,2}|(?:(?<!\\d)\\d{1,3},)+?\\d{3}(?=(?:[^,\\d]|$))|\\p{Sc}?\\d+(?:\\.\\d+)+%?)");
	public static final Pattern URL  = Pattern.compile(Twokenize.url);
	//private static String punctSeq   = "['\"“”‘’]+|[.?!,…]+|[:;]+";
	//private static String urlStart1  = "(?:https?://|\\bwww\\.)";
	//public static final Pattern URL2 = Pattern.compile(urlStart1 + punctSeq);
	public static final Pattern EMAIL  = Pattern.compile(Twokenize.Email);
	public static final Pattern EMOTICON = Pattern.compile(Twokenize.emoticon);
	public static final Pattern HASHTAG = Pattern.compile("#[a-zA-Z0-9_]+");
	public static final Pattern AT_MENTION = Pattern.compile("[@＠][a-zA-Z0-9_]+");
	public static final Pattern EMOJI = Pattern.compile("([\\x{20a0}-\\x{32ff}]+|[\\x{1f000}-\\x{1ffff}]+)");
	public static final Pattern HEARTS_AND_ARROWS = Pattern.compile("((?:<*[-―—=]*>+|<+[-―—=]*>*)|\\p{InArrows}+|(?:<+/?3+)+)");
}