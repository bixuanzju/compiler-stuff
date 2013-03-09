package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Keyword;
//import lexicalAnalyzer.Lextant;
//import tokens.LextantToken;
import tokens.Token;

public class ReturnStatementNode extends ParseNode {

	public ReturnStatementNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.RETURN));
	}

	public ReturnStatementNode(ParseNode node) {
		super(node);
	}
	
	
	////////////////////////////////////////////////////////////
	// attributes
	
//	public Lextant getDeclarationType() {
//		return lextantToken().getLextant();
//	}
//	public LextantToken lextantToken() {
//		return (LextantToken)token;
//	}	
//	
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static ReturnStatementNode withChildren(Token token, ParseNode expression) {
		ReturnStatementNode node = new ReturnStatementNode(token);
		node.appendChild(expression);
		return node;
	}
	
	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
			
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
