package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Lextant;
import tokens.LextantToken;
import tokens.Token;

public class BooleanNotNode extends ParseNode {
	
	public BooleanNotNode(Token token) {
		super(token);
		assert(token instanceof LextantToken);
	}

	public BooleanNotNode(ParseNode node) {
		super(node);
	}
	
	
	public Lextant getTypeIndicator() {
		return lextantToken().getLextant();
	}
	public LextantToken lextantToken() {
		return (LextantToken)token;
	}	
	
	
	public static BooleanNotNode withChildren(ParseNode left, Token token) {
		BooleanNotNode node = new BooleanNotNode(token);
		node.appendChild(left);
		return node;
	}
	
	
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}

}
