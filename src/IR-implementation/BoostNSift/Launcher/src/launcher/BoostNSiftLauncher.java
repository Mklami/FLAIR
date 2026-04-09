package launcher;

import java.io.*;
import java.nio.file.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import org.apache.commons.lang3.StringEscapeUtils;
import java.util.ArrayList;
import java.util.List;
import analysis.BoostNSiftEvaluation;

public class BoostNSiftLauncher {
    private String project;
    private String workingDir;
    private String runDir;
    private String report;
    private String code;
    private String granularity;
    private String sourceFilesTxt;
    private String titleFilesCsv;
    private String descriptionFilesCsv;
    private String commentFilesCsv;
    private String corpusFilesCsv;
    private String resultsCsv;
    private String corpusPreProcessOutput;
    private String titlePreProcessOutput;
    private String descriptionPreProcessOutput;
    private String commentPreProcessOutput;
    private String titlePreProcessOutputFile;
    private String descriptionPreProcessOutputFile;
    private String commentPreProcessOutputFile;
    private String answerMatrixCsv;
    private String corpusRawPath;
    private String corpusMappingPath;
    private String answerFilesCsv;

    public BoostNSiftLauncher(String project, String workingDir, String report, String code, String granularity)  {
        this.project = project;
        this.workingDir = workingDir;
        this.runDir = workingDir + "/boostNsift_run";
        this.report = report;
        this.code = code;
        this.granularity = granularity;
        this.sourceFilesTxt = workingDir + "/sourceFiles.txt";
        this.titleFilesCsv = workingDir + "/titleFiles.csv";
        this.descriptionFilesCsv = workingDir + "/descriptionFiles.csv";
        this.commentFilesCsv = workingDir + "/commentFiles.csv";
        this.corpusFilesCsv = workingDir + "/corpusFiles.csv";
        this.titlePreProcessOutputFile = workingDir + "/titlePreProcessOutput.csv";
        this.descriptionPreProcessOutputFile = workingDir + "/descriptionPreProcessOutput.csv";
        this.commentPreProcessOutputFile = workingDir + "/commentPreProcessOutput.csv";
        this.answerFilesCsv = workingDir + "/answerFiles.csv";
        this.resultsCsv = workingDir + "/" + project + "_results.csv";
        this.corpusPreProcessOutput = workingDir + "/corpusPreProcessOutput";
        this.titlePreProcessOutput = workingDir + "/titlePreProcessOutput";
        this.descriptionPreProcessOutput = workingDir + "/descriptionPreProcessOutput";
        this.commentPreProcessOutput = workingDir + "/commentPreProcessOutput";
        this.answerMatrixCsv = workingDir + "/answerMatrix.csv";
        this.corpusRawPath = workingDir + "/Output/fileLevelGranularity/Corpus-" + project + ".corpusRawFileLevelGranularity";
        this.corpusMappingPath = workingDir + "/Output/fileLevelGranularity/Corpus-" + project + ".corpusMappingFileLevelGranularity";
        System.out.println("Project: " + project);
        System.out.println("Working Directory: " + workingDir);
        System.out.println("Report: " + report);
        System.out.println("Code: " + code);
        System.out.println("Granularity: " + granularity);
        launch();
    }

    private void launch() {
        try {
            System.out.println("Preparing the dataset for the project: " + project + "...");
            prepareDataset();
            System.out.println("Running the BoostNSift analysis...");
            runBoostNSift();
            System.out.println("Evaluating the BoostNSift analysis...");
            evaluateBoostNSift();
        } catch (IOException e) {
            System.out.println("Error while launching BoostNSift: " + e.getMessage());
        }
    }

    private void prepareDataset() {
        try {
            System.out.println("Generating the Corpus...");
            generateCorpus();
            System.out.println("Generating the Queries...");
            generateQueries();
            System.out.println("Preprocessing the Queries and Corpus...");
            preprocessQueriesAndCorpus();
            System.out.println("Generating the Answer Matrix...");
            generateAnswerMatrix();
        } catch (Exception e) {
            System.out.println("Error while preparing the dataset: " + e.getMessage());
        }
    }

    private void generateCorpus() throws IOException {
        corpusGenerator.PathsOfSourceFiles.generatePathsToFile(code, sourceFilesTxt);

        // Check if the sourceFiles.txt file was generated successfully
        if (!Files.exists(Paths.get(sourceFilesTxt))) {
            throw new IOException("Error while generating the sourceFiles.txt file");
        }

        try {
            corpusGenerator.MainCorpusGenerator.generateMainCorpus(granularity, sourceFilesTxt, workingDir, project);
        } catch (Exception e) {
            System.err.println("Error generating the corpus: " + e.getMessage());
            throw new IOException(e);
        }

    }

    private void generateQueries() throws Exception {
        File xmlFile = new File(report);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        NodeList bugList = doc.getElementsByTagName("bug");
        int numBugs = bugList.getLength();

        // Initialize lists for titles, descriptions, and comments
        List<String> titles = new ArrayList<>(numBugs);
        List<String> descriptions = new ArrayList<>(numBugs);
        List<String> comments = new ArrayList<>(numBugs);
        List<String> fixedFileList = new ArrayList<>(numBugs);
        for (int i = 0; i < numBugs; i++) {
            titles.add("");
            descriptions.add("");
            comments.add("");
            fixedFileList.add("");
        }

        for (int i = 0; i < numBugs; i++) {
            Node bugNode = bugList.item(i);
            if (bugNode.getNodeType() == Node.ELEMENT_NODE) {
                Element bugElement = (Element) bugNode;

                String summary = removeHtml(bugElement.getElementsByTagName("summary").item(0).getTextContent());
                String description = removeHtml(bugElement.getElementsByTagName("description").item(0).getTextContent());

                NodeList commentNodes = bugElement.getElementsByTagName("comments");
                StringBuilder commentsBuilder = new StringBuilder();
                for (int j = 0; j < commentNodes.getLength(); j++) {
                    commentsBuilder.append(removeHtml(commentNodes.item(j).getTextContent())).append(" ");
                }

                NodeList fixedFiles = bugElement.getElementsByTagName("fixedFiles");
                StringBuilder fixedFilesBuilder = new StringBuilder();
                for (int j = 0; j < fixedFiles.getLength(); j++) {
                    Element fixedFilesElement = (Element) fixedFiles.item(j);
                    NodeList fileNodes = fixedFilesElement.getElementsByTagName("file");
                    for (int k = 0; k < fileNodes.getLength(); k++) {
                        //Save the get text content of the file nodes first to a variable
                        String fileNodeTextContent = fileNodes.item(k).getTextContent();
                        //check if it ends with .java
                        if (fileNodeTextContent.endsWith(".java")) {
                            fixedFilesBuilder.append(fileNodeTextContent).append(" ");
                        }
                    }
                }

                titles.set(i, summary);
                descriptions.set(i, description);
                comments.set(i, commentsBuilder.toString().trim());
                fixedFileList.set(i, fixedFilesBuilder.toString().trim());
            }
        }

        // Write to files
        try (FileWriter titleWriter = new FileWriter(titleFilesCsv, true);
             FileWriter descriptionWriter = new FileWriter(descriptionFilesCsv, true);
             FileWriter commentWriter = new FileWriter(commentFilesCsv, true);
             FileWriter answerWriter = new FileWriter(answerFilesCsv, true);) {

            for (int i = 0; i < numBugs; i++) {
                titleWriter.write(titles.get(i) + "\n");
                descriptionWriter.write(descriptions.get(i) + "\n");
                commentWriter.write(comments.get(i) + "\n");
                answerWriter.write(fixedFileList.get(i) + "\n");
            }
        }
    }

    private String removeHtml(String htmlText) {
        String textOnly = StringEscapeUtils.unescapeHtml4(htmlText);
        textOnly = textOnly.replaceAll("<[^>]*>", "");
        return textOnly;
    }


    private void preprocessQueriesAndCorpus() throws IOException {
        // Preprocess the corpus
        corpusPreprocessor.MainCorpusPreprocessor.preprocessCorpus(corpusRawPath, corpusPreProcessOutput);

        File corpusPreProcessOutputFile = new File(corpusPreProcessOutput);
        corpusPreprocessor.FolderToFile.displayDirectoryContents(corpusFilesCsv, corpusPreProcessOutputFile);

        // Check if the corpus preprocessing output file is created successfully
        if (!Files.exists(Paths.get(corpusPreProcessOutput))) {
            throw new IOException("Error: corpusPreProcessOutput file is not created successfully.");
        }

        // Preprocess title files
        corpusPreprocessor.MainCorpusPreprocessor.preprocessCorpus(titleFilesCsv, titlePreProcessOutput);

        // Check if the title preprocessing output file is created successfully
        if (!Files.exists(Paths.get(titlePreProcessOutput))) {
            throw new IOException("Error: titlePreProcessOutput file is not created successfully.");
        }


        File titlePreProcessDirectory = new File(titlePreProcessOutput);
        corpusPreprocessor.FolderToFile.displayDirectoryContents(titlePreProcessOutputFile, titlePreProcessDirectory);

        // Preprocess description files
        corpusPreprocessor.MainCorpusPreprocessor.preprocessCorpus(descriptionFilesCsv, descriptionPreProcessOutput);

        // Check if the description preprocessing output file is created successfully
        if (!Files.exists(Paths.get(descriptionPreProcessOutput))) {
            throw new IOException("Error: descriptionPreProcessOutput file is not created successfully.");
        }

        File descriptionPreProcessDirectory = new File(descriptionPreProcessOutput);
        corpusPreprocessor.FolderToFile.displayDirectoryContents(descriptionPreProcessOutputFile, descriptionPreProcessDirectory);

        // Preprocess comment files
        corpusPreprocessor.MainCorpusPreprocessor.preprocessCorpus(commentFilesCsv, commentPreProcessOutput);

        // Check if the comment preprocessing output file is created successfully
        if (!Files.exists(Paths.get(commentPreProcessOutput))) {
            throw new IOException("Error: commentPreProcessOutput file is not created successfully.");
        }

        File commentPreProcessDirectory = new File(commentPreProcessOutput);
        corpusPreprocessor.FolderToFile.displayDirectoryContents(commentPreProcessOutputFile, commentPreProcessDirectory);

    }

    private void generateAnswerMatrix() {
        File answerFilesCsvFile = new File(answerFilesCsv);
        File corpusMappingPathFile = new File(corpusMappingPath);
        try {
            // Read the file lines of both files by not calling the readFileLines method
            List<String> answerFiles = new ArrayList<>();
            List<String> corpusMapping = new ArrayList<>();
            try (BufferedReader answerFilesReader = new BufferedReader(new FileReader(answerFilesCsvFile));
                 BufferedReader corpusMappingReader = new BufferedReader(new FileReader(corpusMappingPathFile))) {
                String line;
                while ((line = answerFilesReader.readLine()) != null) {
                    answerFiles.add(line);
                }
                while ((line = corpusMappingReader.readLine()) != null) {
                    corpusMapping.add(line);
                }
            } catch (IOException e) {
                System.err.println("Error reading the answer files or corpus mapping: " + e.getMessage());
            }
            try (BufferedWriter answerMatrixWriter = new BufferedWriter(new FileWriter(answerMatrixCsv))) {
                for (int i = 0; i < answerFiles.size(); i++) {
                    String[] fixedFiles = answerFiles.get(i).split(" ");
                    List<String> indices = new ArrayList<>();
                    for (String fixedFile : fixedFiles) {
                        String exactPath = "/app/data/gitrepo/" + fixedFile;
                        int lineNumber = corpusMapping.indexOf(exactPath);
                        if (lineNumber == -1) {
                            System.err.println("Error: Could not find the fixed file in the corpus mapping: " + fixedFile);
                            continue;
                        }
                        indices.add(String.valueOf(lineNumber + 1));
                    }
                    if (!indices.isEmpty()) {
                        answerMatrixWriter.write(String.join(",", indices));
                    }
                    answerMatrixWriter.write("\n");
                }
            } catch (IOException e) {
                System.err.println("Error writing to the answer matrix file: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error generating the answer matrix: " + e.getMessage());
        }
    }


    private void runBoostNSift() throws IOException {
        try {
            //check if corpusFilesCsv, titleFilesCsv etc. created successfuly first:
            if (!Files.exists(Paths.get(corpusFilesCsv))) {
                throw new IOException("Error: corpusFilesCsv file is not created successfully.");
            }
            if (!Files.exists(Paths.get(titlePreProcessOutputFile))) {
                throw new IOException("Error: titleFilesCsv file is not created successfully.");
            }
            if (!Files.exists(Paths.get(descriptionPreProcessOutputFile))) {
                throw new IOException("Error: descriptionFilesCsv file is not created successfully.");
            }
            if (!Files.exists(Paths.get(commentPreProcessOutputFile))) {
                throw new IOException("Error: commentFilesCsv file is not created successfully.");
            }
            File runDirFile = new File(runDir);
            if (!runDirFile.exists()) {
                runDirFile.mkdir();
            }
            boostNsift.BoostNSift.runBoostNSift(
                    runDir,
                    corpusFilesCsv,
                    titlePreProcessOutputFile,
                    descriptionPreProcessOutputFile,
                    commentPreProcessOutputFile,
                    resultsCsv);
            // Check if the BoostNSift results file is created successfully
            Path resultsPath = Paths.get(resultsCsv);
            if (!Files.exists(resultsPath)) {
                throw new IOException("Error: BoostNSift results file is not created successfully.");
            }
        } catch (Exception e) {
            // This catches both IOExceptions and ParseExceptions thrown by BoostNSift
            System.err.println("Error running BoostNSift: " + e.getMessage());
            throw new IOException(e); // Rethrow as an IOException or handle it as needed
        }
    }

    private void evaluateBoostNSift() {
        try {
            int totalMethods = (int) Files.lines(Paths.get(corpusRawPath)).count();
            System.out.println("Total number of methods: " + totalMethods);
            try {
                new BoostNSiftEvaluation(resultsCsv, answerMatrixCsv, totalMethods, workingDir, project).evaluate();
            } catch (Exception e) {
                System.err.println("Error evaluating BoostNSift: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Error calculating the total number of methods: " + e.getMessage());
        }
    }

}