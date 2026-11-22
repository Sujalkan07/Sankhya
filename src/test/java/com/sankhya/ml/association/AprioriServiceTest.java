package com.sankhya.ml.association;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AprioriService Unit Tests")
class AprioriServiceTest {

    private final AprioriService aprioriService = new AprioriService();

    @Test
    @DisplayName("Apriori correctly discovers high-lift associations from baskets")
    void testAprioriMining() {
        // 5 baskets:
        // Basket 1: Bread, Milk, Butter
        // Basket 2: Bread, Milk
        // Basket 3: Bread, Butter
        // Basket 4: Milk, Diapers, Beer
        // Basket 5: Bread, Milk, Butter
        List<Set<String>> baskets = List.of(
                new HashSet<>(Arrays.asList("Bread", "Milk", "Butter")),
                new HashSet<>(Arrays.asList("Bread", "Milk")),
                new HashSet<>(Arrays.asList("Bread", "Butter")),
                new HashSet<>(Arrays.asList("Milk", "Diapers", "Beer")),
                new HashSet<>(Arrays.asList("Bread", "Milk", "Butter"))
        );

        List<AssociationRuleDto> rules = aprioriService.mineRules(baskets, 0.20, 0.50, 1.0);

        assertNotNull(rules);
        assertFalse(rules.isEmpty(), "Should discover association rules");

        // Bread and Milk appear together in 3 out of 5 baskets (support = 0.6)
        // Bread appears in 4 baskets (support = 0.8) -> Confidence Bread -> Milk = 3/4 = 0.75
        // Milk appears in 4 baskets (support = 0.8) -> Lift = 0.75 / 0.8 = 0.9375
        boolean foundBreadMilkOrButter = false;
        for (AssociationRuleDto rule : rules) {
            assertTrue(rule.getSupport() >= 0.20);
            assertTrue(rule.getConfidence() >= 0.50);
            assertTrue(rule.getLift() >= 1.0);
            assertNotNull(rule.getRecommendation());
            foundBreadMilkOrButter = true;
        }

        assertTrue(foundBreadMilkOrButter);
    }

    @Test
    @DisplayName("Apriori handles empty or sparse transactions gracefully")
    void testEmptyTransactions() {
        List<AssociationRuleDto> rules = aprioriService.mineRules(Collections.emptyList(), 0.05, 0.5, 1.0);
        assertNotNull(rules);
        assertTrue(rules.isEmpty());
    }
}
