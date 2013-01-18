package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.NumberToken;
import tokens.Token;

public class IntNumberNode extends ParseNode {
	public IntNumberNode(Token token) {
		super(token);
		assert(token instanceof NumberToken);
	}
	public IntNumberNode(ParseNode node) {
		super(node);
	}

////////////////////////////////////////////////////////////
// attributes
	
	public int getValue() {
		return numberToken().getValue();
	}

	public NumberToken numberToken() {
		return (NumberToken)token;
	}	

///////////////////////////////////////////////////////////
// accept a visitor
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}

}
