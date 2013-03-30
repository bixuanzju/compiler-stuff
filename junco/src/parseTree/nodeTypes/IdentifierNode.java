package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import logging.JuncoLogger;
import symbolTable.Binding;
import symbolTable.Scope;
import tokens.IdentifierToken;
import tokens.Token;

public class IdentifierNode extends ParseNode {
	private Binding binding;
	private Scope declarationScope;
	private ParseNode localScopeNode;

	public IdentifierNode(Token token) {
		super(token);
		assert (token instanceof IdentifierToken);
		this.binding = null;
	}

	public IdentifierNode(ParseNode node) {
		super(node);

		if (node instanceof IdentifierNode) {
			this.binding = ((IdentifierNode) node).binding;
		}
		else {
			this.binding = null;
		}
	}

	// //////////////////////////////////////////////////////////
	// attributes

	public IdentifierToken identifierToken() {
		return (IdentifierToken) token;
	}

	public void setBinding(Binding binding) {
		this.binding = binding;
	}

	public Binding getBinding() {
		return binding;
	}

	// //////////////////////////////////////////////////////////
	// Speciality functions

	public Binding findVariableBinding() {
		String identifier = token.getLexeme();

		for (ParseNode current : pathToRoot()) {
			if (current.containsBindingOf(identifier)) {
				declarationScope = current.getScope();
				localScopeNode = current;
				return current.bindingOf(identifier);
			}
		}
		useBeforeDefineError();
		return Binding.nullInstance();
	}

	public ParseNode findScopeNode() {
		return localScopeNode;
	}

	public Binding findGlobalBinding() {
		ParseNode parent = this.getParent();

		while (!(parent instanceof ProgramNode)) {
			parent = parent.getParent();
		}

		String identifier = token.getLexeme();
		if (parent.containsBindingOf(identifier)) {
			declarationScope = parent.getScope();
			localScopeNode = parent;
			return parent.bindingOf(identifier);
		}
		
		useBeforeDefineError();
		return Binding.nullInstance();
		
	}

	public Scope getDeclarationScope() {
		findVariableBinding();
		return declarationScope;
	}

	public void useBeforeDefineError() {
		JuncoLogger log = JuncoLogger
				.getLogger("compiler.semanticAnalyzer.identifierNode");
		Token token = getToken();
		log.severe("identifier " + token.getLexeme() + " used before defined at "
				+ token.getLocation());
	}

	// /////////////////////////////////////////////////////////
	// accept a visitor

	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}

}
