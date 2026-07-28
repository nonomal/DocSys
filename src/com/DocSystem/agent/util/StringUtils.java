package com.DocSystem.agent.util;

/**
 * String utility functions for DocSysAgent.
 * Provides emoji filtering and other string sanitization.
 */
public class StringUtils {

    /**
     * Filter emoji and non-ASCII characters from a string.
     * This prevents database charset issues (utf8mb4 requirement).
     *
     * @param input the input string (may be null)
     * @return string with emoji/non-ASCII characters removed, or null if input was null
     */
    public static String filterEmoji(String input) {
        if (input == null) {
            return null;
        }
        // Remove characters outside ASCII range (0-127) and specific emoji ranges
        // This keeps basic ASCII characters but removes emoji and other unicode symbols
        StringBuilder filtered = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            // Keep ASCII characters (0-127)
            // Remove emoji and other non-ASCII characters
            if (c >= 0 && c <= 127) {
                filtered.append(c);
            }
            // Optional: Keep common Chinese characters (uncomment if needed)
            // else if (c >= 0x4E00 && c <= 0x9FFF) {
            //     filtered.append(c);
            // }
        }
        return filtered.toString();
    }

    /**
     * Truncate a string to maximum length, filtering emoji first.
     *
     * @param input the input string (may be null)
     * @param maxLength maximum length after filtering
     * @return truncated and filtered string, or null if input was null
     */
    public static String truncateAndFilter(String input, int maxLength) {
        if (input == null) {
            return null;
        }
        String filtered = filterEmoji(input);
        if (filtered.length() > maxLength) {
            return filtered.substring(0, maxLength);
        }
        return filtered;
    }

    /**
     * Safe toString that filters emoji.
     *
     * @param obj the object (may be null)
     * @return string representation with emoji filtered, or "null" if input was null
     */
    public static String safeToString(Object obj) {
        if (obj == null) {
            return "null";
        }
        return filterEmoji(obj.toString());
    }
}
