package com.azm.qa.mobile.report;

import io.qameta.allure.Allure;

/**
 * The Java twin of the web framework's {@code annotate()} helper — every test opens with a
 * call to {@link #annotate} so the Allure report can be filtered by feature/story/severity and
 * linked back to the originating spec, the same way the web report links back to Jira.
 */
public final class AllureMeta {

    private AllureMeta() {
    }

    public static void annotate(Meta meta) {
        Allure.epic("AZM Mobile Automation");
        if (meta.feature() != null) {
            Allure.feature(meta.feature());
        }
        if (meta.story() != null) {
            Allure.story(meta.story());
        }
        if (meta.testCaseId() != null) {
            Allure.tms(meta.testCaseId(), meta.testCaseId());
        }
        if (meta.severity() != null) {
            Allure.label("severity", meta.severity());
        }
        if (meta.description() != null) {
            Allure.description(meta.description());
        }
    }

    public record Meta(String testCaseId, String feature, String story, String severity, String description) {
        public static Meta of(String testCaseId, String feature, String story, String severity, String description) {
            return new Meta(testCaseId, feature, story, severity, description);
        }
    }
}
