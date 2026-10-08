package main.java.com.rental.user.dto;

public class PasswordChangeRequest {

	private String id;
	private String phoneNo;
	private String newPassword;
	
		public PasswordChangeRequest( ) {}
	
	public PasswordChangeRequest(String id, String phoneNo, String newPassword) {
		super();
		this.id = id;
		this.phoneNo = phoneNo;
		this.newPassword = newPassword;
	}

		public String getPhoneNo() {
			return phoneNo;
		}

		public void setPhoneNo(String phoneNo) {
			this.phoneNo = phoneNo;
		}

		public String getNewPassword() {
			return newPassword;
		}

		public void setNewPassword(String newPassword) {
			this.newPassword = newPassword;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

}
