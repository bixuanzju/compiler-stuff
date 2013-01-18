package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import tokens.LextantToken;
import tokens.Token;

public class PrintStatementNode extends ParseNode {
	private boolean hasNewline = false;
	
	public PrintStatementNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.PRINT, Keyword.PRUNT));
	}

	public PrintStatementNode(ParseNode node) {
		super(node);
	}

	
	////////////////////////////////////////////////////////////
	// attributes
	
	public Lextant getPrintType() {
		return lextantToken().getLextant();
	}
	public LextantToken lextantToken() {
		return (LextantToken)token;
	}
	
	public void setHasNewline(boolean value) {
		hasNewline = value;
	}
	public boolean hasNewline() {
		return hasNewline;
	}
	
	
	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
		
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}

	public boolean hasSpaces() {
		return getPrintType() == Keyword.PRINT;
	}
}
