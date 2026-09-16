package com.azm.qa.mobile.selfscore.criteria;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.azm.qa.mobile.selfscore.Criterion;
import com.azm.qa.mobile.selfscore.CriterionScore;
import com.azm.qa.mobile.selfscore.RepoScan;

/**
 * The mobile mapping of the deck's "AI/Healing" axis. The deck itself warns against fake
 * healing (a repo that claims AI self-healing but has no real recovery mechanism, or claims an
 * LLM call that doesn't exist) -- so this rewards a real, evidenced recovery mechanism and does
 * NOT reward a bare claim of "AI-powered" with nothing behind it. A deterministic,
 * fingerprint-based cascade with a real break-and-recover proof scores as well as an
 * LLM-backed one; an unsubstantiated claim scores worse than an honest "not implemented".
 */
public final class SelfHealingAuthenticityCriterion implements Criterion {

    @Override
    public String name() {
        return "Self-healing authenticity";
    }

    @Override
    public String mappedFrom() {
        return "AI/Healing";
    }

    @Override
    public CriterionScore evaluate(Path repoRoot) {
        List<String> evidence = new ArrayList<>();
        double score = 0;

        List<Path> ledgers = RepoScan.findByNameContains(repoRoot, "heal");
        List<Path> jsonlLedgers = ledgers.stream()
                .filter(p -> p.toString().endsWith(".jsonl") || p.toString().endsWith(".json"))
                .filter(p -> !p.toString().endsWith(".java"))
                .toList();

        int realEventCount = 0;
        for (Path p : jsonlLedgers) {
            String content = RepoScan.readQuietly(p).orElse("");
            realEventCount += (int) content.lines().filter(l -> !l.isBlank()).count();
        }
        if (realEventCount > 0) {
            score += 4;
            evidence.add(realEventCount + " real event(s) found across " + jsonlLedgers.size()
                    + " heal-ledger-like file(s) -- healing has actually fired, not just been coded.");
        } else if (!jsonlLedgers.isEmpty()) {
            score += 1;
            evidence.add(jsonlLedgers.size() + " heal-ledger-like file(s) found but empty -- the mechanism exists but has never recorded a real heal.");
        } else {
            evidence.add("No heal ledger file found (nothing matching *heal*.json/.jsonl).");
        }

        boolean hasCascadeOrEngine = RepoScan.anyFileNameContains(repoRoot.resolve("src/main"), "selfheal")
                || RepoScan.anyFileNameContains(repoRoot.resolve("src/main"), "cascade");
        if (hasCascadeOrEngine) {
            score += 3;
            evidence.add("A self-healing/cascade engine class exists under src/main.");
        } else {
            evidence.add("No self-healing/cascade engine class found under src/main.");
        }

        List<Path> proofTests = RepoScan.findByNameContains(repoRoot, "selfheal").stream()
                .filter(p -> p.toString().endsWith(".java") && p.toString().contains("test"))
                .toList();
        boolean hasBreakAndRecoverProof = proofTests.stream().anyMatch(p ->
                RepoScan.readQuietly(p).map(RepoScan::stripComments)
                        .map(s -> s.toLowerCase(java.util.Locale.ROOT))
                        .map(s -> (s.contains("break") || s.contains("uuid") || s.contains("deliberat"))
                                && (s.contains("recover") || s.contains("heal")))
                        .orElse(false));
        if (hasBreakAndRecoverProof) {
            score += 3;
            evidence.add("Found a test that deliberately breaks a locator and asserts recovery -- a real proof, not just a happy-path pass.");
        } else if (!proofTests.isEmpty()) {
            score += 1;
            evidence.add(proofTests.size() + " self-healing test file(s) exist but don't clearly assert a deliberate break-and-recover.");
        } else {
            evidence.add("No dedicated self-healing proof test found.");
        }

        String verdict = score >= 8 ? "Self-healing is real, evidenced, and proven to have fired."
                : score >= 4 ? "Self-healing exists but the evidence trail is thin."
                : "No credible self-healing mechanism found.";
        return new CriterionScore(Math.min(10, score), evidence, verdict);
    }
}
