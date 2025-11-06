package com.eb.util;

import java.util.*;
import java.util.stream.IntStream;

public class EmbeddingSearch {

    public static double cosine(double[] a, double[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static void main(String[] args) throws Exception {
        // Example embeddings (normally from AiFunctions.embed)
        double[] query = {0.1, 0.2, 0.3};
        Map<String, double[]> docs = Map.of(
            "Doc A", new double[]{0.1, 0.2, 0.29},
            "Doc B", new double[]{0.5, 0.1, 0.0},
            "Doc C", new double[]{0.09, 0.21, 0.31}
        );

        docs.entrySet().stream()
            .map(e -> Map.entry(e.getKey(), cosine(query, e.getValue())))
            .sorted((a,b) -> Double.compare(b.getValue(), a.getValue()))
            .forEach(e -> System.out.printf("%s -> %.4f%n", e.getKey(), e.getValue()));
    }
}
