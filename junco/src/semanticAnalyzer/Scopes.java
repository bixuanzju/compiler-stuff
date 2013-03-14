package semanticAnalyzer;

import java.util.Stack;
//import parseTree.ParseNode;
import symbolTable.Scope;

// During the semanticAnalyzer traversal, this keeps a stack of currently open scopes.
public class Scopes {
	static Stack<Scope> scopes = new Stack<Scope>();

	private Scopes() {}

//	public static void enterStaticScope(ParseNode node) {
//		int level = scopes.size();
//		Scope baseScope = (level == 0) ? null : scopes.peek().getBaseScope();
//		Scope scope = Scope.createStaticScope(level, baseScope);
//		scopes.push(scope);
//		node.setScope(scope);
//	}
//
//	public static Scope leaveScope() {
//		Scope scope = scopes.pop();
//		scope.leave();
//		return scope;
//	}
}
