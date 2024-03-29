package parseTree;

import java.util.ArrayList;
import java.util.List;
import parseTree.nodeTypes.ProgramNode;

import semanticAnalyzer.PrimitiveType;
import semanticAnalyzer.Type;
import symbolTable.Binding;
import symbolTable.Scope;
import symbolTable.SymbolTable;
import tokens.Token;

public class ParseNode {
	public static final ParseNode NO_PARENT = null;
	
	List<ParseNode>	children;
	ParseNode parent;
	String returnLabel;
	String loopLabel;
	String boxName;

	protected Token token;
	Type type;					// used for expressions
	private Scope scope;		// the scope created by this node, if any.

	public ParseNode(Token token) {
		this.token = token;
		this.type = PrimitiveType.NO_TYPE;
		this.scope = null;
		this.parent = NO_PARENT;
		initChildren();
	}
	// "detached" copy constructor.  Copies all info except tree info (parent and children)
	public ParseNode(ParseNode node) {
		this.token = node.token;
		this.type = node.type;
		this.scope = node.scope;
	}
	public Token getToken() {
		return token;
	}
	
	public String getReturnLabel() {
		return returnLabel;
	}
	
	public void setReturnLabel(String label) {
		returnLabel = label;
	}
	
	public String returnBoxName() {
		return boxName;
	}
	
	public void setBoxName(String label) {
		boxName = label;
	}
	
	public void setLoopLabel(String label) {
		loopLabel = label;
	}
	
	public String returnLoopLabel() {
		return loopLabel;
	}
	
////////////////////////////////////////////////////////////////////////////////////
// attributes
	
	public void setType(Type type) {
		this.type = type;
	}
	public Type getType() {
		return type;
	}

	
////////////////////////////////////////////////////////////////////////////////////
// scopes and bindings 
	public Scope getScope() {
		return scope;
	}
	public void setScope(Scope scope) {
		this.scope = scope;
	}
	public boolean hasScope() {
		return scope != null;
	}
	public Scope getLocalScope() {
		for(ParseNode current : pathToRoot()) {
			if(current.hasScope()) {
				return current.getScope();
			}
		}
		return Scope.nullInstance();
	}
	public boolean containsBindingOf(String identifier) {
		if(!hasScope()) {
			return false;
		}
		SymbolTable symbolTable = scope.getSymbolTable();
		return symbolTable.containsKey(identifier);
	}
	public Binding bindingOf(String identifier) {
		if(!hasScope()) {
			return Binding.nullInstance();
		}
		SymbolTable symbolTable = scope.getSymbolTable();
		return symbolTable.lookup(identifier);
	}
	public Scope getTopScope() {
		ParseNode parent = this.getParent();

		while (!(parent instanceof ProgramNode)) {
			parent = parent.getParent();
		}
		return parent.getScope();
	}
	
////////////////////////////////////////////////////////////////////////////////////
// dealing with children and parent
//
// note: there is no provision as of yet for removal of children.  Be sure to update
// the removed child's parent pointer if you do implement it.
	
	public ParseNode getParent() {
		return parent;
	}
	protected void setParent(ParseNode parent) {
		this.parent = parent;
	}
	public List<ParseNode> getChildren() {
		return children;
	}
	public ParseNode child(int i) {
		return children.get(i);
	}
	public void initChildren() {
		children = new ArrayList<ParseNode>();
	}
	// adds a new child to this node (as first child) and sets its parent link.
	public void insertChild(ParseNode child) {
		children.add(0, child);
		child.setParent(this);
	}
	// adds a new child to this node (as last child) and sets its parent link.
	public void appendChild(ParseNode child) {
		children.add(child);
		child.setParent(this);
	}
	public int nChildren() {
		return children.size();
	}
	
	public void replaceChild(ParseNode old, ParseNode replacement) {
		for (int i = 0; i < nChildren(); i++) {
			if (children.get(i).equals(old)) {
				children.set(i, replacement);
				replacement.setParent(this);
				break;
			}
		}
	}
	
////////////////////////////////////////////////////////////////////////////////////
//Iterable<ParseNode> pathToRoot

	public Iterable<ParseNode> pathToRoot() {
		return new PathToRootIterable(this);
	}
	
////////////////////////////////////////////////////////////////////////////////////
// toString() 

	public String toString() {
		return ParseTreePrinter.print(this);
	}

	
////////////////////////////////////////////////////////////////////////////////////
// for visitors
			
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
	protected void visitChildren(ParseNodeVisitor visitor) {
		for(ParseNode child : children) {
			child.accept(visitor);
		}
	}
	
	


}
