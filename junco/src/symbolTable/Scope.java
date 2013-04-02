package symbolTable;

import inputHandler.TextLocation;
import logging.JuncoLogger;
import parseTree.nodeTypes.IdentifierNode;
import semanticAnalyzer.Type;
import tokens.Token;

public class Scope {
	private int staticNestingLevel;
	private Scope baseScope;
	private AllocationStrategy allocator;
	private SymbolTable symbolTable;
	
//////////////////////////////////////////////////////////////////////
	
	public Scope createSubscope() {
		return new Scope(staticNestingLevel+1, allocator, this);
	}

	public static Scope createParameterScope() {
		AllocationStrategy allocator = parameterScopeStrategy();
		return new Scope(0, allocator, null);
	}
		
	public static Scope createProcedureScope() {
		AllocationStrategy allocator = procedureScopeStrategy();
		return new Scope(0, allocator, null);
	}
	
	public static Scope createBoxBodyScope() {
		AllocationStrategy allocator = BoxScopeStrategy();
		return new Scope(0, allocator, null);
	}
	
	public static Scope createGlobalScope() {
		AllocationStrategy allocator = globalScopeStrategy();
		return new Scope(0, allocator, null);
	}
	
	private static AllocationStrategy globalScopeStrategy() {
		return new PositiveAllocationStrategy(
				MemoryAccessMethod.DIRECT_ACCESS_BASE,
				MemoryLocation.GLOBAL_VARIABLE_BLOCK);
	}
	
	private static AllocationStrategy BoxScopeStrategy() {
		return new PositiveAllocationStrategy(
				MemoryAccessMethod.DOUBLE_INDIRECT_ACCESS_BASE,
				MemoryLocation.STACK_POINTER,
				12);
	}
	
	private static AllocationStrategy procedureScopeStrategy() {
		return new NegativeAllocationStrategy(
				MemoryAccessMethod.INDIRECT_ACCESS_BASE,
				MemoryLocation.FRAME_POINTER,
				-8);
	}
	
	private static AllocationStrategy parameterScopeStrategy() {
		return new NegativeAllocationStrategy(
				MemoryAccessMethod.INDIRECT_ACCESS_BASE,
				MemoryLocation.FRAME_POINTER);
	}
	
//////////////////////////////////////////////////////////////////////
// private constructor.	
	private Scope(int staticNestingLevel, AllocationStrategy allocator, Scope baseScope) {
		super();
		this.baseScope = (baseScope == null) ? this : baseScope;
		this.staticNestingLevel = staticNestingLevel;
		this.symbolTable = new SymbolTable();
		
		this.allocator = allocator;
		allocator.saveState();
	}
	
///////////////////////////////////////////////////////////////////////
//  basic queries	
	public int getStaticNestingLevel() {
		return staticNestingLevel;
	}
	public Scope getBaseScope() {
		return baseScope;
	}
	public SymbolTable getSymbolTable() {
		return symbolTable;
	}
	
///////////////////////////////////////////////////////////////////////
//memory allocation
	// must call leave() when destroying a scope.
	public void leave() {
		allocator.restoreState();
	}
	public int getAllocatedSize() {
		return allocator.getMaxAllocatedSize();
	}
	public void resetAllocatedSize() {
		 allocator.resetOffset();
	}

///////////////////////////////////////////////////////////////////////
//bindings
	public Binding createBinding(IdentifierNode identifierNode, Type type) {
		Token token = identifierNode.getToken();
		symbolTable.errorIfAlreadyDefined(token);

		String lexeme = token.getLexeme();
		Binding binding = allocateNewBinding(type, token.getLocation(), lexeme);	
		symbolTable.install(lexeme, binding);

		return binding;
	}
	private Binding allocateNewBinding(Type type, TextLocation textLocation, String lexeme) {
		MemoryLocation memoryLocation = allocator.allocate(type.getSize());
		return new Binding(type, textLocation, memoryLocation, lexeme);
	}
	
///////////////////////////////////////////////////////////////////////
//toString
	public String toString() {
		String result = "scope: static nesting level " + staticNestingLevel;
		result += " hash "+ hashCode() + "\n";
		result += symbolTable;
		return result;
	}

////////////////////////////////////////////////////////////////////////////////////
//Null Scope object
	public static Scope nullInstance() {
		return NullScope.getInstance();
	}
	private static class NullScope extends Scope {
		private static final int NULL_SCOPE_NESTING_LEVEL = -1;
		private static NullScope instance = null;

		private NullScope() {
			super(NULL_SCOPE_NESTING_LEVEL,
					new PositiveAllocationStrategy(MemoryAccessMethod.NULL_ACCESS, "", 0),
					null);
		}
		public String toString() {
			return "scope: the-null-scope";
		}
		@Override
		public Binding createBinding(IdentifierNode identifierNode, Type type) {
			unscopedIdentifierError(identifierNode.getToken());
			return super.createBinding(identifierNode, type);
		}
		public static Scope getInstance() {
			if(instance==null)
				instance = new NullScope();
			return instance;
		}
	}


///////////////////////////////////////////////////////////////////////
//error reporting
	private static void unscopedIdentifierError(Token token) {
		JuncoLogger log = JuncoLogger.getLogger("complier.scope");
		log.severe("variable " + token.getLexeme() + 
				" used outside of any scope at " + token.getLocation());
	}

}
