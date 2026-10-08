package main.java.com.rental.item.repository;

import java.util.List;

import main.java.com.rental.common.exception.ItemException;
import main.java.com.rental.common.exception.NotFoundException;
import main.java.com.rental.item.dto.ItemCreateRequest;
import main.java.com.rental.item.entity.Item;

public interface ItemRepository {

	// 물품 전체 목록 조회
	List<Item> itemSelect() throws NotFoundException;

	// 물품 번호 기준 단건 조회
	Item itemSelectByitemNum(int itemNum) throws NotFoundException;

	// 물품명 키워드 검색
	List<Item> itemSearch(String keyword) throws NotFoundException;

	// 물품 신규 등록
	int itemInsert(ItemCreateRequest item) throws ItemException;

	// 물품 정보 수정
	int itemUpdate(Item item) throws ItemException;

	// 대여 가능 상태 변경
	int itemUpdateStatus(int itemNum, boolean status) throws ItemException;

	// 물품 삭제
	int itemDelete(int itemNum, String userId) throws ItemException;
	
	//사용자가 등록한 아이템 리스트 조회
	public List<Item> selectByUserId(String userId)throws ItemException;
}