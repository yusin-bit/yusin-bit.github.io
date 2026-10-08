package main.java.com.rental.common.exception;

public class PasswordUpdateException extends RuntimeException {
	public PasswordUpdateException() {}
	public PasswordUpdateException(String message) {
		super(message);
	}
}