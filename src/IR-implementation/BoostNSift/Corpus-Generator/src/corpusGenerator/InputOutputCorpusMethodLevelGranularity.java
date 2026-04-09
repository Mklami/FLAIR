package corpusGenerator;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;

public class InputOutputCorpusMethodLevelGranularity extends InputOutput {
  public static final String EXTENSION_CORPUS_RAW = ".corpusRawMethodLevelGranularity";
  
  public static final String EXTENSION_CORPUS_MAPPING = ".corpusMappingMethodLevelGranularity";
  
  public static final String EXTENSION_CORPUS_MAPPING_WITH_PACKAGE_SEPARATOR = ".corpusMappingWithPackageSeparatorMethodLevelGranularity";
  
  public static final String EXTENSION_CORPUS_DEBUG = ".corpusRawAndMappingDebugMethodLevelGranularity";
  
  private String outputFileNameCorpusRaw;
  
  private String outputFileNameCorpusMapping;
  
  private String outputFileNameCorpusMappingWithPackageSeparator;
  
  private String outputFileNameCorpusRawAndMappingDebug;
  
  private BufferedWriter outputFileCorpusRaw;
  
  private BufferedWriter outputFileCorpusMapping;
  
  private BufferedWriter outputFileCorpusMappingWithPackageSeparator;
  
  private BufferedWriter outputFileCorpusRawAndMappingDebug;
  
  public String getOutputFileNameCorpusRaw() {
    return this.outputFileNameCorpusRaw;
  }
  
  public String getOutputFileNameCorpusMapping() {
    return this.outputFileNameCorpusMapping;
  }
  
  public String getOutputFileNameCorpusMappingWithPackageSeparator() {
    return this.outputFileNameCorpusMappingWithPackageSeparator;
  }
  
  public String getOutputFileNameCorpusRawAndMappingDebug() {
    return this.outputFileNameCorpusRawAndMappingDebug;
  }
  
    public InputOutputCorpusMethodLevelGranularity(String inputFileNameWithListOfInputFileNames, String outputFolderName, String outputFileNameWithoutExtension) throws IOException {
    super(inputFileNameWithListOfInputFileNames, outputFolderName, outputFileNameWithoutExtension);
    System.out.println("CorpusMethodLevelGranularity: inputFileNameWithListOfInputFileNames: " + inputFileNameWithListOfInputFileNames);
    System.out.println("CorpusMethodLevelGranularity: outputFolderName: " + outputFolderName);
    System.out.println("CorpusMethodLevelGranularity: outputFileNameWithoutExtension: " + outputFileNameWithoutExtension);
    this.outputFileNameCorpusRaw = outputFolderName + outputFileNameWithoutExtension + ".corpusRawMethodLevelGranularity";
    this.outputFileNameCorpusMapping = outputFolderName + outputFileNameWithoutExtension + ".corpusMappingMethodLevelGranularity";
    this.outputFileNameCorpusMappingWithPackageSeparator = outputFolderName + outputFileNameWithoutExtension + ".corpusMappingWithPackageSeparatorMethodLevelGranularity";
    this.outputFileNameCorpusRawAndMappingDebug = outputFolderName + outputFileNameWithoutExtension + ".corpusRawAndMappingDebugMethodLevelGranularity";
    File directory = new File(outputFolderName);

    // Check if the directory exists
    if (!directory.exists()) {
      // Attempt to create the directory and all necessary parent directories
      if (!directory.mkdirs()) {
        throw new IOException("Failed to create output directory: " + outputFolderName);
      }
    }
  }
  
  public void initializeOutputStream() throws Exception {
    this.outputFileCorpusRaw = new BufferedWriter(new FileWriter(this.outputFileNameCorpusRaw));
    this.outputFileCorpusMapping = new BufferedWriter(new FileWriter(this.outputFileNameCorpusMapping));
    this.outputFileCorpusMappingWithPackageSeparator = new BufferedWriter(new FileWriter(this.outputFileNameCorpusMappingWithPackageSeparator));
    this.outputFileCorpusRawAndMappingDebug = new BufferedWriter(new FileWriter(this.outputFileNameCorpusRawAndMappingDebug));
  }
  
  public void appendToCorpusMapping(String idMethod) {
    appendToFile(this.outputFileCorpusMapping, idMethod);
  }
  
  public void appendToCorpusMappingWithPackageSeparator(String idMethod) {
    appendToFile(this.outputFileCorpusMappingWithPackageSeparator, idMethod);
  }
  
  public void appendToCorpusRaw(String methodContent) {
    appendToFile(this.outputFileCorpusRaw, methodContent);
  }
  
  public void appendToCorpusDebug(String buf) {
    appendToFile(this.outputFileCorpusRawAndMappingDebug, buf);
  }
  
  public void closeOutputStreams() throws Exception {
    this.outputFileCorpusRaw.close();
    this.outputFileCorpusMapping.close();
    this.outputFileCorpusMappingWithPackageSeparator.close();
    this.outputFileCorpusRawAndMappingDebug.close();
  }
  
  public void printMessageWhereOutputFilesWereSaved() {
    System.out.println("CorpusMethodLevelGranularity: Corpus was saved to file: " + this.outputFileNameCorpusRaw);
    System.out.println("CorpusMethodLevelGranularity: Mapping was saved to file: " + this.outputFileNameCorpusMapping);
    System.out.println("CorpusMethodLevelGranularity: Mapping with package separator was saved to file: " + this.outputFileNameCorpusMappingWithPackageSeparator);
    System.out.println("CorpusMethodLevelGranularity: Corpus with debug information was saved to file: " + this.outputFileNameCorpusRawAndMappingDebug);
  }
}

