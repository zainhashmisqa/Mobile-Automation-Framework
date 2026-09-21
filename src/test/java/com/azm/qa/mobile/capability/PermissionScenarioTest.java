package com.azm.qa.mobile.capability;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.azm.qa.mobile.base.BaseTest;
import com.azm.qa.mobile.base.RetryAnalyzer;
import com.azm.qa.mobile.core.config.ConfigManager;
import com.azm.qa.mobile.core.permissions.PermissionUtils;
import com.azm.qa.mobile.report.AllureMeta;

import io.appium.java_client.android.AndroidDriver;

/**
 * S-30/S-31/S-32/S-33: runtime permission grant/deny, verified at the OS level via
 * {@link PermissionUtils}' dumpsys-backed ground truth. The scenarios as written describe
 * triggering these prompts mid-flow (during a download, on the login screen) -- those trigger
 * points need screen objects this repo doesn't have yet (app detail, login), so these tests
 * exercise the grant/deny mechanism directly rather than through an in-app trigger. See
 * capability/COVERAGE.md.
 */
public class PermissionScenarioTest extends BaseTest {

    private static final String STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
    private static final String CAMERA = "android.permission.CAMERA";
    private static final String ACCOUNTS = "android.permission.GET_ACCOUNTS";

    private PermissionUtils permissions;

    @BeforeMethod(alwaysRun = true)
    public void wireHelpers() {
        permissions = new PermissionUtils((AndroidDriver) driver, ConfigManager.require("app.package"));
    }

    @Test(groups = {"regression", "smoke"}, description = "S-30: storage permission GRANT is reflected at the OS level",
            retryAnalyzer = RetryAnalyzer.class)
    public void storagePermissionGrantIsReflected() {
        AllureMeta.annotate(AllureMeta.Meta.of("S-30", "Runtime Permissions", STORAGE, "HIGH",
                "Grants storage permission and confirms it via dumpsys, independent of Appium's own request."));

        permissions.grant(STORAGE);
        Assert.assertTrue(permissions.isGranted(STORAGE), STORAGE + " should read as granted via dumpsys");
    }

    @Test(groups = {"regression", "smoke"}, description = "S-31: storage permission DENY is reflected at the OS level",
            retryAnalyzer = RetryAnalyzer.class)
    public void storagePermissionDenyIsReflected() {
        AllureMeta.annotate(AllureMeta.Meta.of("S-31", "Runtime Permissions", STORAGE, "HIGH",
                "Revokes storage permission and confirms the denial via dumpsys. In-app reprompt "
                        + "messaging is not exercised -- no download-trigger screen object exists yet."));

        permissions.revoke(STORAGE);
        Assert.assertFalse(permissions.isGranted(STORAGE), STORAGE + " should read as NOT granted via dumpsys");
    }

    @Test(groups = {"regression"}, description = "S-32: camera permission grant/deny is reflected at the OS level",
            retryAnalyzer = RetryAnalyzer.class)
    public void cameraPermissionGrantAndDenyAreReflected() {
        AllureMeta.annotate(AllureMeta.Meta.of("S-32", "Runtime Permissions", CAMERA, "MEDIUM",
                "Grants then revokes camera permission, confirming each state via dumpsys."));

        permissions.grant(CAMERA);
        Assert.assertTrue(permissions.isGranted(CAMERA), CAMERA + " should read as granted via dumpsys");

        permissions.revoke(CAMERA);
        Assert.assertFalse(permissions.isGranted(CAMERA), CAMERA + " should read as NOT granted via dumpsys");
    }

    @Test(groups = {"regression"}, description = "S-33: accounts permission grant/deny is reflected at the OS level",
            retryAnalyzer = RetryAnalyzer.class)
    public void accountsPermissionGrantAndDenyAreReflected() {
        AllureMeta.annotate(AllureMeta.Meta.of("S-33", "Runtime Permissions", ACCOUNTS, "MEDIUM",
                "Grants then revokes the accounts permission used by the login flow, confirming each "
                        + "state via dumpsys. The login screen itself is not driven here -- no screen "
                        + "object exists for it yet."));

        permissions.grant(ACCOUNTS);
        Assert.assertTrue(permissions.isGranted(ACCOUNTS), ACCOUNTS + " should read as granted via dumpsys");

        permissions.revoke(ACCOUNTS);
        Assert.assertFalse(permissions.isGranted(ACCOUNTS), ACCOUNTS + " should read as NOT granted via dumpsys");
    }
}
