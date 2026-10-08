package main.java.com.rental.web;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import main.java.com.rental.common.exception.NotFoundException;
import main.java.com.rental.common.util.DBManager;
import main.java.com.rental.category.entity.Category;
import main.java.com.rental.category.service.CategoryServiceImpl;
import main.java.com.rental.post.dto.AvailablePost;
import main.java.com.rental.post.service.PostService;
import main.java.com.rental.post.service.PostServiceImpl;
import main.java.com.rental.item.dto.ItemCreateRequest;
import main.java.com.rental.item.entity.Item;
import main.java.com.rental.item.service.ItemService;
import main.java.com.rental.item.service.ItemServiceImpl;
import main.java.com.rental.rental.dto.RentalCreateRequest;
import main.java.com.rental.rental.dto.RentalDetail;
import main.java.com.rental.rental.entity.Rental;
import main.java.com.rental.rental.service.RentalServiceImpl;
import main.java.com.rental.session.Session;
import main.java.com.rental.user.dto.PasswordChangeRequest;
import main.java.com.rental.user.dto.UserLoginRequest;
import main.java.com.rental.user.dto.UserResponse;
import main.java.com.rental.user.dto.UserSignUpRequest;
import main.java.com.rental.user.service.UserServiceImpl;

/**
 * 별도 WAS 없이 실행되는 로컬 웹 진입점.
 * 브라우저 요청을 기존 Service -> Repository -> JDBC 흐름에 연결한다.
 */
public class LocalWebApplication {
    // 로컬 실행은 기본값 그대로, 배포 환경(Render 등)은 환경변수로 바꾼다.
    private static final String HOST = env("HOST", "127.0.0.1");
    private static final int PORT = Integer.parseInt(env("PORT", "8787"));
    private static final boolean OPEN_BROWSER = !"false".equalsIgnoreCase(env("OPEN_BROWSER", "true"));
    // 다른 도메인(예: GitHub Pages)에서 API 를 호출할 때 허용할 Origin 목록 (쉼표로 구분)
    private static final List<String> ALLOWED_ORIGINS = List.of(env("ALLOWED_ORIGINS", "").split("\\s*,\\s*"));
    private static final Path WEB_ROOT = Path.of("web").toAbsolutePath().normalize();
    private static final long SESSION_IDLE_MILLIS = 2 * 60 * 60 * 1000L;
    private static final long SESSION_MAX_MILLIS = 12 * 60 * 60 * 1000L;
    private static final int MAX_BODY_BYTES = 16 * 1024;
    private static final long MINUTE = 60 * 1000L;
    // 로그인·회원가입처럼 공격 대상이 되기 쉬운 API
    private static final List<String> AUTH_PATHS = List.of("/api/login", "/api/signup", "/api/check-id",
            "/api/find-id", "/api/password", "/api/password/reset");

    // 로그인 토큰 → 사용자. 접속한 사람마다 자기 토큰으로 따로 로그인 상태를 가진다.
    private record WebSession(UserResponse user, long createdAt, long lastSeen) { }
    private final Map<String, WebSession> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    // 요청 횟수 제한: IP 당 전체 API 분당 300회, 인증 API 분당 30회 / 아이디 당 로그인 실패 15분에 5회
    private final RequestLimiter apiLimiter = new RequestLimiter(300, MINUTE);
    private final RequestLimiter authLimiter = new RequestLimiter(30, MINUTE);
    private final RequestLimiter loginFailures = new RequestLimiter(5, 15 * MINUTE);

    private final UserServiceImpl userService = new UserServiceImpl();
    private final CategoryServiceImpl categoryService = new CategoryServiceImpl();
    private final PostService postService = PostServiceImpl.getInstance();
    private final ItemService itemService = ItemServiceImpl.getInstance();
    private final RentalServiceImpl rentalService = RentalServiceImpl.getInstance();

    public static void main(String[] args) throws Exception {
        new LocalWebApplication().start();
    }

    private void start() throws Exception {
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getByName(HOST), PORT), 0);
        } catch (IOException alreadyRunning) {
            openBrowser();
            return;
        }

        api(server, "/api/health", this::health);
        api(server, "/api/stats", this::globalStats);
        api(server, "/api/login", this::login);
        api(server, "/api/signup", this::signUp);
        api(server, "/api/check-id", this::checkId);
        api(server, "/api/find-id", this::findId);
        api(server, "/api/password", this::changePassword);
        api(server, "/api/password/reset", this::resetPassword);
        api(server, "/api/logout", this::logout);
        api(server, "/api/posts", this::availablePosts);
        api(server, "/api/items/mine", this::myItems);
        api(server, "/api/items/create", this::createItem);
        api(server, "/api/items/update", this::updateItem);
        api(server, "/api/items/delete", this::deleteItem);
        api(server, "/api/categories", this::categories);
        api(server, "/api/rentals/current", this::currentRentals);
        api(server, "/api/rentals/action", this::rentalAction);
        api(server, "/api/rentals", this::createRental);
        server.createContext("/", new StaticHandler());
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("빌림 로컬 웹 프로그램 실행: http://" + HOST + ":" + PORT);
        openBrowser();
    }

    /**
     * 모든 API 공통 처리: CORS 헤더, 사전 요청(OPTIONS) 응답,
     * Authorization 토큰으로 찾은 사용자를 이 요청의 로그인 사용자로 지정.
     */
    private void api(HttpServer server, String path, HttpHandler handler) {
        server.createContext(path, exchange -> {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            String ip = clientIp(exchange);
            boolean allowed = apiLimiter.tryAcquire(ip)
                    && (!AUTH_PATHS.contains(exchange.getRequestURI().getPath()) || authLimiter.tryAcquire(ip));
            if (!allowed) {
                tooManyRequests(exchange, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
                return;
            }
            Session.beginRequest(currentSessionUser(exchange));
            try {
                handler.handle(exchange);
            } finally {
                Session.endRequest();
            }
        });
    }

    private void addCorsHeaders(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin == null || !ALLOWED_ORIGINS.contains(origin)) return;
        var headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", origin);
        headers.set("Vary", "Origin");
        headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Authorization, Content-Type");
        headers.set("Access-Control-Max-Age", "600");
    }

    private static String bearerToken(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        return header.substring("Bearer ".length()).trim();
    }

    private UserResponse currentSessionUser(HttpExchange exchange) {
        long now = System.currentTimeMillis();
        sessions.values().removeIf(s -> now - s.lastSeen() > SESSION_IDLE_MILLIS || now - s.createdAt() > SESSION_MAX_MILLIS);
        String token = bearerToken(exchange);
        if (token == null) return null;
        WebSession session = sessions.computeIfPresent(token, (key, s) -> new WebSession(s.user(), s.createdAt(), now));
        return session == null ? null : session.user();
    }

    private String newSession(UserResponse user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        long now = System.currentTimeMillis();
        sessions.put(token, new WebSession(user, now, now));
        return token;
    }

    /** 비밀번호가 바뀐 사용자의 로그인 세션을 끊는다 (keepToken 은 유지) */
    private void endSessionsOf(String userId, String keepToken) {
        sessions.entrySet().removeIf(e -> e.getValue().user().getId().equals(userId) && !e.getKey().equals(keepToken));
    }

    /**
     * 요청한 클라이언트 IP. Render 처럼 프록시 뒤에서 실행될 때는 프록시가 붙여주는 헤더를 사용한다.
     * (X-Forwarded-For 는 클라이언트가 앞부분을 꾸밀 수 있으므로 프록시가 마지막에 붙인 값을 사용)
     */
    private static String clientIp(HttpExchange exchange) {
        var headers = exchange.getRequestHeaders();
        for (String name : List.of("CF-Connecting-IP", "True-Client-IP")) {
            String value = headers.getFirst(name);
            if (value != null && !value.isBlank()) return value.trim();
        }
        String forwarded = headers.getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            return parts[parts.length - 1].trim();
        }
        return exchange.getRemoteAddress().getAddress().getHostAddress();
    }

    private static void tooManyRequests(HttpExchange exchange, String message) throws IOException {
        exchange.getResponseHeaders().set("Retry-After", "60");
        sendError(exchange, 429, message);
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private void health(HttpExchange exchange) throws IOException {
        // 배포 환경(Render)의 헬스체크는 HEAD 요청으로 들어오므로 본문 없이 상태 코드만 돌려준다.
        boolean head = "HEAD".equalsIgnoreCase(exchange.getRequestMethod());
        if (!head && !method(exchange, "GET")) return;
        try (Connection connection = DBManager.getConnection()) {
            if (head) {
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
            }
            sendJson(exchange, 200, "{\"ok\":true,\"database\":\"" +
                    json(connection.getCatalog()) + "\"}");
        } catch (Exception e) {
            if (head) {
                exchange.sendResponseHeaders(500, -1);
                exchange.close();
                return;
            }
            // 접속 주소 같은 내부 정보가 노출되지 않도록 상세 내용은 서버 로그에만 남긴다.
            System.err.println("[health] DB 연결 실패: " + message(e));
            sendError(exchange, 500, "DB 연결 실패");
        }
    }

    private void globalStats(HttpExchange exchange) throws IOException {
        if (!method(exchange, "GET")) return;
        try {
            int[] stats = rentalService.getGlobalStats();
            sendJson(exchange, 200, "{\"ok\":true,\"available\":" + stats[0]
                    + ",\"active\":" + stats[1] + ",\"completed\":" + stats[2] + "}");
        } catch (Exception e) {
            serverError(exchange, e);
        }
    }

    private void login(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        try {
            Map<String, String> form = readForm(exchange);
            // 같은 아이디로 로그인 실패가 반복되면 일정 시간 잠근다 (비밀번호 무작위 대입 방지)
            String failureKey = form.getOrDefault("id", "").trim().toLowerCase();
            if (loginFailures.isBlocked(failureKey)) {
                tooManyRequests(exchange, "로그인 실패가 너무 많습니다. 15분 후 다시 시도해주세요.");
                return;
            }
            UserResponse user;
            try {
                user = userService.login(new UserLoginRequest(form.get("id"), form.get("password")));
            } catch (Exception loginFailed) {
                loginFailures.tryAcquire(failureKey);
                throw loginFailed;
            }
            loginFailures.reset(failureKey);
            String token = newSession(user);
            sendJson(exchange, 200, "{\"ok\":true,\"token\":\"" + token + "\",\"user\":{\"id\":\"" + json(user.getId()) +
                    "\",\"nickname\":\"" + json(user.getNickName()) + "\",\"name\":\"" +
                    json(user.getName()) + "\",\"phone\":\"" + json(user.getPhoneNo()) + "\"}}");
        } catch (Exception e) {
            sendError(exchange, 401, message(e));
        }
    }

    private void logout(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        String token = bearerToken(exchange);
        if (token != null) sessions.remove(token);
        userService.logout();
        sendJson(exchange, 200, "{\"ok\":true}");
    }

    private void signUp(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        try {
            Map<String, String> form = readForm(exchange);
            userService.signUp(new UserSignUpRequest(form.get("id"), form.get("password"),
                    form.get("nickname"), form.get("name"), form.get("phone")));
            sendJson(exchange, 201, "{\"ok\":true,\"message\":\"회원 정보가 MySQL에 저장되었습니다.\"}");
        } catch (Exception e) {
            sendError(exchange, 400, message(e));
        }
    }

    private void checkId(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        try {
            String id = readForm(exchange).get("id");
            boolean available = userService.isIdAvailable(id);
            String message = available ? "사용 가능한 아이디입니다." : "이미 사용 중인 아이디입니다.";
            sendJson(exchange, 200, "{\"ok\":true,\"available\":" + available
                    + ",\"message\":\"" + json(message) + "\"}");
        } catch (Exception e) {
            sendError(exchange, 400, message(e));
        }
    }

    private void findId(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        try {
            String id = userService.findId(readForm(exchange).get("phone"));
            sendJson(exchange, 200, "{\"ok\":true,\"id\":\"" + json(id) + "\"}");
        } catch (Exception e) {
            sendError(exchange, 404, message(e));
        }
    }

    private void changePassword(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        if (!loggedIn(exchange)) return;
        try {
            Map<String, String> form = readForm(exchange);
            String currentPassword = form.get("currentPassword");
            String newPassword = form.get("newPassword");
            String confirmation = form.get("confirmation");
            if (newPassword == null || newPassword.isBlank() || !newPassword.equals(confirmation)) {
                sendError(exchange, 400, "새 비밀번호와 확인 값이 일치하지 않습니다.");
                return;
            }

            UserResponse currentUser = Session.getInstance().getLoginUser();
            String failureKey = currentUser.getId().toLowerCase();
            if (loginFailures.isBlocked(failureKey)) {
                tooManyRequests(exchange, "비밀번호 확인 실패가 너무 많습니다. 15분 후 다시 시도해주세요.");
                return;
            }
            try {
                userService.login(new UserLoginRequest(currentUser.getId(), currentPassword));
            } catch (Exception wrongPassword) {
                loginFailures.tryAcquire(failureKey);
                throw wrongPassword;
            }
            userService.updatePassword(new PasswordChangeRequest(
                    currentUser.getId(), currentUser.getPhoneNo(), newPassword));
            // 다른 기기에 남아 있는 로그인은 끊고, 지금 쓰는 세션만 유지
            endSessionsOf(currentUser.getId(), bearerToken(exchange));
            sendJson(exchange, 200, "{\"ok\":true,\"message\":\"비밀번호가 변경되었습니다.\"}");
        } catch (Exception e) {
            sendError(exchange, 400, message(e));
        }
    }

    private void resetPassword(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        try {
            Map<String, String> form = readForm(exchange);
            String id = form.getOrDefault("id", "").trim();
            String phone = form.getOrDefault("phone", "").trim();
            String newPassword = form.get("newPassword");
            String confirmation = form.get("confirmation");
            if (newPassword == null || newPassword.isBlank() || !newPassword.equals(confirmation)) {
                sendError(exchange, 400, "새 비밀번호와 확인 값이 일치하지 않습니다.");
                return;
            }
            userService.updatePassword(new PasswordChangeRequest(id, phone, newPassword));
            // 비밀번호를 재설정하면 그 계정의 기존 로그인은 모두 끊는다
            endSessionsOf(id, null);
            sendJson(exchange, 200, "{\"ok\":true,\"message\":\"비밀번호가 변경되었습니다. 새 비밀번호로 로그인해주세요.\"}");
        } catch (Exception e) {
            sendError(exchange, 400, message(e));
        }
    }

    private void availablePosts(HttpExchange exchange) throws IOException {
        if (!method(exchange, "GET")) return;
        if (!loggedIn(exchange)) return;
        try {
            List<AvailablePost> posts = postService.selectAvailablePost();
            StringBuilder json = new StringBuilder("{\"ok\":true,\"posts\":[");
            for (int i = 0; i < posts.size(); i++) {
                AvailablePost post = posts.get(i);
                if (i > 0) json.append(',');
                json.append("{\"postNum\":").append(post.getPostNum())
                    .append(",\"title\":\"").append(json(post.getTitle()))
                    .append("\",\"content\":\"").append(json(post.getContent()))
                    .append("\",\"rentDate\":\"").append(json(post.getRentDate()))
                    .append("\",\"returnDate\":\"").append(json(post.getReturnDate()))
                    .append("\",\"addr\":\"").append(json(post.getAddr()))
                    .append("\",\"itemName\":\"").append(json(post.getItemName()))
                    .append("\",\"category\":\"").append(json(post.getCategory())).append("\"}");
            }
            sendJson(exchange, 200, json.append("]}").toString());
        } catch (NotFoundException empty) {
            sendJson(exchange, 200, "{\"ok\":true,\"posts\":[]}");
        } catch (Exception e) {
            serverError(exchange, e);
        }
    }

    private void myItems(HttpExchange exchange) throws IOException {
        if (!method(exchange, "GET")) return;
        if (!loggedIn(exchange)) return;
        try {
            String userId = Session.getInstance().getLoginUser().getId();
            List<Item> items = itemService.selectByUserId(userId);
            StringBuilder json = new StringBuilder("{\"ok\":true,\"items\":[");
            for (int i = 0; i < items.size(); i++) {
                Item item = items.get(i);
                if (i > 0) json.append(',');
                json.append("{\"itemNum\":").append(item.getItemNum())
                    .append(",\"itemName\":\"").append(json(item.getItemName()))
                    .append("\",\"available\":").append(item.isStatus())
                    .append(",\"smallCategoryCode\":\"").append(json(item.getSmallCategoryCode()))
                    .append("\"}");
            }
            sendJson(exchange, 200, json.append("]}").toString());
        } catch (Exception e) {
            serverError(exchange, e);
        }
    }

    private void createItem(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        if (!loggedIn(exchange)) return;
        try {
            Map<String, String> form = readForm(exchange);
            String itemName = form.getOrDefault("itemName", "").trim();
            String smallCategoryCode = form.getOrDefault("smallCategoryCode", "").trim();
            if (itemName.isBlank() || smallCategoryCode.isBlank())
                throw new IllegalArgumentException("물품명과 소분류를 선택해주세요.");
            String userId = Session.getInstance().getLoginUser().getId();
            int itemNum = itemService.itemInsert(new ItemCreateRequest(itemName, true, smallCategoryCode, userId));
            sendJson(exchange, 201, "{\"ok\":true,\"itemNum\":" + itemNum
                    + ",\"message\":\"물품이 DB에 등록되었습니다.\"}");
        } catch (Exception e) {
            sendError(exchange, 400, message(e));
        }
    }

    private void updateItem(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        if (!loggedIn(exchange)) return;
        try {
            Map<String, String> form = readForm(exchange);
            int itemNum = Integer.parseInt(form.getOrDefault("itemNum", "0"));
            String itemName = form.getOrDefault("itemName", "").trim();
            String smallCategoryCode = form.getOrDefault("smallCategoryCode", "").trim();
            if (itemNum <= 0 || itemName.isBlank() || smallCategoryCode.isBlank())
                throw new IllegalArgumentException("수정할 물품 정보를 확인해주세요.");
            String userId = Session.getInstance().getLoginUser().getId();
            itemService.itemUpdate(new Item(itemNum, itemName, smallCategoryCode, userId));
            sendJson(exchange, 200, "{\"ok\":true,\"message\":\"물품 정보가 수정되었습니다.\"}");
        } catch (Exception e) {
            sendError(exchange, 400, message(e));
        }
    }

    private void deleteItem(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        if (!loggedIn(exchange)) return;
        try {
            int itemNum = Integer.parseInt(readForm(exchange).getOrDefault("itemNum", "0"));
            String userId = Session.getInstance().getLoginUser().getId();
            itemService.itemDelete(itemNum, userId);
            sendJson(exchange, 200, "{\"ok\":true,\"message\":\"물품이 DB에서 삭제되었습니다.\"}");
        } catch (Exception e) {
            sendError(exchange, 400, message(e));
        }
    }

    private void categories(HttpExchange exchange) throws IOException {
        if (!method(exchange, "GET")) return;
        if (!loggedIn(exchange)) return;
        try {
            List<Category> bigCategories = categoryService.getBigCategories();
            StringBuilder json = new StringBuilder("{\"ok\":true,\"categories\":[");
            for (int i = 0; i < bigCategories.size(); i++) {
                Category big = bigCategories.get(i);
                if (i > 0) json.append(',');
                json.append("{\"code\":\"").append(json(big.getCode()))
                    .append("\",\"name\":\"").append(json(big.getName())).append("\",\"children\":[");
                List<Category> smallCategories = categoryService.getSmallCategories(big.getCode());
                for (int j = 0; j < smallCategories.size(); j++) {
                    Category small = smallCategories.get(j);
                    if (j > 0) json.append(',');
                    json.append("{\"code\":\"").append(json(small.getCode()))
                        .append("\",\"name\":\"").append(json(small.getName())).append("\"}");
                }
                json.append("]}");
            }
            sendJson(exchange, 200, json.append("]}").toString());
        } catch (Exception e) {
            serverError(exchange, e);
        }
    }

    private void createRental(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        if (!loggedIn(exchange)) return;
        try {
            Map<String, String> form = readForm(exchange);
            int postNum = Integer.parseInt(form.getOrDefault("postNum", "0"));
            String borrowerId = Session.getInstance().getLoginUser().getId();
            rentalService.rentalCreate(new RentalCreateRequest(postNum, borrowerId));
            sendJson(exchange, 201, "{\"ok\":true,\"status\":100,\"message\":\"대여 신청이 DB에 저장되었습니다.\"}");
        } catch (Exception e) {
            sendError(exchange, 400, message(e));
        }
    }

    private void currentRentals(HttpExchange exchange) throws IOException {
        if (!method(exchange, "GET")) return;
        if (!loggedIn(exchange)) return;
        try {
            List<RentalDetail> rentals = rentalService.selectRelatedRentalHistory();
            String loginId = Session.getInstance().getLoginUser().getId();
            StringBuilder json = new StringBuilder("{\"ok\":true,\"rentals\":[");
            for (int i = 0; i < rentals.size(); i++) {
                RentalDetail rental = rentals.get(i);
                if (i > 0) json.append(',');
                json.append("{\"rentalNum\":").append(rental.getRentalNum())
                    .append(",\"postNum\":").append(rental.getPostNum())
                    .append(",\"borrowerId\":\"").append(json(rental.getBorrowerId()))
                    .append("\",\"lenderId\":\"").append(json(rental.getLenderId()))
                    .append("\",\"role\":\"").append(loginId.equals(rental.getLenderId()) ? "lender" : "borrower")
                    .append("\",\"itemName\":\"").append(json(rental.getItemName()))
                    .append("\",\"title\":\"").append(json(rental.getTitle()))
                    .append("\",\"rentDate\":\"").append(json(rental.getRentDate()))
                    .append("\",\"returnDate\":\"").append(json(rental.getReturnDate()))
                    .append("\",\"addr\":\"").append(json(rental.getAddr()))
                    .append("\",\"status\":").append(rental.getStatus().getCode())
                    .append(",\"statusName\":\"").append(json(rental.getStatus().getName())).append("\"}");
            }
            sendJson(exchange, 200, json.append("]}").toString());
        } catch (Exception e) {
            serverError(exchange, e);
        }
    }

    private void rentalAction(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        if (!loggedIn(exchange)) return;
        try {
            Map<String, String> form = readForm(exchange);
            int rentalNum = Integer.parseInt(form.getOrDefault("rentalNum", "0"));
            String action = form.getOrDefault("action", "");
            String resultMessage;
            if ("approve".equals(action)) {
                Rental rental = rentalService.selectByRentalNum(rentalNum);
                if (!rentalService.approveRental(rentalNum, rental.getPostNum()))
                    throw new IllegalStateException("대여 신청 승인에 실패했습니다.");
                resultMessage = "대여 신청을 승인했습니다.";
            } else {
                int fromStatus;
                int toStatus;
                switch (action) {
                    case "reject" -> { fromStatus = 100; toStatus = 102; resultMessage = "대여 신청을 거절했습니다."; }
                    case "start" -> { fromStatus = 101; toStatus = 110; resultMessage = "물품 인도를 확인하고 대여를 시작했습니다."; }
                    case "return-request" -> { fromStatus = 110; toStatus = 200; resultMessage = "반납을 신청했습니다."; }
                    case "return-approve" -> { fromStatus = 200; toStatus = 201; resultMessage = "반납 신청을 승인했습니다."; }
                    case "return-confirm" -> { fromStatus = 201; toStatus = 210; resultMessage = "반납 거래를 확인했습니다."; }
                    case "complete" -> { fromStatus = 210; toStatus = 211; resultMessage = "반납을 최종 완료했습니다."; }
                    default -> throw new IllegalArgumentException("지원하지 않는 처리입니다.");
                }
                rentalService.updateStatusRentalNum(toStatus, rentalNum, fromStatus);
            }
            sendJson(exchange, 200, "{\"ok\":true,\"message\":\"" + json(resultMessage) + "\"}");
        } catch (Exception e) {
            sendError(exchange, 400, message(e));
        }
    }

    private boolean loggedIn(HttpExchange exchange) throws IOException {
        if (Session.getInstance().getLoginUser() != null) return true;
        sendError(exchange, 401, "먼저 로그인해주세요.");
        return false;
    }

    private boolean method(HttpExchange exchange, String expected) throws IOException {
        if (expected.equalsIgnoreCase(exchange.getRequestMethod())) return true;
        sendError(exchange, 405, "지원하지 않는 요청 방식입니다.");
        return false;
    }

    private Map<String, String> readForm(HttpExchange exchange) throws IOException {
        // 아주 큰 요청으로 메모리를 다 쓰지 않도록 본문 크기를 제한한다.
        byte[] raw = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
        if (raw.length > MAX_BODY_BYTES) throw new IllegalArgumentException("요청 내용이 너무 큽니다.");
        String body = new String(raw, StandardCharsets.UTF_8);
        Map<String, String> values = new HashMap<>();
        if (body.isBlank()) return values;
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            values.put(key, value);
        }
        return values;
    }

    private static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        sendJson(exchange, status, "{\"ok\":false,\"message\":\"" + json(message) + "\"}");
    }

    /** 예상하지 못한 서버 오류: 내부 메시지(SQL·접속 정보 등)는 로그에만 남기고 사용자에게는 일반 문구를 보낸다. */
    private static void serverError(HttpExchange exchange, Exception e) throws IOException {
        System.err.println("[" + exchange.getRequestURI().getPath() + "] " + e.getClass().getName() + ": " + message(e));
        sendError(exchange, 500, "서버 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
    }

    private static void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        addSecurityHeaders(exchange);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) { output.write(bytes); }
    }

    private static void addSecurityHeaders(HttpExchange exchange) {
        var headers = exchange.getResponseHeaders();
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("X-Frame-Options", "DENY");
        headers.set("Referrer-Policy", "no-referrer");
    }

    /** JSON 문자열 이스케이프: 따옴표·역슬래시·제어문자, 그리고 HTML 로 해석될 수 있는 < > & 까지 */
    private static String json(String value) {
        if (value == null) return "";
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '<', '>', '&' -> sb.append(String.format("\\u%04x", (int) c));
                default -> {
                    if (c < 0x20 || c == ' ' || c == ' ') sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    private static String message(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    private static void openBrowser() {
        if (!OPEN_BROWSER) return;
        try {
            URI uri = URI.create("http://" + HOST + ":" + PORT);
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(uri);
        } catch (Exception ignored) { }
    }

    private static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            if ("/".equals(requestPath)) requestPath = "/index.html";
            Path file = WEB_ROOT.resolve(requestPath.substring(1)).normalize();
            boolean head = "HEAD".equalsIgnoreCase(exchange.getRequestMethod());
            if (!file.startsWith(WEB_ROOT) || !Files.isRegularFile(file)) {
                byte[] missing = "Not found".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, head ? -1 : missing.length);
                if (!head) exchange.getResponseBody().write(missing);
                exchange.close();
                return;
            }
            String name = file.getFileName().toString();
            String type = name.endsWith(".css") ? "text/css; charset=utf-8"
                    : name.endsWith(".js") ? "application/javascript; charset=utf-8"
                    : "text/html; charset=utf-8";
            exchange.getResponseHeaders().set("Content-Type", type);
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            addSecurityHeaders(exchange);
            if (type.startsWith("text/html")) {
                // 같은 서버의 스크립트·스타일·API 만 허용, 다른 사이트에 iframe 으로 넣지 못하게
                exchange.getResponseHeaders().set("Content-Security-Policy",
                        "default-src 'self'; img-src 'self' data:; object-src 'none'; base-uri 'self'; "
                                + "form-action 'self'; frame-ancestors 'none'");
            }
            if (head) {
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
            }
            long length = Files.size(file);
            exchange.sendResponseHeaders(200, length);
            try (InputStream input = Files.newInputStream(file); OutputStream output = exchange.getResponseBody()) {
                input.transferTo(output);
            }
        }
    }
}
