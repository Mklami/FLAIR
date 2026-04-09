package boostNsift;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class BoostNSift {

    private static int counter = 1;

    private static final HashMap<Integer, Float> grandResults = new HashMap<>();

    public static void runBoostNSift(String workingDir, String corpusFilePath, String titlesFilePath, String descriptionsFilePath, String commentsFilePath, String resultsFilePath) throws IOException, ParseException {
        FileUtils.cleanDirectory(new File(workingDir)); // Clean the index directory
        Analyzer analyzer = new StandardAnalyzer();
        Directory directory = FSDirectory.open(new File(workingDir).toPath());
        createIndex(directory, corpusFilePath, analyzer); // Step 1: Create a local directory index

        DirectoryReader ireader = DirectoryReader.open(directory);
        IndexSearcher isearcher = setupSearcher(ireader, analyzer); // Step 2: Initialize the IR algorithm and Query Parser

        List<String> titles = readFileLines(titlesFilePath);
        List<String> descriptions = readFileLines(descriptionsFilePath);
        List<String> comments = readFileLines(commentsFilePath);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(resultsFilePath)))) {
            processQueries(isearcher, analyzer, titles, descriptions, comments, writer); // Steps 3-5: Process and search queries
        } finally {
            ireader.close();
            directory.close();
        }
    }

    public static void main(String[] args) throws IOException, ParseException {
        if (args.length < 6) {
            System.out.println("Usage: java BoostNSift <workingDir> <corpusFilePath> <titlesFilePath> <descriptionsFilePath> <commentsFilePath> <resultsFilePath>");
            return;
        }
        runBoostNSift(args[0], args[1], args[2], args[3], args[4], args[5]);
    }

    private static void createIndex(Directory directory, String corpusFilePath, Analyzer analyzer) throws IOException {
        IndexWriter iwriter = null;
        try {
            iwriter = new IndexWriter(directory, new IndexWriterConfig(analyzer));
            //check if corpus file exists
            File file = new File(corpusFilePath);
            counter = 1;
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
                String text;
                while ((text = bufferedReader.readLine()) != null) {
                    Document doc = new Document();
                    doc.add(new TextField("method_id", Integer.toString(counter), Field.Store.YES));
                    doc.add(new TextField("method_contents", text, Field.Store.YES));
                    iwriter.addDocument(doc);
                    counter++;
                }
            }
            System.out.println("Documents indexed: " + counter);
        } finally {
            if (iwriter != null) {
                iwriter.close();
            }
        }
    }

    private static IndexSearcher setupSearcher(DirectoryReader ireader, Analyzer analyzer) {
        try {
            IndexSearcher isearcher = new IndexSearcher(ireader);
            float k1 = 1.0f, b = 0.3f;
            BM25Similarity similarity = counter > 3000 ? new BM25Similarity(k1, b) : new BM25Similarity(0.0f, b);
            isearcher.setSimilarity(similarity);
            return isearcher;
        } catch (Exception e) {
            System.out.println("Error setting up the searcher: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static List<String> readFileLines(String filePath) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(filePath)))) {
            try {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    lines.add(line.trim());
                }
            }
            catch (IOException e) {
                System.out.println("Error reading file: " + e.getMessage());
                System.out.println("File path: " + filePath);
                e.printStackTrace();
            }
        }
        return lines;
    }

    private static void processQueries(IndexSearcher isearcher, Analyzer analyzer, List<String> titles, List<String> descriptions, List<String> comments, BufferedWriter writer) throws IOException, ParseException {
        QueryParser parser = new QueryParser("method_contents", analyzer);
        BooleanQuery.setMaxClauseCount(900000000);
        ArrayList<String> queries = new ArrayList<>();
        String aggregatedQuery = "";
        try {
            for (int i = 0; i < descriptions.size(); i++) {
                String title = titles.get(i);
                String description = descriptions.get(i);
                String comment = comments.get(i);
                BooleanQuery.setMaxClauseCount(900000000);
                Query query;
                if (!title.trim().equals("")) {
                    query = parser.parse(title);
                    title = query.toString().trim().replaceAll(" ", "^1 ");
                }
                if (!description.trim().equals("")) {
                    query = parser.parse(description);
                    description = query.toString().trim().replaceAll(" ", "^1 ");
                }
                if (!comment.trim().equals("")) {
                    query = parser.parse(comment);
                    comment = query.toString().trim().replaceAll(" ", "^1 ");
                }
                description = title.trim() + " " + description.trim() + " " + comment.trim();
                queries.add(description);
                aggregatedQuery += description + " ";
            }
        } catch (Exception e) {
            System.out.println("Error processing queries: " + e.getMessage());
            e.printStackTrace();
        }

        Query quToSearch = parser.parse(aggregatedQuery);
        int maxDoc = isearcher.getIndexReader().maxDoc();
        TopDocs topDocs = isearcher.search(quToSearch, maxDoc);
        ScoreDoc[] nhits = topDocs.scoreDocs;

        for (ScoreDoc hit : nhits) {
            Document hitDoc = isearcher.doc(hit.doc);
            grandResults.put(Integer.parseInt(hitDoc.get("method_id")), hit.score / nhits[0].score);
        }

        try {
            for (int i = 0; i < queries.size(); i++) {
                System.out.println("Processing query " + (i + 1));
                System.out.println("Title: " + titles.get(i));
                System.out.println("Description: " + descriptions.get(i));
                System.out.println("Comment: " + comments.get(i));

                Query queryToSearch = parser.parse(queries.get(i));
                int maxDocForQuery = isearcher.getIndexReader().maxDoc();
                TopDocs topDocsForQuery = isearcher.search(queryToSearch, maxDocForQuery);
                ScoreDoc[] hits = topDocsForQuery.scoreDocs;

                for (ScoreDoc hit : hits) {
                    Document hitDoc = isearcher.doc(hit.doc);
                    if (hitDoc.get("method_contents").length() < 11) continue;
                    Float grandResultScore = grandResults.get(Integer.parseInt(hitDoc.get("method_id")));
                    if (grandResultScore != null && grandResultScore > (hit.score / hits[0].score)) continue;
                    writer.write(hitDoc.get("method_id") + "," + (hit.score / hits[0].score) + ",");
                }
                writer.newLine();
                writer.flush();
            }
        } catch (Exception e) {
            System.out.println("Error processing queries: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
