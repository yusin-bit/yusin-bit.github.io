package main.java.com.rental.user.entity;

public class User {

	private String id;                  // PK
	private String password;
	private String nickName;
	private String name;
	private String phoneNo;


	public User() {}

	public User(String id, String password, String nickName, String name, String phoneNo) {
		this.id = id;
		this.password = password;
		this.nickName = nickName;
		this.name = name;
		this.phoneNo = phoneNo;
	}


	

	public String getPassword() {
		return password;
	}


	public void setPassword(String password) {
		this.password = password;
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
	@Override
	public String toString() {
		return "[회원 정보]\n"
				+ "아이디 : " + id + "  |  닉네임 : " + nickName + "\n"
				+ "이름 : " + name + "  |  연락처 : " + phoneNo;
	}


}
