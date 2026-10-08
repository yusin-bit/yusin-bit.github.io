package main.java.com.rental.user.dto;

public class UserLoginRequest {
	private String id;
	private String password;
	
	
	public UserLoginRequest(String id, String password) {
		super();
		this.id = id;
		this.password = password;
	}


	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}


	public String getPassword() {
		return password;
	}


	public void setPassword(String password) {
		this.password = password;
	}
	
	
}
