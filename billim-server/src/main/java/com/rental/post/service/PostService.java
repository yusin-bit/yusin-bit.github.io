package main.java.com.rental.post.service;

import java.util.List;

import main.java.com.rental.common.exception.NotFoundException;
import main.java.com.rental.common.exception.PostException;
import main.java.com.rental.post.dto.AvailablePost;
import main.java.com.rental.post.dto.PostCreate;
import main.java.com.rental.post.dto.PostUpdate;
import main.java.com.rental.post.entity.Post;

public interface PostService {
	/**
	 * 생성
	 */
	int postCreate(PostCreate postCreate)throws PostException;
	/**
	 * 수정
	 */
	int postUpdate(PostUpdate postUpdate)throws PostException;
	/**
	 * 삭제
	 */
	int postDelete(int postNum)throws PostException;
	
	/**
	 * 서치
	 */
	Post selectByItemNum(int itemNum)throws PostException;

    List<Post> selectByTitleKeyword(String titleKeyword)throws PostException;

    List<Post> selectByContentKeyword(String contentKeyword)throws PostException;

    List<Post> selectByRentDate(String rentDate)throws PostException;

    List<Post> selectByAddr(String addr)throws PostException;
    
    List<AvailablePost> selectAvailablePost() throws PostException, NotFoundException;
	List<Post> selectById() throws PostException, NotFoundException;
	List<Post> selectAll()throws PostException;
}
