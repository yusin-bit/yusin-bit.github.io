package main.java.com.rental.rental.dto;

import main.java.com.rental.rental.enums.RentalStatus;

public class RentalDetail {
    private final int rentalNum;
    private final int postNum;
    private final String borrowerId;
    private final String lenderId;
    private final RentalStatus status;
    private final String itemName;
    private final String title;
    private final String rentDate;
    private final String returnDate;
    private final String addr;

    public RentalDetail(int rentalNum, int postNum, String borrowerId, String lenderId,
            RentalStatus status, String itemName, String title, String rentDate,
            String returnDate, String addr) {
        this.rentalNum = rentalNum;
        this.postNum = postNum;
        this.borrowerId = borrowerId;
        this.lenderId = lenderId;
        this.status = status;
        this.itemName = itemName;
        this.title = title;
        this.rentDate = rentDate;
        this.returnDate = returnDate;
        this.addr = addr;
    }

    public int getRentalNum() { return rentalNum; }
    public int getPostNum() { return postNum; }
    public String getBorrowerId() { return borrowerId; }
    public String getLenderId() { return lenderId; }
    public RentalStatus getStatus() { return status; }
    public String getItemName() { return itemName; }
    public String getTitle() { return title; }
    public String getRentDate() { return rentDate; }
    public String getReturnDate() { return returnDate; }
    public String getAddr() { return addr; }
}
