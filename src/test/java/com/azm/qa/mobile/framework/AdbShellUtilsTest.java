package com.azm.qa.mobile.framework;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.azm.qa.mobile.core.util.AdbShellUtils;

/**
 * Pure unit tests for {@link AdbShellUtils#extractComponent} against real captured {@code
 * dumpsys window} lines -- no device, no driver. These are the exact two line shapes found
 * (and confirmed) directly against a real API 34 emulator while fixing the bug where {@code
 * dumpsys window windows} (not {@code dumpsys window}) silently returned nothing useful on
 * this API level -- see AdbShellUtils' own javadoc.
 */
public class AdbShellUtilsTest {

    @Test
    public void extractsComponentFromAMCurrentFocusLine() {
        String line = "  mCurrentFocus=Window{c5542a2 u0 com.google.android.apps.nexuslauncher/"
                + "com.google.android.apps.nexuslauncher.NexusLauncherActivity}";

        String component = AdbShellUtils.extractComponent(line);

        Assert.assertEquals(component,
                "com.google.android.apps.nexuslauncher/com.google.android.apps.nexuslauncher.NexusLauncherActivity");
    }

    @Test
    public void extractsComponentFromAnMFocusedAppLineAndDropsTheTrailingTaskId() {
        // mFocusedApp lines carry a trailing " t77" (task id) after the component, inside the
        // same brace -- confirmed directly this must be trimmed off, not just the closing brace.
        String line = "  mFocusedApp=ActivityRecord{24f285c u0 "
                + "com.google.android.apps.nexuslauncher/.NexusLauncherActivity t77}";

        String component = AdbShellUtils.extractComponent(line);

        Assert.assertEquals(component, "com.google.android.apps.nexuslauncher/.NexusLauncherActivity");
    }

    @Test
    public void extractsARealAptoideComponent() {
        String line = "  mFocusedApp=ActivityRecord{162abb5 u0 cm.aptoide.pt/.view.MainActivity t104}";

        Assert.assertEquals(AdbShellUtils.extractComponent(line), "cm.aptoide.pt/.view.MainActivity");
    }

    @Test
    public void returnsEmptyStringWhenTheLineHasNoComponentSlash() {
        Assert.assertEquals(AdbShellUtils.extractComponent("  mCurrentFocus=null"), "");
    }
}
