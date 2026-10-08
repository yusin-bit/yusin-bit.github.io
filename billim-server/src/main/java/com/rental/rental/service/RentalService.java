package main.java.com.rental.rental.service;

import java.util.List;

import main.java.com.rental.common.exception.NotFoundException;
import main.java.com.rental.common.exception.RentalException;
import main.java.com.rental.rental.dto.RentalCreateRequest;
import main.java.com.rental.rental.dto.RentalDetail;
import main.java.com.rental.rental.entity.Rental;

public interface RentalService {
	/**
	 * rental 생성
	 */
	public void rentalCreate(RentalCreateRequest rentalCreateRequest) throws RentalException;

	/**
	 * 대여 신청 현황 조회 status == 100
	 */
	public List<Rental> selectRentalRequestList() throws RentalException, NotFoundException;

	/**
	 * 현재 대여 현황 조회 status == 110
	 */
	public List<Rental> selectCurrentRentalList() throws RentalException, NotFoundException;

	/** 로그인한 임차인의 전체 대여/반납 내역 조회 */
	public List<Rental> selectMyRentalHistory() throws RentalException, NotFoundException;

	public List<RentalDetail> selectRelatedRentalHistory() throws RentalException;

	public int[] getGlobalStats() throws RentalException;

	public boolean approveRental(int rentalNum, int postNum) throws RentalException;

	/**
	 * 로그인시 대기중인 승인목록(status in(100,200)) 리스트
	 */
	public List<Rental> getPendingApprovals() throws RentalException;

	public Rental selectByRentalNum(int rentalNum) throws NotFoundException, RentalException;

	public List<Rental> selectByStatus(int status) throws NotFoundException, RentalException;

	public int updateStatusRentalNum(int setStatus, int rentalNum, int status) throws RentalException;

}
