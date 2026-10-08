package main.java.com.rental.common.exception;

public class RentalException extends Exception {
	public RentalException() {
		super("대여 정보를 처리하지 못했습니다. 잠시 후 다시 시도해주세요.");
	}
	public RentalException(String message) {
		super(message);
	}
	
}
