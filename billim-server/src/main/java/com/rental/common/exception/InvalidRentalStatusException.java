package main.java.com.rental.common.exception;

public class InvalidRentalStatusException extends RuntimeException {
	public InvalidRentalStatusException() {}
	public InvalidRentalStatusException(String message) {
		super(message);
	}
}
