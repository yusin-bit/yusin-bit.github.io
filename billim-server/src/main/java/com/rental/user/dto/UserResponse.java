package main.java.com.rental.user.dto;

public class UserResponse {
	private String id;
	private String nickName;
	private String name;
	private String phoneNo;

	public UserResponse(String id, String nickName, String name, String phoneNo) {
		this.id = id;
		this.nickName = nickName;
		this.name = name;
		this.phoneNo = phoneNo;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPhoneNo() {
		return phoneNo;
	}

	public void setPhoneNo(String phoneNo) {
		this.phoneNo = phoneNo;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}


}
