package main.java.com.rental.common.util;

import java.util.List;

import main.java.com.rental.item.entity.Item;
import main.java.com.rental.post.entity.Post;
import main.java.com.rental.rental.entity.Rental;

public class ViewUtil {
	// 010-1234-5678 형태로 변환을 위한 메소드
	public static String formatPhone(String phone) {
		phone = phone.replace("-", "");
		if (phone.matches("\\d{11}")) {
			phone = phone.replaceFirst("(\\d{3})(\\d{4})(\\d{4})", "$1-$2-$3");
//            System.out.println("전화번호 : " + phoneNo);
			return phone;
		} else {
			System.out.println("올바른 휴대폰 번호를 입력해주세요.");
			return null;
		}
	}
	/**
	 * 리스트 순환 돌면서 해당하는 번호가 Item객체 반환
	 * @param num
	 * @return Item
	 */
	public static Item checkedItemNum(List<Item> list, int num) {
		if (list == null) return null;
		for (Item item : list) {
			if (item.getItemNum() == num) {
				return item;
			}
		}
		return null;
	}
	
	public static Post checkedPostNum(List<Post> list, int num) {
		if (list == null) return null;
		for (Post post : list) {
			if (post.getPostNum() == num) {
				return post;
			}
		}
		return null;
	}
	public static Rental checkedRentalNum(List<Rental> list, int num) {
		if (list == null) return null;
		for (Rental rental : list) {
			if (rental.getRentalNum() == num) {
				return rental;
			}
		}
		return null;
	}
	
}
