package com.azm.qa.mobile.framework;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.azm.qa.mobile.capability.appmgmt.AppInstallUtils;

/**
 * Pure unit tests for {@link AppInstallUtils#parseVersionName} against canned {@code dumpsys
 * package} fixtures -- no device, no driver.
 */
public class AppInstallUtilsTest {

    @Test
    public void parsesVersionNameFromARealisticDumpsysBlock() {
        String dump = "  Package [cm.aptoide.pt] (5c84e31):\n"
                + "    userId=10142\n"
                + "    pkg=Package{4fc6c84 cm.aptoide.pt}\n"
                + "    versionCode=92205300 minSdk=16 targetSdk=32\n"
                + "    versionName=9.22.5.3\n"
                + "    splits=[base]\n";

        Assert.assertEquals(AppInstallUtils.parseVersionName(dump), "9.22.5.3");
    }

    @Test
    public void returnsEmptyStringWhenNoVersionNameLineExists() {
        String dump = "  Package [cm.aptoide.pt] (5c84e31):\n    userId=10142\n";

        Assert.assertEquals(AppInstallUtils.parseVersionName(dump), "");
    }

    @Test
    public void tolerantOfLeadingWhitespaceOnTheVersionNameLine() {
        String dump = "\t\t  versionName=1.0.0-beta\n";

        Assert.assertEquals(AppInstallUtils.parseVersionName(dump), "1.0.0-beta");
    }
}
