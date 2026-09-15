package com.bunbunka.programmernotebook;

import java.util.Locale;

public final class NoteLogic {
    private NoteLogic() {}

    public static boolean shouldPersist(String title, String content) {
        return !safe(title).trim().isEmpty() || !safe(content).trim().isEmpty();
    }

    public static boolean matches(String title, String content, String query) {
        String needle = safe(query).trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) return true;
        return safe(title).toLowerCase(Locale.ROOT).contains(needle)
                || safe(content).toLowerCase(Locale.ROOT).contains(needle);
    }

    public static String displayTitle(String title) {
        String value = safe(title).trim();
        return value.isEmpty() ? "Без названия" : value;
    }

    public static String preview(String content, int maxLength) {
        if (maxLength <= 0) return "";
        String value = safe(content).replaceAll("\\s+", " ").trim();
        if (value.length() <= maxLength) return value;
        if (maxLength == 1) return "…";
        return value.substring(0, maxLength - 1) + "…";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
