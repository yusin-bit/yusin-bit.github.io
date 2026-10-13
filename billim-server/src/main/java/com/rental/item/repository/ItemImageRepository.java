package main.java.com.rental.item.repository;

import java.util.Map;

import main.java.com.rental.common.exception.ItemException;

/** 물품 사진 (ItemImage 테이블). 사진은 목록과 따로 불러와서 목록 조회가 무거워지지 않게 한다 */
public interface ItemImageRepository {

	/** 사진 본문 */
	record ImageData(byte[] data, String contentType) { }

	/** 목록에 붙일 사진 정보. version 은 수정 시각(초)이라 바뀌면 브라우저 캐시를 새로 받는다 */
	record ImageInfo(int itemNum, long version, String credit) { }

	ImageData find(int itemNum) throws ItemException;

	/** 물품 번호 → 사진 정보 (사진 있는 물품만) */
	Map<Integer, ImageInfo> infoByItem() throws ItemException;

	/** 게시글 번호 → 그 게시글 물품의 사진 정보 (사진 있는 것만) */
	Map<Integer, ImageInfo> infoByPost() throws ItemException;

	/** 물품 주인 아이디, 물품이 없으면 null */
	String ownerOf(int itemNum) throws ItemException;

	/** 있으면 바꾸고 없으면 추가 */
	void save(int itemNum, byte[] data, String contentType, String credit) throws ItemException;

	int delete(int itemNum) throws ItemException;
}
