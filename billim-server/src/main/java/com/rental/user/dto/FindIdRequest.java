package main.java.com.rental.user.dto;

public class FindIdRequest {
    private String phoneNo;

    public FindIdRequest() {}

    public FindIdRequest(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }
}
