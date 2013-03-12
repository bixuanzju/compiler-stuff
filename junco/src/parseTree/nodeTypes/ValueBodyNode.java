package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.TypeVariable;
import tokens.Token;

public class ValueBodyNode extends ParseNode {
	
	private TypeVariable returnType = new TypeVariable();
	
	public TypeVariable getReturnType() {
		return returnType;
	}

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
