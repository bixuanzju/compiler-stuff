package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Lextant;
import tokens.LextantToken;
import tokens.Token;

public class UniaryOperatorNode extends ParseNode {
	
	public UniaryOperatorNode(Token token) {
		super(token);
		assert(token instanceof LextantToken);
	}

	public UniaryOperatorNode(ParseNode node) {
		super(node);
	}
	
	
	public Lextant getTypeIndicator() {
		return lextantToken().getLextant();
	}
	public LextantToken lextantToken() {
		return (LextantToken)token;
	}	
	
	
	public static UniaryOperatorNode withChildren(ParseNode left, Token token) {
		UniaryOperatorNode node = new UniaryOperatorNode(token);
		node.appendChild(left);
		return node;
	}
	
	
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}

}
