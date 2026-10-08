package main.java.com.rental.view;

public class MenuView {

	public MenuView() {
		// 메뉴뷰 시작 시 접근할 LoginMenuView
		LoginMenuView lmv = new LoginMenuView();
		AfterLoginView alv = null;
		while (true) {
			lmv.loginMenuView();
			if(alv ==null) alv = new AfterLoginView();
			if(alv !=null) alv.mainMenu();
		}
	}
}