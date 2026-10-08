package main.java.com.rental.category.controller;

import java.util.List;
import java.util.Collections;

import main.java.com.rental.category.entity.Category;
import main.java.com.rental.category.service.CategoryService;
import main.java.com.rental.category.service.CategoryServiceImpl;
import main.java.com.rental.common.exception.NotFoundException;
import main.java.com.rental.view.FailView;

public class CategoryController {

	public CategoryService categoryService = new CategoryServiceImpl();

	// 대분류 카테고리 목록 조회
	public List<Category> getBigCategories() {
		try {
			return categoryService.getBigCategories();
		} catch (NotFoundException e) {
			FailView.FailMessage(e.getMessage());
			return Collections.emptyList();
		}
	}

	// 선택한 대분류에 속한 소분류 카테고리 목록 조회
	public List<Category> getSmallCategories(String bigCategoryCode) {
		try {
			return categoryService.getSmallCategories(bigCategoryCode);
		} catch (NotFoundException e) {
			FailView.FailMessage(e.getMessage());
			return Collections.emptyList();
		}
	}
}
