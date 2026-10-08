package main.java.com.rental.common.exception;

public class AdminException extends Exception{
	public AdminException() {
		super("관리자 요청을 처리하지 못했습니다. 잠시 후 다시 시도해주세요.");
	}
	public AdminException(String message) {
		super(message);
	}
}
