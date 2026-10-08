package main.java.com.rental.rental.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import main.java.com.rental.common.exception.NotFoundException;
import main.java.com.rental.common.exception.RentalException;
import main.java.com.rental.common.util.DBManager;
import main.java.com.rental.rental.dto.RentalCreateRequest;
import main.java.com.rental.rental.dto.RentalDetail;
import main.java.com.rental.rental.entity.Rental;
import main.java.com.rental.rental.repository.RentalRepository;
import main.java.com.rental.rental.repository.RentalRepositoryImpl;

public class RentalServiceImpl implements RentalService {

	private RentalRepository rr = new RentalRepositoryImpl();

	private RentalServiceImpl() {
	}

	public static RentalServiceImpl getInstance() {
		return new RentalServiceImpl();
	}

	@Override
	public void rentalCreate(RentalCreateRequest rentalCreateRequest) throws RentalException {
		// 실패하면 저장소가 이유(본인 물품/이미 신청/대여 중 등)를 담은 예외를 던진다
		int re = rr.rentalCreate(rentalCreateRequest);
		if (re == 0)
			throw new RentalException("생성에 실패했습니다.");
	}

	@Override
	public Rental selectByRentalNum(int rentalNum) throws NotFoundException, RentalException {
		Rental re = rr.selectByRentalNum(rentalNum);
		if (re == null)
			throw new NotFoundException();
		return re;
	}

	@Override
	public List<Rental> selectByStatus(int status) throws NotFoundException, RentalException {
		List<Rental> re = rr.selectByStatus(status);
		if (re.isEmpty())
			throw new NotFoundException();
		return re;
	}

	@Override
	public List<Rental> selectRentalRequestList() throws RentalException, NotFoundException {
		List<Rental> re = rr.selectRentalRequestList();
		if (re.isEmpty())
			throw new NotFoundException();
		return re;
	}

	@Override
	public List<Rental> selectCurrentRentalList() throws RentalException, NotFoundException {
		List<Rental> re = rr.selectCurrentRentalList();
		if (re.isEmpty())
			throw new NotFoundException();
		return re;
	}

	@Override
	public List<Rental> selectMyRentalHistory() throws RentalException, NotFoundException {
		List<Rental> rentals = rr.selectMyRentalHistory();
		if (rentals.isEmpty())
			throw new NotFoundException("대여 내역이 없습니다.");
		return rentals;
	}

	@Override
	public List<RentalDetail> selectRelatedRentalHistory() throws RentalException {
		return rr.selectRelatedRentalHistory();
	}

	@Override
	public int[] getGlobalStats() throws RentalException {
		return rr.selectGlobalStats();
	}

	@Override
	public boolean approveRental(int rentalNum, int postNum) throws RentalException {
		Connection con = null;
		try {
			con = DBManager.getConnection();
			// 트랜잭션 시작
			con.setAutoCommit(false);
			// 0. 같은 게시글에 대한 승인·신청이 동시에 처리되지 않도록 게시글 행을 먼저 잠근다
			rr.lockPost(con, postNum);
			// 1. 다른 대여 요청 거절
			if (!rr.rejectOtherRentals(con, postNum, rentalNum)) {
				con.rollback();
				return false;
			}
			// 2. 현재 대여 요청 승인
			if (!rr.approveRental(con, rentalNum)) {
				con.rollback();
				return false;
			}
			if (!rr.updateItemStatusByPost(con, postNum, false)) {
				con.rollback();
				return false;
			}
			// 둘 다 성공
			con.commit();
			return true;
		} catch (SQLException e) {
			try {
				if (con != null) {
					con.rollback();
				}
			} catch (SQLException rollbackException) {
				// rollbackException.printStackTrace();
			}
			// e.printStackTrace();
			return false;

		} finally {
			DBManager.close(con, null);
		}
	}

	@Override
	public List<Rental> getPendingApprovals() throws RentalException {
		List<Rental> re = rr.getPendingApprovals();
		if (re.isEmpty())
			throw new NotFoundException();
		return re;
	}

	@Override
	public int updateStatusRentalNum(int setStatus, int rentalNum, int status) throws RentalException {
		int result = rr.updateStatusRentalNum(setStatus, rentalNum, status);
		if (result == 0)
			throw new RentalException("실패되었습니다.");
		return result;
	}

}
