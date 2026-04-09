package corpusPreprocessor;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FolderToFile {
  public static void main(String[] args) throws Exception {
    File currentDir = new File("C:\\Users\\abdul.razzaq.UL\\Downloads\\NetML-master\\NetML-master\\data\\combined_method_text\\ant\\");
    displayDirectoryContents("C:\\Users\\abdul.razzaq.UL\\Downloads\\NetML-master\\NetML-master\\data\\combined_method_text\\ant\\ant.txt", currentDir);
  }
  
  public static String readFile(String path, Charset encoding) throws IOException {
    byte[] encoded = Files.readAllBytes(Paths.get(path, new String[0]));
    String content = new String(encoded, encoding);
    return content.replaceAll("\n", " ").replaceAll("  ", " ").trim();
  }
  
  public static void displayDirectoryContents(String newFileName, File dir) throws IOException {
    File newFile = new File(newFileName);
    BufferedWriter writer = new BufferedWriter(new FileWriter(newFile));
    try {
      File[] files = dir.listFiles();
      for (File file : files) {
        if (file.isDirectory()) {
          displayDirectoryContents(newFileName, file);
        } else {
          String content = readFile(file.getCanonicalPath(), StandardCharsets.UTF_8);
          writer.write(content + "\n");
          System.out.println("file:" + file.getCanonicalPath());
        }
      } 
      writer.flush();
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    } 
  }
}

