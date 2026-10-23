package com.cangxu.wms.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Frequent 2-itemset association mining for order correlation.
 * This is a focused Apriori step: frequent 1-itemsets -> frequent pairs -> rules.
 */
public final class AprioriAnalyzer {

    private AprioriAnalyzer() {
    }

    public record Rule(String antecedent, String consequent, double support, double confidence, double lift) {
    }

    public static List<Rule> minePairs(List<Set<String>> transactions, double minSupport, double minConfidence) {
        if (transactions == null || transactions.isEmpty()) {
            return List.of();
        }
        int total = transactions.size();
        Map<String, Integer> itemCounts = new HashMap<>();
        for (Set<String> transaction : transactions) {
            for (String item : transaction) {
                itemCounts.merge(item, 1, Integer::sum);
            }
        }

        Set<String> frequentItems = new HashSet<>();
        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            if ((double) entry.getValue() / total >= minSupport) {
                frequentItems.add(entry.getKey());
            }
        }

        Map<String, Integer> pairCounts = new HashMap<>();
        for (Set<String> transaction : transactions) {
            List<String> items = transaction.stream().filter(frequentItems::contains).sorted().toList();
            for (int i = 0; i < items.size(); i++) {
                for (int j = i + 1; j < items.size(); j++) {
                    String key = items.get(i) + "\u0001" + items.get(j);
                    pairCounts.merge(key, 1, Integer::sum);
                }
            }
        }

        List<Rule> rules = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : pairCounts.entrySet()) {
            String[] pair = entry.getKey().split("\u0001");
            double pairSupport = (double) entry.getValue() / total;
            if (pairSupport < minSupport) {
                continue;
            }
            String a = pair[0];
            String b = pair[1];
            double supportA = (double) itemCounts.get(a) / total;
            double supportB = (double) itemCounts.get(b) / total;
            addRule(rules, a, b, pairSupport, supportA, supportB, minConfidence);
            addRule(rules, b, a, pairSupport, supportB, supportA, minConfidence);
        }
        return rules;
    }

    private static void addRule(List<Rule> rules, String a, String b, double pairSupport,
                                double supportA, double supportB, double minConfidence) {
        double confidence = supportA == 0 ? 0 : pairSupport / supportA;
        if (confidence >= minConfidence) {
            double lift = supportB == 0 ? 0 : confidence / supportB;
            rules.add(new Rule(a, b, pairSupport, confidence, lift));
        }
    }
}
