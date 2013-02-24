package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class ValueBodyNode extends ParseNode {

	public ValueBodyNode(ParseNode node) {
		super(node);
	
	}
	
	public ValueBodyNode(Token token) {
		super(token);
	
	}
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}

}
