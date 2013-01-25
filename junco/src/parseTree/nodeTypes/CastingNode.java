package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Lextant;
import tokens.LextantToken;
import tokens.Token;

public class CastingNode extends ParseNode {
	
	public CastingNode(Token token) {
		super(token);
		assert(token instanceof LextantToken);
	}

	public CastingNode(ParseNode node) {
		super(node);
	}
	
	
	public Lextant getTypeIndicator() {
		return lextantToken().getLextant();
	}
	public LextantToken lextantToken() {
		return (LextantToken)token;
	}	
	
	
	public static CastingNode withChildren(ParseNode left, Token token) {
		CastingNode node = new CastingNode(token);
		node.appendChild(left);
		return node;
	}
	
	
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}

}
