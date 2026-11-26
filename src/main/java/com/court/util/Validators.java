package com.court.util;

import com.court.exception.ValidationException;

import java.util.regex.Pattern;

/**
 * Input checks shared by all services. Each method throws a {@link ValidationException}
 * with a message that can be shown to the user as it is.
 */
public final class Validators {

    private static final Pattern USERNAME = Pattern.compile("[a-z][a-z0-9._]{2,29}");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private static final Pattern PHONE = Pattern.compile("[6-9]\\d{9}");               // Indian mobile number
    private static final Pattern BAR_ID = Pattern.compile("[A-Z]{1,3}/\\d{1,5}/(19|20)\\d{2}"); // e.g. PB/1021/2014
    private static final Pattern ROOM_NO = Pattern.compile("[A-Za-z0-9-]{1,10}");

    private Validators() {
    }

    /** Required text with a maximum length; returns the trimmed value. */
    public static String required(String value, String field, int maxLength) throws ValidationException {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException(field + " is required.");
        }
        if (trimmed.length() > maxLength) {
            throw new ValidationException(field + " can be at most " + maxLength + " characters.");
        }
        return trimmed;
    }

    /** Required text that must be at least {@code minLength} characters (e.g. an order). */
    public static String minLength(String value, String field, int minLength, int maxLength) throws ValidationException {
        String trimmed = required(value, field, maxLength);
        if (trimmed.length() < minLength) {
            throw new ValidationException(field + " must be at least " + minLength + " characters.");
        }
        return trimmed;
    }

    /** Optional text: empty becomes null. */
    public static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static String username(String value) throws ValidationException {
        String v = required(value, "Username", 30).toLowerCase();
        if (!USERNAME.matcher(v).matches()) {
            throw new ValidationException("Username must start with a letter and use only letters, numbers, dots "
                    + "or underscores (3 to 30 characters).");
        }
        return v;
    }

    public static String password(String value) throws ValidationException {
        if (value == null || value.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters.");
        }
        return value;
    }

    public static String email(String value) throws ValidationException {
        String v = optional(value);
        if (v != null && !EMAIL.matcher(v).matches()) {
            throw new ValidationException("Enter a valid email address, like name@example.com.");
        }
        return v;
    }

    public static String phone(String value) throws ValidationException {
        String v = optional(value);
        if (v != null) {
            v = v.replaceAll("[\\s-]", "");
            if (v.startsWith("+91")) v = v.substring(3);
            if (!PHONE.matcher(v).matches()) {
                throw new ValidationException("Enter a 10-digit mobile number starting with 6, 7, 8 or 9.");
            }
        }
        return v;
    }

    public static String barId(String value) throws ValidationException {
        String v = required(value, "Bar Council ID", 30).toUpperCase();
        if (!BAR_ID.matcher(v).matches()) {
            throw new ValidationException("Bar Council ID must look like PB/1021/2014 (state / number / year).");
        }
        return v;
    }

    public static String roomNo(String value) throws ValidationException {
        String v = required(value, "Room number", 10);
        if (!ROOM_NO.matcher(v).matches()) {
            throw new ValidationException("Room number can use letters, numbers and dashes only.");
        }
        return v;
    }
}
