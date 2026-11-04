package project2;

/**
 * Minimum password lengths per role. Single source for registration, user
 * creation, and password changes.
 */
public final class PasswordPolicy {

    public static final int USER_MIN_LENGTH = 6;
    public static final int ADMIN_MIN_LENGTH = 12;

    private PasswordPolicy() {
    }

    public static int minimumLengthForRole(String role) {
        return "ADMIN".equals(role) ? ADMIN_MIN_LENGTH : USER_MIN_LENGTH;
    }

    public static void checkLength(String role, String password) {
        int minimum = minimumLengthForRole(role);
        if (password.length() < minimum) {
            throw new IllegalArgumentException("Password must be at least " + minimum + " characters.");
        }
    }
}
