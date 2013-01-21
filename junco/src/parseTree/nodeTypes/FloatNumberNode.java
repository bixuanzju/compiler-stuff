package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.FloatingToken;
import tokens.Token;

public class FloatNumberNode extends ParseNode {
	public FloatNumberNode(Token token) {
		super(token);
		assert(token instanceof FloatingToken);
	}
	public FloatNumberNode(ParseNode node) {
		super(node);
	}

////////////////////////////////////////////////////////////
// attributes
	
	public double getValue() {
		return FloatingToken().getValue();
	}

	public FloatingToken FloatingToken() {
		return (FloatingToken)token;
	}	

///////////////////////////////////////////////////////////
// accept a visitor
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}

}
