package main.java.com.rental.common.exception;

public class NotFoundException extends RuntimeException {
	public NotFoundException() {
		super("목록이 존재하지 않습니다.");
	}
	public NotFoundException(String message) {
		super(message);
	}
}
