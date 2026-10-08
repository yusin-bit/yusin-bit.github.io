package main.java.com.rental.post.service;

import java.util.List;

import main.java.com.rental.common.exception.NotFoundException;
import main.java.com.rental.common.exception.PostException;
import main.java.com.rental.post.dto.AvailablePost;
import main.java.com.rental.post.dto.PostCreate;
import main.java.com.rental.post.dto.PostUpdate;
import main.java.com.rental.post.entity.Post;
import main.java.com.rental.post.repository.PostRepository;
import main.java.com.rental.post.repository.PostRepositoryImpl;

public class PostServiceImpl implements PostService{
	
	private PostServiceImpl() {	}
	public static PostService getInstance() {
		return new PostServiceImpl();
	}
	
	PostRepository pr = new PostRepositoryImpl();
	@Override
	public int postCreate(PostCreate postCreate) throws PostException {
		int re = pr.postCreate(postCreate);
		if(re==0) throw new PostException("생성에 실패했습니다. ");
		return re;
	}

	@Override
	public int postUpdate(PostUpdate postUpdate) throws PostException {
		int re = pr.postUpdate(postUpdate);
		if(re==0) throw new PostException("업데이트에 실패했습니다.");
		return re;
	}

	@Override
	public int postDelete(int postNum) throws PostException {
		int re = pr.postDelete(postNum);
		if(re==0) throw new PostException("삭제에 실패했습니다.");
		return re;
	}

	@Override
	public List<Post> selectAll() throws PostException, NotFoundException{
		List<Post> list = pr.selectAll();
		if(list.isEmpty()) throw new NotFoundException("검색결과가 존재하지 않습니다."); 
		return list;
	}
	@Override
	public List<Post> selectById() throws PostException, NotFoundException {
		List<Post> list = pr.selectById();
		if(list.isEmpty()) throw new NotFoundException("검색결과가 존재하지 않습니다."); 
		return list;
	}
	@Override
	public Post selectByItemNum(int itemNum) throws PostException {
		Post post = pr.selectByItemNum(itemNum);
		if(post==null) throw new NotFoundException("검색결과가 존재하지 않습니다."); 
		return post;
	}

	@Override
	public List<Post> selectByTitleKeyword(String titleKeyword) throws PostException {
		List<Post> list = pr.selectByTitleKeyword(titleKeyword);
		if(list.isEmpty()) throw new NotFoundException("검색결과가 존재하지 않습니다."); 
		return list;
	}

	@Override
	public List<Post> selectByContentKeyword(String contentKeyword) throws PostException {
		List<Post> list = pr.selectByContentKeyword(contentKeyword);
		if(list.isEmpty()) throw new NotFoundException("검색결과가 존재하지 않습니다."); 
		return list;
	}

	@Override
	public List<Post> selectByRentDate(String rentDate) throws PostException {
		List<Post> list = pr.selectByRentDate(rentDate);
		if(list.isEmpty()) throw new NotFoundException("검색결과가 존재하지 않습니다."); 
		return list;
	}

	@Override
	public List<Post> selectByAddr(String addr) throws PostException {
		List<Post> list = pr.selectByAddr(addr);
		if(list.isEmpty()) throw new NotFoundException("검색결과가 존재하지 않습니다."); 
		return list;
	}
	
	@Override
	public List<AvailablePost> selectAvailablePost() throws PostException, NotFoundException{
		List<AvailablePost> list = pr.selectAvailablePost();
		if(list.isEmpty()) throw new NotFoundException("검색결과가 존재하지 않습니다."); 
		return list;
	}

}
