package lexicalAnalyzer;

import inputHandler.PushbackCharStream;
import tokens.NullToken;
import tokens.Token;

public abstract class ScannerImp implements Scanner {
	protected Token nextToken;
	protected final PushbackCharStream input;
	
	protected abstract void findNextToken();

	public ScannerImp(PushbackCharStream input) {
		super();
		this.input = input;
		nextToken = null;
		findNextToken();
	}

	// Iterator<Token> implementation
	@Override
	public boolean hasNext() {
		return !(nextToken instanceof NullToken);
	}

	@Override
	public Token next() {
		Token result = nextToken;
		findNextToken();
		return result;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}