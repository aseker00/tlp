package aseker00.tlp.pos;

import aseker00.tlp.ling.Token;

public class SpecialToken extends Token {
	Token normalizedToken;
	public SpecialToken(String value, Token normalizedToken) {
		super(value);
		this.normalizedToken = normalizedToken;
	}

	public Token normalized() {
		return this.normalizedToken;
	}
}
