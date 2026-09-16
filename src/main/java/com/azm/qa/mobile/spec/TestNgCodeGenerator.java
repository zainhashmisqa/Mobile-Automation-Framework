package com.azm.qa.mobile.spec;

import java.util.Locale;

/**
 * Turns a parsed {@link SpecModel} into a compilable TestNG class, purely by string templating
 * — no AI, no network call, same input always yields the same output byte-for-byte. That
 * determinism is what {@code codegen-drift-check} in CI depends on: regenerating from an
 * unchanged spec must never produce a diff.
 *
 * <p>Generated files live under {@code src/test/java/.../generated/} and are overwritten on
 * every run — see the class-level warning comment this generator writes into every file it
 * produces. Anything hand-written belongs in the source spec (as a step this vocabulary
 * understands) or in a separate, non-generated class.
 */
public final class TestNgCodeGenerator {

    private static final String PACKAGE = "com.azm.qa.mobile.generated";
    private final StepVocabulary vocabulary = new StepVocabulary();

    public GeneratedFile generate(SpecModel spec) {
        String className = toClassName(spec.featureName) + "Test";
        StringBuilder src = new StringBuilder();
        int todoCount = 0;

        src.append("package ").append(PACKAGE).append(";\n\n");
        src.append("// GENERATED FILE -- do not edit by hand.\n");
        src.append("// Source: specs/").append(toFileStem(spec.featureName)).append(".md\n");
        src.append("// Regenerate with: mvn exec:java -Dexec.args=\"md --file specs/")
                .append(toFileStem(spec.featureName)).append(".md\"\n\n");
        src.append("import com.azm.qa.mobile.base.BaseTest;\n");
        src.append("import com.azm.qa.mobile.core.deeplink.DeepLinkUtils;\n");
        src.append("import com.azm.qa.mobile.core.gestures.GestureUtils;\n");
        src.append("import com.azm.qa.mobile.core.lifecycle.AppLifecycleUtils;\n");
        src.append("import com.azm.qa.mobile.core.permissions.PermissionUtils;\n");
        src.append("import com.azm.qa.mobile.core.context.WebViewContextUtils;\n");
        src.append("import com.azm.qa.mobile.report.AllureMeta;\n");
        src.append("import org.testng.annotations.BeforeMethod;\n");
        src.append("import org.testng.annotations.Test;\n\n");

        src.append("public class ").append(className).append(" extends BaseTest {\n\n");
        src.append("    private ")
                .append(spec.screen != null
                        ? "com.azm.qa.mobile.screens." + spec.screen
                        : "com.azm.qa.mobile.screens.GenericScreen")
                .append(" screen;\n");
        src.append("    private GestureUtils gestures;\n");
        src.append("    private AppLifecycleUtils appLifecycle;\n");
        src.append("    private PermissionUtils permissions;\n");
        src.append("    private DeepLinkUtils deepLink;\n");
        src.append("    private WebViewContextUtils webViewContext;\n\n");

        // TestNG runs a superclass's @BeforeMethod before a subclass's own, so `driver` from
        // BaseTest.setUp() is always ready by the time this runs -- no explicit ordering needed.
        src.append("    @BeforeMethod(alwaysRun = true)\n");
        src.append("    public void wireHelpers() {\n");
        src.append("        screen = new ")
                .append(spec.screen != null
                        ? "com.azm.qa.mobile.screens." + spec.screen
                        : "com.azm.qa.mobile.screens.GenericScreen")
                .append("(driver);\n");
        src.append("        gestures = new GestureUtils(driver);\n");
        src.append("        appLifecycle = new AppLifecycleUtils(driver, com.azm.qa.mobile.core.config.ConfigManager.require(\"app.package\"));\n");
        src.append("        permissions = new PermissionUtils((io.appium.java_client.android.AndroidDriver) driver, com.azm.qa.mobile.core.config.ConfigManager.require(\"app.package\"));\n");
        src.append("        deepLink = new DeepLinkUtils((io.appium.java_client.android.AndroidDriver) driver, com.azm.qa.mobile.core.config.ConfigManager.require(\"app.package\"));\n");
        src.append("        webViewContext = new WebViewContextUtils(driver);\n");
        src.append("    }\n\n");

        for (SpecModel.Scenario scenario : spec.scenarios) {
            GeneratedMethod method = generateMethod(spec, scenario);
            todoCount += method.todoCount;
            src.append(method.source);
            src.append('\n');
        }

        src.append("}\n");
        return new GeneratedFile(className + ".java", src.toString(), todoCount);
    }

    private GeneratedMethod generateMethod(SpecModel spec, SpecModel.Scenario scenario) {
        String methodName = toMethodName(scenario.title);
        // A set, not a list: a CRITICAL-priority scenario that also carries an explicit
        // "@smoke" tag would otherwise add "smoke" twice -- once from the priority rule, once
        // from the tag itself -- and duplicate groups in generated source is exactly the kind
        // of thing codegen-drift-check should never wave through.
        java.util.LinkedHashSet<String> groups = new java.util.LinkedHashSet<>();
        groups.add("regression");
        boolean isNegativeOrEdge = "NEGATIVE".equals(scenario.type) || "EDGE".equals(scenario.type);
        boolean isCriticalOrHigh = "CRITICAL".equalsIgnoreCase(scenario.priority) || "HIGH".equalsIgnoreCase(scenario.priority);
        if (isNegativeOrEdge) {
            groups.add("negative");
        } else if (isCriticalOrHigh) {
            groups.add("smoke");
        }
        scenario.tags.forEach(t -> groups.add(t.replace("@", "")));

        StringBuilder m = new StringBuilder();
        m.append("    @Test(groups = {")
                .append(groups.stream().map(g -> "\"" + g + "\"").reduce((a, b) -> a + ", " + b).orElse(""))
                .append("}, description = ")
                .append(quote(scenario.title))
                .append(")\n");
        m.append("    public void ").append(methodName).append("() {\n");
        m.append("        AllureMeta.annotate(AllureMeta.Meta.of(")
                .append(quote(spec.key != null ? spec.key : "UNSPECIFIED")).append(", ")
                .append(quote(spec.featureName)).append(", ")
                .append(quote(scenario.title)).append(", ")
                .append(quote(scenario.priority)).append(", ")
                .append(quote(spec.description)).append("));\n");

        int todoCount = 0;
        for (SpecModel.Step step : scenario.steps) {
            StepVocabulary.CompiledStep compiled = vocabulary.compile(step);
            if (compiled.isTodo()) {
                todoCount++;
            }
            for (String line : compiled.javaLines()) {
                m.append("        ").append(line).append("\n");
            }
        }
        m.append("    }\n");

        return new GeneratedMethod(m.toString(), todoCount);
    }

    private static String toClassName(String featureName) {
        if (featureName == null || featureName.isBlank()) {
            return "Unnamed";
        }
        StringBuilder out = new StringBuilder();
        for (String word : featureName.split("[^A-Za-z0-9]+")) {
            if (!word.isEmpty()) {
                out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return out.isEmpty() ? "Unnamed" : out.toString();
    }

    private static String toMethodName(String title) {
        String className = toClassName(title);
        return className.isEmpty() ? "scenario" : Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    private static String toFileStem(String featureName) {
        if (featureName == null || featureName.isBlank()) {
            return "unnamed";
        }
        return featureName.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }

    private static String quote(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    public record GeneratedFile(String fileName, String source, int todoCount) {
    }

    private record GeneratedMethod(String source, int todoCount) {
    }
}
