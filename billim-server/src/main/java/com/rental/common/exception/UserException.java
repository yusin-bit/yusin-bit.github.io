package main.java.com.rental.common.exception;

public class UserException  extends RuntimeException {
	public UserException() {
		super("회원 정보를 처리하지 못했습니다. 잠시 후 다시 시도해주세요.");
	}
	public UserException(String message) {
		super(message);
	}
}
