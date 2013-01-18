package applications;

public class JuncoApplication {

	private static final int EXIT_CODE_FOR_ERROR = 1;
	private static final String outputDirectory = "output/";

	public JuncoApplication() {
		super();
	}
	
	
	protected static void checkArguments(String[] args, String applicationName) {
		if(args.length != 1) {
			printUsageMessage(applicationName);
		}
	}
	private static void printUsageMessage(String applicationName) {
		System.err.println("usage: " + applicationName + " filename");
		System.err.println("    (use exactly one filename argument)");
		System.exit(EXIT_CODE_FOR_ERROR);
	}


	protected static String outputFilename(String filename) {
		return outputDirectory + basename(filename) + ".asm";
	}
	// removes preceding directory names and the file extension
	// e.g. /usr/root/tricks/bigBag.cpp  ->  bigBag
	private static String basename(String filename) {
		int lastSlash = filename.lastIndexOf('/');
		int lastBackslash = filename.lastIndexOf('\\');
		int start = Math.max(lastSlash, lastBackslash) + 1;
		
		int end = filename.indexOf('.', start);
		if(end == -1) {
			return filename.substring(start);
		}
		return filename.substring(start, end);
	}


}