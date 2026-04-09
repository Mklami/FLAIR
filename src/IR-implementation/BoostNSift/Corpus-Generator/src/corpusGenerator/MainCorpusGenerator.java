package corpusGenerator;

import org.eclipse.jdt.core.dom.CompilationUnit;
import java.nio.file.Paths;

import java.io.IOException;

public class MainCorpusGenerator {

  /**
   * Generates the corpus based on the provided granularity level.
   *
   * @param granularity    The level of granularity (method, class, or file level).
   * @param sourceFilesTxt The path to the input text file listing source file paths.
   * @param workingDir     The working directory where output files will be saved.
   * @param project        The project name, used as a prefix for output files.
   * @throws Exception If an error occurs during processing.
   */
  public static void generateMainCorpus(String granularity, String sourceFilesTxt, String workingDir, String project) throws Exception {
    if (granularity == null || sourceFilesTxt == null || workingDir == null || project == null) {
      printUsage();
      return; // Stop execution or throw an exception as per your application's error handling policy
    }
    System.out.println("granularity debug for MainCorpusGenerator: " + granularity);
    switch (granularity) {
      case "methodLevelGranularity":
        generateForMethodLevelGranularity(sourceFilesTxt, workingDir, project);
        break;
      case "classLevelGranularity":
        generateForClassLevelGranularity(sourceFilesTxt, workingDir, project);
        break;
      case "fileLevelGranularity":
        generateForFileLevelGranularity(sourceFilesTxt, workingDir, project);
        break;
      default:
        throw new IllegalArgumentException("Invalid granularity level: " + granularity);
    }
  }

  private static void printUsage() {
    System.err.println("Generates a corpus from the source code at different levels of granularity.");
    System.err.println("Usage:");
    System.err.println("  java -jar CorpusGenerator.jar -methodLevelGranularity inputFileNameWithListOfInputFileNames outputFolder outputFileNameWithoutExtension");
    System.err.println("  java -jar CorpusGenerator.jar -classLevelGranularity inputFileNameWithListOfInputFileNames outputFolder outputFileNameWithoutExtension");
    System.err.println("  java -jar CorpusGenerator.jar -fileLevelGranularity inputFileNameWithListOfInputFileNames outputFolder outputFileNameWithoutExtension");
    System.err.println();
    System.err.println("Where:");
    System.err.println("  inputFileNameWithListOfInputFileNames");
    System.err.println("    is a file name containing n lines. Each line is a full path of a java file to be analyzed.");
    System.err.println("  outputFolder");
    System.err.println("    is the folder name where the corpus will be saved");
    System.err.println("  outputFileNameWithoutExtension");
    System.err.println("    the prefix of the output files (e.g., the name of the software system)");
    System.err.println();
    System.err.println("The output produced by this tool using the -methodLevelGranularity option will contain 4 files:");
    System.err.println("  outputFolder/outputFileNameWithoutExtension.corpusRawMethodLevelGranularity");
    System.err.println("    contains the corpus where each method extracted from the java files is on its own line");
    System.err.println("  outputFolder/outputFileNameWithoutExtension.corpusMappingMethodLevelGranularity");
    System.err.println("    contains the id of the method from the corpus on its own line (e.g., packageName.className.methodName)");
    System.err.println("  outputFolder/outputFileNameWithoutExtension.corpusMappingWithPackageSeparatorMethodLevelGranularity");
    System.err.println("    contains the id of the method from the corpus on its own line, with a separator character ('$') between package and class name (e.g., packageName$className.methodName)");
    System.err.println("  outputFolder/outputFileNameWithoutExtension.corpusRawAndMappingDebugMethodLevelGranularity");
    System.err.println("    contains some verbose information about the corpus extraction (for verification purposes only)");
    System.err.println();
    System.err.println("The output produced by this tool using the -classLevelGranularity option will contain 3 files:");
  }

  private static void generateForMethodLevelGranularity(String sourceFilesTxt, String workingDir, String project) throws IOException {
    // Assume InputOutputCorpusMethodLevelGranularity is properly implemented
    System.out.println("granularity debug for generateForMethodLevelGranularity: " + sourceFilesTxt);
    InputOutputCorpusMethodLevelGranularity inputOutput = new InputOutputCorpusMethodLevelGranularity(sourceFilesTxt, workingDir + "/Output/methodLevelGranularity/", "Corpus-" + project);
    try {
      parseAndSaveMultipleFiles(inputOutput);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void generateForClassLevelGranularity(String sourceFilesTxt, String workingDir, String project) throws IOException {
    // Assume InputOutputCorpusClassLevelGranularity is properly implemented
    InputOutputCorpusClassLevelGranularity inputOutput = new InputOutputCorpusClassLevelGranularity(sourceFilesTxt, workingDir + "/Output/classLevelGranularity/", "Corpus-" + project);
    try {
      parseAndSaveMultipleFilesClassLevelGranularity(inputOutput);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void generateForFileLevelGranularity(String sourceFilesTxt, String workingDir, String project) throws IOException {
    // Assume InputOutputCorpusFileLevelGranularity is properly implemented
    InputOutputCorpusFileLevelGranularity inputOutput = new InputOutputCorpusFileLevelGranularity(sourceFilesTxt, workingDir + "/Output/fileLevelGranularity/", "Corpus-" + project);
    try {
      parseAndSaveMultipleFilesFileLevelGranularity(inputOutput);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void parseAndSaveMultipleFiles(InputOutputCorpusMethodLevelGranularity inputOutput) throws Exception {
    System.out.println("Starting to parse and save multiple files");
    inputOutput.initializeOutputStream();
    String[] inputFileNames = InputOutput.readFile(inputOutput.getInputFileNameWithListOfInputFileNames()).split("\r\n");
    for (String inputFileName : inputFileNames) {
      inputOutput.appendToCorpusDebug("Preprocessing file:\t" + inputFileName);
      parseAndSaveOneFile(inputOutput, inputFileName);
    }
    inputOutput.closeOutputStreams();
    System.out.println("Finished parsing and saving multiple files");
  }

  private static void parseAndSaveOneFile(InputOutputCorpusMethodLevelGranularity inputOutput, String inputFileName) {
    String fileContent = "";
    fileContent = InputOutput.readFile(inputFileName);
    ParserCorpusMethodLevelGranularity parser = new ParserCorpusMethodLevelGranularity(inputOutput, fileContent);
    CompilationUnit compilationUnitSourceCode = parser.parseSourceCode();
    parser.exploreSourceCode(compilationUnitSourceCode);
  }

  private static void parseAndSaveMultipleFilesClassLevelGranularity(InputOutputCorpusClassLevelGranularity inputOutput) throws Exception {
    inputOutput.initializeOutputStream();
    String[] inputFileNames = InputOutput.readFile(inputOutput.getInputFileNameWithListOfInputFileNames()).split("\r\n");
    for (String inputFileName : inputFileNames) {
      inputOutput.appendToCorpusDebug("Preprocessing file:\t" + inputFileName);
      parseAndSaveOneFileClassLevelGranularity(inputOutput, inputFileName);
    }
    inputOutput.closeOutputStreams();
  }

  private static void parseAndSaveOneFileClassLevelGranularity(InputOutputCorpusClassLevelGranularity inputOutput, String inputFileName) {
    String fileContent = "";
    fileContent = InputOutput.readFile(inputFileName);
    ParserCorpusClassLevelGranularity parser = new ParserCorpusClassLevelGranularity(inputOutput, fileContent);
    CompilationUnit compilationUnitSourceCode = parser.parseSourceCode();
    parser.exploreSourceCodeClassLevelGranularity(compilationUnitSourceCode);
  }

  private static void parseAndSaveMultipleFilesFileLevelGranularity(InputOutputCorpusFileLevelGranularity inputOutput) throws Exception {
    inputOutput.initializeOutputStream();
    String[] inputFileNames = InputOutput.readFile(inputOutput.getInputFileNameWithListOfInputFileNames()).split("\r\n");
    for (String inputFileName : inputFileNames) {
      inputOutput.appendToCorpusDebug("Preprocessing file:\t" + inputFileName);
      parseAndSaveOneFileFileLevelGranularity(inputOutput, inputFileName);
    }
    inputOutput.closeOutputStreams();
  }

  private static void parseAndSaveOneFileFileLevelGranularity(InputOutputCorpusFileLevelGranularity inputOutput, String inputFileName) {
    String fileContent = "";
    fileContent = InputOutput.readFile(inputFileName);
    ParserCorpusFileLevelGranularity parser = new ParserCorpusFileLevelGranularity(inputOutput, fileContent, inputFileName);
    CompilationUnit compilationUnitSourceCode = parser.parseSourceCode();
    parser.exploreSourceCodeFileLevelGranularity(compilationUnitSourceCode);
  }

  static void testMainMethodLevelGranularity() throws Exception {
    String workFolder = "TestCases/";
    String[] listOfSystems = {"System1", "System2", "System3", "jEdit4.3"};
    int i = 0;
    for (String systemName : listOfSystems) {
      System.out.println("Current system=" + systemName);
      InputOutputCorpusMethodLevelGranularity inputOutput = new InputOutputCorpusMethodLevelGranularity(workFolder + "Input/inputFileNames" + listOfSystems[i] + ".txt", workFolder + "Output/methodLevelGranularity/", "Corpus-" + listOfSystems[i]);
      i++;
      parseAndSaveMultipleFiles(inputOutput);
    }
  }

  static void testMainClassLevelGranularity() throws Exception {
    String workFolder = "TestCases/";
    String[] listOfSystems = {"System1", "System2", "System3", "jEdit4.3"};
    int i = 0;
    for (String systemName : listOfSystems) {
      System.out.println("Current system=" + systemName);
      InputOutputCorpusClassLevelGranularity inputOutput = new InputOutputCorpusClassLevelGranularity(workFolder + "Input/inputFileNames" + listOfSystems[i] + ".txt", workFolder + "Output/classLevelGranularity/", "Corpus-" + listOfSystems[i]);
      i++;
      parseAndSaveMultipleFilesClassLevelGranularity(inputOutput);
    }
  }

  static void testMainFileLevelGranularity() throws Exception {
    String workFolder = "TestCases/";
    String[] listOfSystems = {"System1", "System2", "System3", "jEdit4.3"};
    int i = 0;
    for (String systemName : listOfSystems) {
      System.out.println("Current system=" + systemName);
      InputOutputCorpusFileLevelGranularity inputOutput = new InputOutputCorpusFileLevelGranularity(workFolder + "Input/inputFileNames" + listOfSystems[i] + ".txt", workFolder + "Output/fileLevelGranularity/", "Corpus-" + listOfSystems[i]);
      i++;
      parseAndSaveMultipleFilesFileLevelGranularity(inputOutput);
    }
  }

  public static void main(String[] args) throws Exception {
    testMainMethodLevelGranularity();
    testMainClassLevelGranularity();
    testMainFileLevelGranularity();

    args = new String[4];
    if (args.length != 4) {
      printUsage();
      return;
    }
    generateMainCorpus(args[0], args[1], args[2], args[3]);
  }
}

