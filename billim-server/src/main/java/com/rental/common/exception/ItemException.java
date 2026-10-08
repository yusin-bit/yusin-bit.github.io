package main.java.com.rental.common.exception;

public class ItemException extends Exception{
	public ItemException() {
		super("물품 정보를 처리하지 못했습니다. 잠시 후 다시 시도해주세요.");
	}
	public ItemException(String message) {
		super(message);
	}
}
