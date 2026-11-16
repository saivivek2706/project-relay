package com.azentio.projectrelay.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Masking {
    private static final Pattern ACCOUNT_NUMBER = Pattern.compile("(?<!\\d)\\d{10,16}(?!\\d)");

    private Masking() { }

    public static String mask(String account) {
        if (account == null || account.length() < 4) return "XXXXXX****";
        return "XXXXXX" + account.substring(account.length() - 4);
    }

    public static String maskPath(List<String> accounts) {
        return accounts.stream().map(Masking::mask).collect(Collectors.joining(" -> "));
    }

    /** Masks any standalone 10-16 digit account number embedded in investigator-supplied text. */
    public static String maskSensitiveText(String value) {
        if (value == null || value.isEmpty()) return value;
        Matcher matcher = ACCOUNT_NUMBER.matcher(value);
        return matcher.replaceAll(match -> "XXXXXX" + match.group().substring(match.group().length() - 4));
    }
}
