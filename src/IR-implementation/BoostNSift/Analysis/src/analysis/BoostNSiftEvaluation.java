package analysis;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BoostNSiftEvaluation {
    private String resultsPath;
    private String answerMatrixPath;
    private int methods;
    private String workingDir;
    private String projectName;
    private String vsmFilePath;
    private String vsmFile10Path;
    private String resultsFilePath;

    public BoostNSiftEvaluation(String resultsPath, String answerMatrixPath, int methods, String workingDir, String projectName) {
        this.resultsPath = resultsPath;
        this.answerMatrixPath = answerMatrixPath;
        this.methods = methods;
        this.workingDir = workingDir;
        this.projectName = projectName;
        this.vsmFilePath = Paths.get(workingDir, projectName + ".csv").toString();
        this.vsmFile10Path = Paths.get(workingDir, projectName + "10.csv").toString();
        this.resultsFilePath = Paths.get(workingDir, "results", projectName + "_results.csv").toString();
        System.out.println("Results file path: " + resultsFilePath);
        System.out.println("VSM file path: " + vsmFilePath);
        System.out.println("VSM file 10 path: " + vsmFile10Path);




    }

    public void evaluate() throws IOException {
        List<double[]> results = new ArrayList<>();
        Files.createDirectories(Paths.get(resultsFilePath).getParent());
        try (CSVReader reader1 = new CSVReader(new FileReader(resultsPath));
             CSVReader reader2 = new CSVReader(new FileReader(answerMatrixPath));
             CSVWriter writer = new CSVWriter(new FileWriter(vsmFilePath));
             CSVWriter writer1 = new CSVWriter(new FileWriter(vsmFile10Path));
             CSVWriter resultWriter = new CSVWriter(new FileWriter(resultsFilePath))) {

            String[] list1, list2;
            while ((list1 = reader1.readNext()) != null && (list2 = reader2.readNext()) != null) {
                final String[] finalList1 = list1;  // Make a final copy of list1
                List<Integer> retrieveIds = Arrays.stream(finalList1).filter(x -> !x.isEmpty()).map(Integer::parseInt).collect(Collectors.toList());
                int[] ids = IntStream.rangeClosed(1, methods).toArray();
                List<Integer> retIds = IntStream.range(0, retrieveIds.size()).filter(i -> i % 2 == 0).mapToObj(retrieveIds::get).collect(Collectors.toList());
                List<Double> retScores = IntStream.range(0, retrieveIds.size()).filter(i -> i % 2 != 0).mapToObj(i -> Double.parseDouble(finalList1[i])).collect(Collectors.toList());

                List<Integer> allIds = new ArrayList<>();
                allIds.addAll(retIds);
                Arrays.stream(ids).filter(id -> !retIds.contains(id)).forEach(allIds::add);

                List<Integer> sortedIds = allIds.stream().sorted(Comparator.comparingDouble(i -> -retScores.get(allIds.indexOf(i)))).collect(Collectors.toList());
                List<Double> sortedScores = sortedIds.stream().map(id -> retScores.get(allIds.indexOf(id))).collect(Collectors.toList());

                sortedIds.removeIf(id -> sortedScores.get(sortedIds.indexOf(id)) <= 0.0);

                List<Integer> answerList = Arrays.stream(list2).filter(x -> !x.isEmpty()).map(Integer::parseInt).distinct().collect(Collectors.toList());
                List<Integer> binary = sortedIds.stream().map(id -> answerList.contains(id) ? 1 : 0).collect(Collectors.toList());

                double mrr = InformationRetrievalMetrics.meanReciprocalRank(Collections.singletonList(binary));
                double map = InformationRetrievalMetrics.averagePrecision(binary);

                results.add(new double[]{mrr, map,
                        binary.stream().limit(1).anyMatch(b -> b == 1) ? 1 : 0,
                        binary.stream().limit(5).anyMatch(b -> b == 1) ? 1 : 0,
                        binary.stream().limit(10).anyMatch(b -> b == 1) ? 1 : 0,
                        binary.stream().limit(20).anyMatch(b -> b == 1) ? 1 : 0,
                        binary.stream().limit(100).anyMatch(b -> b == 1) ? 1 : 0});
            }

            for (double[] result : results) {
                resultWriter.writeNext(Arrays.stream(result).mapToObj(String::valueOf).toArray(String[]::new));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        double[] meanResults = results.stream().mapToDouble(r -> Arrays.stream(r).limit(2).average().orElse(0.0)).toArray();
        int[] topResults = results.stream().mapToInt(r -> (int) IntStream.range(2, 7)
                .mapToDouble(i -> r[i])
                .sum()).toArray();

        System.out.println(Arrays.toString(meanResults));
        System.out.println(Arrays.toString(topResults));
    }



    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("Usage: java BoostNSiftEvaluation <resultsPath> <answerMatrixPath> <methods> <workingDir> <projectName>");
            return;
        }
        int methods = Integer.parseInt(args[2]);
        BoostNSiftEvaluation evaluator = new BoostNSiftEvaluation(args[0], args[1], methods, args[3], args[4]);
        try {
            evaluator.evaluate();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
