package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Keyword;
import tokens.Token;

public class WhileStatementNode extends ParseNode {

	public WhileStatementNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.WHILE));
	}

	public WhileStatementNode(ParseNode node) {
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
	
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
//	public static WhileStatementNode withChildren(Token token, ParseNode declaredName, ParseNode initializer) {
//		WhileStatementNode node = new WhileStatementNode(token);
//		node.appendChild(declaredName);
//		node.appendChild(initializer);
//		return node;
//	}
	
	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
			
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
