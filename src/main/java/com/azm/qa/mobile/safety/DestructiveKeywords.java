package com.azm.qa.mobile.safety;

import java.util.List;
import java.util.Locale;

/**
 * Words that flag a step as destructive — logout, delete, uninstall, factory-reset, cancel a
 * subscription. Nothing in this framework blocks such a step outright (a real test sometimes
 * needs one), but {@code /gen-tests} is instructed never to *add* one unless the user
 * explicitly asked, and {@code TestNgCodeGenerator} could use this list to flag a scenario for
 * extra review. Mirrors the web framework's {@code destructive-keywords.ts} list, narrowed to
 * what's meaningful on mobile (app uninstall/reset, OS-level account removal) in addition to
 * the app-level actions both platforms share.
 */
public final class DestructiveKeywords {

    public static final List<String> KEYWORDS = List.of(
            "logout", "log out", "sign out", "signout",
            "delete", "remove", "cancel subscription", "deactivate", "close account",
            "uninstall", "factory reset", "clear app data", "wipe device",
            "revoke all permissions", "disable account"
    );

    private DestructiveKeywords() {
    }

    public static boolean matches(String stepText) {
        if (stepText == null) {
            return false;
        }
        String lower = stepText.toLowerCase(Locale.ROOT);
        return KEYWORDS.stream().anyMatch(lower::contains);
    }
}
