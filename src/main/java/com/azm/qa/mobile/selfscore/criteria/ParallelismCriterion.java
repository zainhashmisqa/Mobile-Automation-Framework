package com.azm.qa.mobile.selfscore.criteria;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.azm.qa.mobile.selfscore.Criterion;
import com.azm.qa.mobile.selfscore.CriterionScore;
import com.azm.qa.mobile.selfscore.RepoScan;

/**
 * Parallelism plus the mobile-specific concern of device fragmentation. Specifically checks for
 * the exact collision risk this project found and fixed in itself: multiple device profiles
 * that each hardcode a systemPort, rather than deriving ports so a new profile can't collide.
 */
public final class ParallelismCriterion implements Criterion {

    private static final Pattern PARALLEL_ATTR = Pattern.compile("parallel\\s*=\\s*\"(methods|tests|classes)\"");
    private static final Pattern THREAD_COUNT = Pattern.compile("thread-count\\s*=\\s*\"(\\d+)\"");
    private static final Pattern DEVICE_ENTRY = Pattern.compile("\"(deviceName|udid)\"\\s*:");
    private static final Pattern HARDCODED_PORT = Pattern.compile("\"systemPort\"\\s*:");

    @Override
    public String name() {
        return "Parallelism & device fragmentation";
    }

    @Override
    public String mappedFrom() {
        return "Parallelism";
    }

    @Override
    public CriterionScore evaluate(Path repoRoot) {
        List<String> evidence = new ArrayList<>();
        double score = 0;

        List<Path> suiteFiles = RepoScan.findByNameContains(repoRoot, "testng").stream()
                .filter(p -> p.toString().endsWith(".xml"))
                .toList();
        boolean anyParallel = false;
        int maxThreadCount = 0;
        for (Path f : suiteFiles) {
            String content = RepoScan.readQuietly(f).orElse("");
            if (PARALLEL_ATTR.matcher(content).find()) {
                anyParallel = true;
            }
            var m = THREAD_COUNT.matcher(content);
            while (m.find()) {
                maxThreadCount = Math.max(maxThreadCount, Integer.parseInt(m.group(1)));
            }
        }
        if (anyParallel && maxThreadCount > 1) {
            score += 4;
            evidence.add("A testng.xml declares parallel execution with thread-count=" + maxThreadCount + ".");
        } else if (anyParallel) {
            score += 1;
            evidence.add("parallel=\"...\" is declared but thread-count is 1 -- parallelism is configured but not actually enabled.");
        } else {
            evidence.add("No testng*.xml file declares parallel execution.");
        }

        List<Path> deviceConfigs = RepoScan.findByNameContains(repoRoot, "device-profiles");
        int profileCount = 0;
        boolean hardcodedPorts = false;
        for (Path f : deviceConfigs) {
            String content = RepoScan.readQuietly(f).orElse("");
            profileCount += RepoScan.countMatches(content, DEVICE_ENTRY);
            if (HARDCODED_PORT.matcher(content).find()) {
                hardcodedPorts = true;
            }
        }
        if (profileCount >= 2) {
            score += 3;
            evidence.add(deviceConfigs.size() + " device-profile file(s) with " + profileCount + " device entrie(s) -- more than one real device profile is defined.");
        } else if (profileCount == 1) {
            score += 1;
            evidence.add("Only one device profile defined -- no fragmentation evidence.");
        } else {
            evidence.add("No device-profiles-style config found.");
        }

        boolean hasPortPool = RepoScan.anyFileNameContains(repoRoot.resolve("src/main"), "devicepool")
                || RepoScan.anyFileNameContains(repoRoot.resolve("src/main"), "portpool")
                || RepoScan.anyFileNameContains(repoRoot.resolve("src/main"), "portallocator");
        if (hasPortPool) {
            score += 3;
            evidence.add("A per-thread port-derivation class (DevicePool/PortPool) exists -- a new device profile can't collide with an existing one's port.");
        } else if (hardcodedPorts) {
            evidence.add("Device profiles hardcode systemPort values with no derivation mechanism found -- adding a profile risks a silent port collision (\"instrumentation process cannot be initialized\").");
        } else {
            evidence.add("No port-derivation mechanism found (and no hardcoded ports either -- ports may not be managed at all).");
        }

        String verdict = score >= 8 ? "Parallel execution is real, and port collisions are structurally impossible."
                : score >= 4 ? "Parallelism is partially set up but has an unaddressed collision risk or is unproven."
                : "Little real parallelism or device-fragmentation coverage.";
        return new CriterionScore(Math.min(10, score), evidence, verdict);
    }
}
