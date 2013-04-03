package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class MemberAccessNode extends ParseNode {
	
	

	public MemberAccessNode(Token token) {
		super(token);
	}

	public MemberAccessNode(ParseNode node) {
		super(node);
	}
	
	public static MemberAccessNode withChildren(Token token, ParseNode left, ParseNode right) {
		MemberAccessNode node = new MemberAccessNode(token);
		node.appendChild(left);
		node.appendChild(right);
		return node;
	}
			
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
