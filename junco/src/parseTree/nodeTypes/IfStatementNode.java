package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Keyword;
import tokens.Token;

public class IfStatementNode extends ParseNode {

	public IfStatementNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.IF));
	}

	public IfStatementNode(ParseNode node) {
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
	
//	public static IfStatementNode withChildren(Token token, ParseNode declaredName, ParseNode initializer) {
//		IfStatementNode node = new IfStatementNode(token);
//		node.appendChild(declaredName);
//		node.appendChild(initializer);
//		return node;
//	}
//	
//	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
			
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
