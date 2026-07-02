package imss.gob.mx.cohorte.utils;

import java.security.SecureRandom;

public final class CredentialGenerator {

    private CredentialGenerator() {}

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%&*";
    private static final String ALL = UPPER + LOWER + DIGITS + SPECIAL;

    public static String generarPasswordSeguro() {
        SecureRandom rng = new SecureRandom();
        char[] pw = new char[16];

        pw[0] = UPPER.charAt(rng.nextInt(UPPER.length()));
        pw[1] = LOWER.charAt(rng.nextInt(LOWER.length()));
        pw[2] = DIGITS.charAt(rng.nextInt(DIGITS.length()));
        pw[3] = SPECIAL.charAt(rng.nextInt(SPECIAL.length()));

        for (int i = 4; i < pw.length; i++) {
            pw[i] = ALL.charAt(rng.nextInt(ALL.length()));
        }

        for (int i = pw.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            char tmp = pw[i];
            pw[i] = pw[j];
            pw[j] = tmp;
        }

        return new String(pw);
    }
}
