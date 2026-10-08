package main.java.com.rental.common.util;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * 비밀번호 해시 유틸 (JDK 기본 PBKDF2 사용, 외부 라이브러리 없음)
 * 저장 형식: pbkdf2$반복횟수$솔트(Base64)$해시(Base64)
 * 예전처럼 평문으로 저장된 비밀번호도 matches()로 확인할 수 있고,
 * needsUpgrade()가 true 이면 로그인 성공 후 해시로 바꿔 저장한다.
 */
public final class PasswordHasher {
	private static final String PREFIX = "pbkdf2$";
	private static final int ITERATIONS = 100_000;
	private static final int SALT_BYTES = 16;
	private static final int HASH_BITS = 256;
	private static final SecureRandom RANDOM = new SecureRandom();

	private PasswordHasher() {
	}

	public static String hash(String rawPassword) {
		byte[] salt = new byte[SALT_BYTES];
		RANDOM.nextBytes(salt);
		byte[] hash = pbkdf2(rawPassword, salt, ITERATIONS);
		Base64.Encoder encoder = Base64.getEncoder();
		return PREFIX + ITERATIONS + "$" + encoder.encodeToString(salt) + "$" + encoder.encodeToString(hash);
	}

	public static boolean matches(String rawPassword, String stored) {
		if (rawPassword == null || stored == null) return false;
		if (!stored.startsWith(PREFIX)) {
			// 해시 적용 전 평문 데이터
			return MessageDigest.isEqual(rawPassword.getBytes(), stored.getBytes());
		}
		String[] parts = stored.split("\\$");
		if (parts.length != 4) return false;
		int iterations = Integer.parseInt(parts[1]);
		Base64.Decoder decoder = Base64.getDecoder();
		byte[] salt = decoder.decode(parts[2]);
		byte[] expected = decoder.decode(parts[3]);
		return MessageDigest.isEqual(expected, pbkdf2(rawPassword, salt, iterations));
	}

	public static boolean needsUpgrade(String stored) {
		return stored != null && !stored.startsWith(PREFIX);
	}

	private static byte[] pbkdf2(String rawPassword, byte[] salt, int iterations) {
		try {
			KeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, HASH_BITS);
			return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
		} catch (Exception e) {
			throw new IllegalStateException("비밀번호 해시 처리에 실패했습니다.", e);
		}
	}
}
