package analysis;

import java.util.*;
import java.util.stream.*;

public class InformationRetrievalMetrics {

    public static double meanReciprocalRank(List<List<Integer>> rs) {
        return rs.stream()
                .mapToDouble(r -> {
                    OptionalInt indexOpt = IntStream.range(0, r.size())
                            .filter(i -> r.get(i) != 0)
                            .findFirst();
                    return indexOpt.isPresent() ? 1.0 / (indexOpt.getAsInt() + 1) : 0.0;
                }).average().orElse(0.0);
    }

    public static double rPrecision(List<Integer> r) {
        int lastRelevantIndex = IntStream.range(0, r.size())
                .filter(i -> r.get(i) != 0)
                .reduce((first, second) -> second)
                .orElse(-1);
        if (lastRelevantIndex == -1) return 0.0;
        return IntStream.rangeClosed(0, lastRelevantIndex)
                .filter(i -> r.get(i) != 0)
                .count() / (double) (lastRelevantIndex + 1);
    }

    public static double precisionAtK(List<Integer> r, int k) {
        if (k > r.size()) {
            throw new IllegalArgumentException("Relevance score length < k");
        }
        long relevantCount = IntStream.range(0, k)
                .filter(i -> r.get(i) != 0)
                .count();
        return relevantCount / (double) k;
    }

    public static double recallAtK(List<Integer> r, int k, int correctElements) {
        if (k > r.size()) k = r.size();
        long relevantCount = IntStream.range(0, k)
                .filter(i -> r.get(i) != 0)
                .count();
        return relevantCount / (double) correctElements;
    }

    public static double averagePrecision(List<Integer> r) {
        double sum = 0.0;
        int relevantCount = 0;
        for (int i = 0; i < r.size(); i++) {
            if (r.get(i) != 0) {
                relevantCount++;
                sum += precisionAtK(r, i + 1) * 1.0;
            }
        }
        return relevantCount == 0 ? 0.0 : sum / relevantCount;
    }

    public static double meanAveragePrecision(List<List<Integer>> rs) {
        return rs.stream()
                .mapToDouble(InformationRetrievalMetrics::averagePrecision)
                .average().orElse(0.0);
    }

    public static double dcgAtK(List<Integer> r, int k, int method) {
        if (k > r.size()) k = r.size();
        double dcg = 0.0;
        for (int i = 0; i < k; i++) {
            double score = r.get(i);
            double logPosition = method == 0 ? Math.log(i + 2) / Math.log(2) : Math.log(i + 1) / Math.log(2);
            if (i == 0 && method == 0) logPosition = 1.0;
            dcg += score / logPosition;
        }
        return dcg;
    }

    public static double ndcgAtK(List<Integer> r, int k, int method) {
        List<Integer> sortedR = new ArrayList<>(r);
        Collections.sort(sortedR, Collections.reverseOrder());
        double maxDcg = dcgAtK(sortedR, k, method);
        if (maxDcg == 0) return 0.0;
        return dcgAtK(r, k, method) / maxDcg;
    }

    public static void main(String[] args) {
        // Example test cases can be added here to verify the methods.
    }
}
