package com.cangxu.wms.algorithm;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AprioriAnalyzerTest {

    @Test
    void minesFrequentPairsAndRules() {
        List<Set<String>> transactions = List.of(
                Set.of("A", "B", "C"),
                Set.of("A", "B"),
                Set.of("A", "C"),
                Set.of("B", "C")
        );

        List<AprioriAnalyzer.Rule> rules = AprioriAnalyzer.minePairs(transactions, 0.5, 0.6);

        assertFalse(rules.isEmpty());
        assertTrue(rules.stream().anyMatch(r -> r.antecedent().equals("A") && r.consequent().equals("B")));
        assertTrue(rules.stream().allMatch(r -> r.confidence() >= 0.6));
        assertTrue(rules.stream().allMatch(r -> r.lift() > 0));
    }
}
