package com.azm.qa.mobile.selfscore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Shared, read-only file-system scanning for every {@link Criterion}. Deliberately generic —
 * no criterion hardcodes this project's own package name, because the whole point of
 * {@code selfscore} is to score a repo it did not build (a teammate's framework), not just
 * itself.
 *
 * <p>{@link #stripComments} exists because a naive text search for something like
 * {@code Thread.sleep(} finds it inside javadoc explaining a class has none — confirmed
 * directly against this very repo, which returns 3 raw hits and 0 real ones. Every criterion
 * that greps for a code pattern should scan stripped source, not raw source.
 */
public final class RepoScan {

    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    private RepoScan() {
    }

    public static List<Path> javaFiles(Path repoRoot, String... subdirs) {
        List<Path> all = new ArrayList<>();
        for (String subdir : subdirs) {
            Path dir = repoRoot.resolve(subdir);
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (var stream = Files.walk(dir)) {
                all.addAll(stream.filter(p -> p.toString().endsWith(".java")).toList());
            } catch (IOException ignored) {
                // A directory that vanishes mid-walk just contributes nothing -- not fatal.
            }
        }
        return all;
    }

    public static Optional<String> readQuietly(Path file) {
        try {
            return Optional.of(Files.readString(file));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Removes block comments (including javadoc) entirely, then drops any remaining line whose
     * trimmed content starts with {@code //}. This is what makes the flakiness check honest:
     * {@code {@code Thread.sleep}} inside a javadoc block never survives to the search.
     */
    public static String stripComments(String source) {
        String noBlocks = BLOCK_COMMENT.matcher(source).replaceAll("");
        return noBlocks.lines()
                .filter(line -> !line.strip().startsWith("//"))
                .collect(Collectors.joining("\n"));
    }

    public static int countMatches(String text, Pattern pattern) {
        Matcher m = pattern.matcher(text);
        int count = 0;
        while (m.find()) {
            count++;
        }
        return count;
    }

    public static List<Path> findByNameContains(Path repoRoot, String needle) {
        if (!Files.isDirectory(repoRoot)) {
            return List.of();
        }
        try (var stream = Files.walk(repoRoot)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(java.util.Locale.ROOT)
                            .contains(needle.toLowerCase(java.util.Locale.ROOT)))
                    .filter(p -> !p.toString().contains(java.io.File.separator + "target" + java.io.File.separator))
                    .filter(p -> !p.toString().contains(java.io.File.separator + ".git" + java.io.File.separator))
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    public static boolean anyFileNameContains(Path repoRoot, String needle) {
        return !findByNameContains(repoRoot, needle).isEmpty();
    }
}
