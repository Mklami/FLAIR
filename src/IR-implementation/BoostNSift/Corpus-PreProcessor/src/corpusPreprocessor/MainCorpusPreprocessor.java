package corpusPreprocessor;


import corpusPreprocessor.CorpusPreprocessor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainCorpusPreprocessor {
  /**
   * Preprocesses the given corpus or query files and generates preprocessed output.
   *
   * @param inputFileName The full path to the input file (corpus or query CSV).
   * @param outputFolder The directory where the preprocessed output will be saved.
   * @throws IOException If there is an error in file operations.
   */
  public static void preprocessCorpus(String inputFileName, String outputFolder) throws IOException {
    // Assuming CorpusPreprocessor has a constructor that accepts an input file name and an output folder
    CorpusPreprocessor corpusPreprocessor = new CorpusPreprocessor(inputFileName, outputFolder);
    try {
        corpusPreprocessor.preprocessCorpus();
    }
    catch (Exception e) {
        e.printStackTrace();
    }

    // Example check to see if the output was generated successfully
    Path outputPath = Paths.get(outputFolder);
    if (Files.notExists(outputPath)) {
      throw new IOException("Error: Preprocess output file is not created successfully at " + outputPath);
    }
  }

      public static void main(String[] args) {
     if (args.length < 2) {
        System.out.println("Usage: java MainCorpusPreprocessor <input file name> <output folder>");
        return;
     }

     String inputFileName = args[0];
     String outputFolder = args[1];

     try {
        preprocessCorpus(inputFileName, outputFolder);
     } catch (IOException e) {
        System.out.println("Error while preprocessing the corpus: " + e.getMessage());
     }
      }
}
