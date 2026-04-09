package corpusGenerator;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class PathsOfSourceFiles {

	/**
	 * Generates a text file listing the paths of all source files in the given directory.
	 *
	 * @param codeDirectoryPath Path to the directory containing source code files.
	 * @param outputPath        Path to the output text file that will list all source file paths.
	 * @throws IOException If an I/O error occurs.
	 */
	public static void generatePathsToFile(String codeDirectoryPath, String outputPath) throws IOException {
		Path start = Paths.get(codeDirectoryPath);

		// Use try-with-resources to ensure that the stream and writer are closed after use
		try (Stream<Path> stream = Files.walk(start);
			 FileWriter writer = new FileWriter(outputPath)) {

			stream
					// Filter to only include .java files
					.filter(path -> path.toString().endsWith(".java"))
					// Map Path objects to their string representations
					.map(Path::toString)
					// Write each file path to the output file
					.forEach(filePath -> {
						try {
							writer.write(filePath + System.lineSeparator());
						} catch (IOException e) {
							e.printStackTrace();
						}
					});
		}
	}

	// Main method for standalone usage
	public static void main(String[] args) {
		try {
			if (args.length < 2) {
				System.out.println("Usage: java corpusGenerator.PathsOfSourceFiles <source code path> <output file path>");
				return;
			}
			generatePathsToFile(args[0], args[1]);
			System.out.println("Paths of .java files have been written to " + args[1]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
