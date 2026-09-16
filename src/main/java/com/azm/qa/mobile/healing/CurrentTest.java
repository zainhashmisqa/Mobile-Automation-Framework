package com.azm.qa.mobile.healing;

/**
 * Thread-scoped "where am I" context, so a {@link HealEvent} can name the actual test class,
 * method and screen it happened in without threading three extra parameters through every
 * {@code BaseScreen} call. {@code BaseTest} sets the test identity in {@code @BeforeMethod}
 * and clears it in {@code @AfterMethod}; {@code BaseScreen} sets the screen name in its
 * constructor.
 */
public final class CurrentTest {

    private static final ThreadLocal<String> CLASS_NAME = ThreadLocal.withInitial(() -> "unknown");
    private static final ThreadLocal<String> METHOD_NAME = ThreadLocal.withInitial(() -> "unknown");
    private static final ThreadLocal<String> SCREEN_NAME = ThreadLocal.withInitial(() -> "unknown");

    private CurrentTest() {
    }

    public static void set(String className, String methodName) {
        CLASS_NAME.set(className);
        METHOD_NAME.set(methodName);
    }

    public static void setScreen(String screenName) {
        SCREEN_NAME.set(screenName);
    }

    public static void clear() {
        CLASS_NAME.remove();
        METHOD_NAME.remove();
        SCREEN_NAME.remove();
    }

    public static String className() {
        return CLASS_NAME.get();
    }

    public static String methodName() {
        return METHOD_NAME.get();
    }

    public static String screenName() {
        return SCREEN_NAME.get();
    }
}
