package main.java.com.rental.session;

import main.java.com.rental.user.dto.UserResponse;

public class Session {

    private static final Session instance = new Session();

    // 웹 요청을 처리하는 동안에는 그 요청을 보낸 사용자를 스레드별로 따로 보관한다.
    // (콘솔 프로그램처럼 요청 범위가 없을 때는 아래 loginUser 필드를 그대로 사용)
    private static final ThreadLocal<UserResponse[]> requestUser = new ThreadLocal<>();

    private UserResponse loginUser;

    private Session() {
    }

    public static Session getInstance() {
        return instance;
    }

    /** 웹 요청 시작: 토큰으로 찾은 사용자(없으면 null)를 이 요청의 로그인 사용자로 둔다. */
    public static void beginRequest(UserResponse user) {
        requestUser.set(new UserResponse[] { user });
    }

    /** 웹 요청 종료: 요청 중에 바뀐 로그인 사용자를 돌려주고 스레드 값을 지운다. */
    public static UserResponse endRequest() {
        UserResponse[] holder = requestUser.get();
        requestUser.remove();
        return holder == null ? null : holder[0];
    }

    public UserResponse getLoginUser() {
        UserResponse[] holder = requestUser.get();
        return holder != null ? holder[0] : loginUser;
    }

    public void setLoginUser(UserResponse loginUser) {
        UserResponse[] holder = requestUser.get();
        if (holder != null) holder[0] = loginUser;
        else this.loginUser = loginUser;
    }

    public void logout() {
        setLoginUser(null);
    }
}
