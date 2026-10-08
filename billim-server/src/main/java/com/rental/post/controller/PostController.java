package main.java.com.rental.post.controller;


import java.util.List;
import java.util.Collections;

import main.java.com.rental.common.exception.NotFoundException;
import main.java.com.rental.common.exception.PostException;
import main.java.com.rental.post.dto.AvailablePost;
import main.java.com.rental.post.dto.PostCreate;
import main.java.com.rental.post.dto.PostUpdate;
import main.java.com.rental.post.entity.Post;
import main.java.com.rental.post.service.PostService;
import main.java.com.rental.post.service.PostServiceImpl;
import main.java.com.rental.view.FailView;
import main.java.com.rental.view.SuccessView;

public class PostController {
	
	
	PostService ps = PostServiceImpl.getInstance();
	public void postCreate(PostCreate postCreate) {
		try {
			ps.postCreate(postCreate);
			SuccessView.printMessage("게시글이 생성되었습니다.");
		} catch (Exception e) {
			FailView.FailMessage(e.getMessage());
		}
	}
	
	public void postUpdate(PostUpdate postUpdate) {
		try {
			ps.postUpdate(postUpdate);
			SuccessView.printMessage("게시글이 수정되었습니다.");
		} catch (Exception e) {
			FailView.FailMessage(e.getMessage());
		}
	}
	
	public void postDelete(int postNum) {
		try {
			ps.postDelete(postNum);
			SuccessView.printMessage("게시글이 삭제되었습니다.");
		} catch (Exception e) {
			FailView.FailMessage(e.getMessage());
		}
	}
	
	public void selectAll() {
		try {
			SuccessView.printEntityList(ps.selectAll());
		} catch (Exception e) {
			FailView.FailMessage(e.getMessage());
		}
	}
	
	public List<Post> selectById(){
		try {
			List<Post> re =ps.selectById();
			SuccessView.printEntityList(re);
			return re;
		} catch (Exception e) {
			FailView.FailMessage(e.getMessage());
		}
		return Collections.emptyList();
	}
	
	public void selectByItemNum(int itemNum) {
		try {
			Post post = ps.selectByItemNum(itemNum);
			SuccessView.printEntity(post);
		} catch (Exception e) {
			FailView.FailMessage(e.getMessage());
		}
	}
	public void selectByTitleKeyword(String titleKeyword) {
		try {
			List<Post> list = ps.selectByTitleKeyword(titleKeyword);
			SuccessView.printEntityList(list);
		} catch (Exception e) {
			FailView.FailMessage(e.getMessage());
		}
	}
	public void selectByContentKeyword(String contentKeyword) {
		try {
			List<Post> list = ps.selectByContentKeyword(contentKeyword);
			SuccessView.printEntityList(list);
		} catch (Exception e) {
			FailView.FailMessage(e.getMessage());
		}
	}
	public void selectByRentDate(String rentDate) {
		try {
			List<Post> list = ps.selectByRentDate(rentDate);
			SuccessView.printEntityList(list);
		} catch (Exception e) {
			FailView.FailMessage(e.getMessage());
		}
	}
	public void selectByAddr(String addr) {
		try {
			List<Post> list = ps.selectByAddr(addr);
			SuccessView.printEntityList(list);
		} catch (Exception e) {
			FailView.FailMessage(e.getMessage());
		}
	}
	public List<AvailablePost> selectAvailablePost() {
		try {
			List<AvailablePost> list = ps.selectAvailablePost();
			return list;
		} catch (PostException | NotFoundException e) {
			FailView.FailMessage(e.getMessage());
		}
		return Collections.emptyList();
	}
	
}
