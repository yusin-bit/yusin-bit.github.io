package main.java.com.rental.rental.controller;

import java.util.List;
import java.util.Collections;

import main.java.com.rental.common.exception.NotFoundException;
import main.java.com.rental.common.exception.RentalException;
import main.java.com.rental.rental.dto.RentalCreateRequest;
import main.java.com.rental.rental.entity.Rental;
import main.java.com.rental.rental.service.RentalService;
import main.java.com.rental.rental.service.RentalServiceImpl;
import main.java.com.rental.view.FailView;
import main.java.com.rental.view.SuccessView;

public class RentalController {
	RentalService rs = RentalServiceImpl.getInstance();

	public void rentalCreate(RentalCreateRequest rentalCreateRequest) {
		try {
			rs.rentalCreate(rentalCreateRequest);
			SuccessView.printMessage("생성되었습니다.");
		} catch (RentalException e) {
			FailView.FailMessage(e.getMessage());
		}
	}

	// status 값 따라 출력
	public List<Rental> selectByStatus(int status) {
		try {
			List<Rental> list = rs.selectByStatus(status);
			SuccessView.printEntityList(list);
			return list;
		} catch (Exception e) {
			FailView.FailMessage(e.getMessage());
		}
		return Collections.emptyList();
	}

	/**
	 * 대여 신청 현황 조회 status == 100
	 */
	public void selectRentalRequestList() {
		try {
			List<Rental> list = rs.selectRentalRequestList();
			SuccessView.printEntityList(list);
		} catch (Exception e) {
			FailView.FailMessage(e.getMessage());
		}
	}

	/**
	 * 현재 대여 현황 조회 status == 110
	 */
	public void selectCurrentRentalList() {
		try {
			List<Rental> list = rs.selectCurrentRentalList();
			SuccessView.printEntityList(list);
		} catch (Exception e) {
			FailView.FailMessage(e.getMessage());
		}
	}

	public boolean approveRental(int rentalNum, int postNum) {
		try {
			if (rs.approveRental(rentalNum, postNum)) {
				SuccessView.printMessage("대여 승인에 성공했습니다.");
				return true;
			}
			FailView.FailMessage("대여 승인에 실패했습니다.");
		} catch (Exception e) {
			FailView.FailMessage(e.getMessage());
		}
		return false;
	}

	// set Status = ? where rentalNum = ? AND status =?
	public int updateStatusRentalNum(int setStatus, int rentalNum, int status) {
		try {
			return rs.updateStatusRentalNum(setStatus, rentalNum, status);
		} catch (RentalException e) {
			FailView.FailMessage(e.getMessage());
		}
		return 0;
	}

	// 로그인 시 현재 승인 대기중인 목록 출력
	public void getPendingApprovals() {
		try {
			List<Rental> list = rs.getPendingApprovals();
			SuccessView.printPendingApprovals(list);
		} catch (Exception e) {
			// 아무것도 없으면 출력안함.
		}
	}
}
