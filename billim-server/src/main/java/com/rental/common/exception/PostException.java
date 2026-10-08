package main.java.com.rental.common.exception;

public class PostException extends Exception{
	public PostException() {
		super("게시글을 처리하지 못했습니다. 잠시 후 다시 시도해주세요.");
	}
	public PostException(String message) {
		super(message);
	}
}
