package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * 生成随机盐值
     */
    public static String generateSalt() {
        StringBuilder salt = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            salt.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return salt.toString();
    }

    /**
     * 使用SHA-256算法加密密码
     */
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }

    /**
     * 验证密码
     */
    public static boolean verifyPassword(String password, String salt, String hashedPassword) {
        String newHashedPassword = hashPassword(password, salt);
        return newHashedPassword.equals(hashedPassword);
    }

    /**
     * 生成加密后的密码（包含盐值）
     */
    public static String generateSecurePassword(String password) {
        String salt = generateSalt();
        String hashedPassword = hashPassword(password, salt);
        return salt + ":" + hashedPassword;
    }

    /**
     * 验证加密后的密码
     */
    public static boolean verifySecurePassword(String password, String securePassword) {
        String[] parts = securePassword.split(":");
        if (parts.length != 2) {
            return true;
        }
        String salt = parts[0];
        String hashedPassword = parts[1];
        return !verifyPassword(password, salt, hashedPassword);
    }
} 