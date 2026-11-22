package com.sankhya.ml.association;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AprioriService {

    public List<AssociationRuleDto> mineRules(List<Set<String>> transactions,
                                              double minSupport,
                                              double minConfidence,
                                              double minLift) {
        if (transactions == null || transactions.isEmpty()) {
            return Collections.emptyList();
        }

        int totalTransactions = transactions.size();

        // 1. Find frequent 1-itemsets
        Map<String, Integer> itemCounts = new HashMap<>();
        for (Set<String> tx : transactions) {
            for (String item : tx) {
                itemCounts.merge(item, 1, Integer::sum);
            }
        }

        int minSupportCount = Math.max(1, (int) Math.ceil(minSupport * totalTransactions));
        Map<String, Double> frequent1Itemsets = new HashMap<>();
        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            if (entry.getValue() >= minSupportCount) {
                frequent1Itemsets.put(entry.getKey(), (double) entry.getValue() / totalTransactions);
            }
        }

        List<String> items = new ArrayList<>(frequent1Itemsets.keySet());
        Collections.sort(items);

        // 2. Find frequent 2-itemsets
        Map<Set<String>, Integer> pairCounts = new HashMap<>();
        for (Set<String> tx : transactions) {
            List<String> presentItems = new ArrayList<>();
            for (String item : items) {
                if (tx.contains(item)) {
                    presentItems.add(item);
                }
            }

            int pSize = presentItems.size();
            for (int i = 0; i < pSize; i++) {
                for (int j = i + 1; j < pSize; j++) {
                    Set<String> pair = new TreeSet<>(Arrays.asList(presentItems.get(i), presentItems.get(j)));
                    pairCounts.merge(pair, 1, Integer::sum);
                }
            }
        }

        // 3. Generate Association Rules from frequent 2-itemsets
        List<AssociationRuleDto> rules = new ArrayList<>();

        for (Map.Entry<Set<String>, Integer> entry : pairCounts.entrySet()) {
            int countAB = entry.getValue();
            if (countAB < minSupportCount) continue;

            double supportAB = (double) countAB / totalTransactions;
            List<String> pairList = new ArrayList<>(entry.getKey());
            String itemA = pairList.get(0);
            String itemB = pairList.get(1);

            double supportA = frequent1Itemsets.getOrDefault(itemA, 0.0);
            double supportB = frequent1Itemsets.getOrDefault(itemB, 0.0);

            // Direction 1: A -> B
            evaluateAndAddRule(rules, Collections.singleton(itemA), Collections.singleton(itemB),
                    supportAB, supportA, supportB, minConfidence, minLift);

            // Direction 2: B -> A
            evaluateAndAddRule(rules, Collections.singleton(itemB), Collections.singleton(itemA),
                    supportAB, supportB, supportA, minConfidence, minLift);
        }

        // Sort rules by Lift descending
        rules.sort(Comparator.comparingDouble(AssociationRuleDto::getLift).reversed());
        return rules;
    }

    private void evaluateAndAddRule(List<AssociationRuleDto> rules,
                                    Set<String> antecedent,
                                    Set<String> consequent,
                                    double supportAB,
                                    double supportA,
                                    double supportB,
                                    double minConfidence,
                                    double minLift) {
        if (supportA <= 0 || supportB <= 0) return;

        double confidence = supportAB / supportA;
        if (confidence < minConfidence) return;

        double lift = confidence / supportB;
        if (lift < minLift) return;

        double conviction;
        if (Math.abs(1.0 - confidence) < 1e-6) {
            conviction = 100.0; // effectively infinite
        } else {
            conviction = (1.0 - supportB) / (1.0 - confidence);
        }

        String antStr = String.join(", ", antecedent);
        String conStr = String.join(", ", consequent);
        String recommendation = String.format(
                "Cross-merchandising: Customers purchasing [%s] are %.1fx more likely to purchase [%s]. Co-locate in aisle end-caps or bundle with a 10%% incentive.",
                antStr, lift, conStr
        );

        rules.add(new AssociationRuleDto(antecedent, consequent, supportAB, confidence, lift, conviction, recommendation));
    }
}
