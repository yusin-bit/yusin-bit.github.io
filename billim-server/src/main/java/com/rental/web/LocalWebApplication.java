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

import main.java.com.rental.admin.dto.AdminDtos.AdminAccount;
import main.java.com.rental.admin.dto.AdminDtos.CategoryRow;
import main.java.com.rental.admin.dto.AdminDtos.ItemRow;
import main.java.com.rental.admin.dto.AdminDtos.PostRow;
import main.java.com.rental.admin.dto.AdminDtos.RentalRow;
import main.java.com.rental.admin.dto.AdminDtos.Summary;
import main.java.com.rental.admin.dto.AdminDtos.UserRow;
import main.java.com.rental.admin.service.AdminService;
import main.java.com.rental.admin.service.AdminServiceImpl;
import main.java.com.rental.common.exception.NotFoundException;
import main.java.com.rental.post.dto.PostCreate;
import main.java.com.rental.post.dto.PostUpdate;
import main.java.com.rental.post.entity.Post;
import main.java.com.rental.rental.enums.RentalStatus;
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
    // 프록시 뒤에서 실행할 때 클라이언트 IP 로 믿을 헤더 이름 (예: Render → CF-Connecting-IP). 비우면 접속 주소 사용
    private static final String TRUSTED_IP_HEADER = env("TRUSTED_IP_HEADER", "");
    private static final Path WEB_ROOT = Path.of("web").toAbsolutePath().normalize();
    private static final long SESSION_IDLE_MILLIS = 2 * 60 * 60 * 1000L;
    private static final long SESSION_MAX_MILLIS = 12 * 60 * 60 * 1000L;
    private static final int MAX_BODY_BYTES = 16 * 1024;
    private static final long MINUTE = 60 * 1000L;
    // 로그인·회원가입처럼 공격 대상이 되기 쉬운 API
    private static final List<String> AUTH_PATHS = List.of("/api/login", "/api/signup", "/api/check-id",
            "/api/find-id", "/api/password", "/api/password/reset", "/api/admin/login");

    // 로그인 토큰 → 사용자. 접속한 사람마다 자기 토큰으로 따로 로그인 상태를 가진다.
    private record WebSession(UserResponse user, long createdAt, long lastSeen) { }
    private final Map<String, WebSession> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    // 요청 횟수 제한: IP 당 전체 API 분당 300회, 인증 API 분당 30회 / 아이디 당 로그인 실패 15분에 5회
    private final RequestLimiter apiLimiter = new RequestLimiter(300, MINUTE);
    private final RequestLimiter authLimiter = new RequestLimiter(30, MINUTE);
    private final RequestLimiter loginFailures = new RequestLimiter(5, 15 * MINUTE);
    // 계정 탈취·도배 방지: IP 당 회원가입 1시간 5회, 아이디 찾기 1시간 10회 / 아이디 당 비밀번호 재설정 1시간 3회
    private final RequestLimiter signupLimiter = new RequestLimiter(5, 60 * MINUTE);
    private final RequestLimiter findIdLimiter = new RequestLimiter(10, 60 * MINUTE);
    private final RequestLimiter resetLimiter = new RequestLimiter(3, 60 * MINUTE);
    // 무료 DB 용량 보호: 회원 1명당 등록 가능한 물품 수
    private static final int MAX_ITEMS_PER_USER = 30;
    // 한 계정이 동시에 유지할 수 있는 로그인 세션 수
    private static final int MAX_SESSIONS_PER_USER = 5;

    // 관리자 로그인 토큰 (일반 회원 토큰과 따로 관리 → 회원 토큰으로 관리자 API 를 쓸 수 없음)
    private record AdminSession(AdminAccount admin, long createdAt, long lastSeen) { }
    private final Map<String, AdminSession> adminSessions = new ConcurrentHashMap<>();
    private final AdminService adminService = AdminServiceImpl.getInstance();

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
        // 게시글(대여 글) 관리: 콘솔 PostMenuView 와 같은 기능
        api(server, "/api/posts/mine", this::myPosts);
        api(server, "/api/posts/create", this::createPost);
        api(server, "/api/posts/update", this::updatePost);
        api(server, "/api/posts/delete", this::deletePost);
        api(server, "/api/posts/search", this::searchPosts);
        // 관리자
        api(server, "/api/admin/login", this::adminLogin);
        api(server, "/api/admin/logout", this::adminLogout);
        api(server, "/api/admin/summary", this::adminSummary);
        api(server, "/api/admin/users", this::adminUsers);
        api(server, "/api/admin/users/reset-password", this::adminResetPassword);
        api(server, "/api/admin/posts", this::adminPosts);
        api(server, "/api/admin/posts/delete", this::adminDeletePost);
        api(server, "/api/admin/rentals", this::adminRentals);
        api(server, "/api/admin/rentals/reject", this::adminRejectRental);
        api(server, "/api/admin/users/logout", ex -> adminAction(ex, form -> {
            String userId = form.getOrDefault("userId", "").trim();
            endSessionsOf(userId, null);
            return userId + " 회원의 로그인을 모두 끊었습니다.";
        }));
        api(server, "/api/admin/users/unlock", ex -> adminAction(ex, form -> {
            String userId = form.getOrDefault("userId", "").trim();
            loginFailures.reset(userId.toLowerCase());
            resetLimiter.reset(userId.toLowerCase());
            return userId + " 회원의 로그인 잠금을 해제했습니다.";
        }));
        api(server, "/api/admin/items", this::adminItems);
        api(server, "/api/admin/items/delete", ex -> adminAction(ex, form -> {
            adminService.deleteItem(intParam(form, "itemNum"));
            return "물품을 삭제했습니다.";
        }));
        api(server, "/api/admin/items/restore", ex -> adminAction(ex, form -> {
            adminService.restoreItem(intParam(form, "itemNum"));
            return "물품을 대여 가능 상태로 되돌렸습니다.";
        }));
        api(server, "/api/admin/categories", this::adminCategories);
        api(server, "/api/admin/categories/big/create", ex -> adminAction(ex, form ->
                "대분류 " + adminService.addBigCategory(form.get("name")) + " 을(를) 추가했습니다."));
        api(server, "/api/admin/categories/small/create", ex -> adminAction(ex, form ->
                "소분류 " + adminService.addSmallCategory(form.get("bigCode"), form.get("name")) + " 을(를) 추가했습니다."));
        api(server, "/api/admin/categories/small/delete", ex -> adminAction(ex, form -> {
            adminService.deleteSmallCategory(form.getOrDefault("code", "").trim());
            return "소분류를 삭제했습니다.";
        }));
        api(server, "/api/admin/categories/big/delete", ex -> adminAction(ex, form -> {
            adminService.deleteBigCategory(form.getOrDefault("code", "").trim());
            return "대분류를 삭제했습니다.";
        }));
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
                auditAdmin(exchange, path, ip);
            }
        });
    }

    /**
     * 관리자 감사 로그: 관리자 API 의 변경 요청(POST)마다 누가·무엇을·결과를 서버 로그에 남긴다.
     * (요청 본문은 남기지 않음 → 임시 비밀번호 같은 값이 로그에 들어가지 않음)
     */
    private void auditAdmin(HttpExchange exchange, String path, String ip) {
        if (!path.startsWith("/api/admin/") || !"POST".equalsIgnoreCase(exchange.getRequestMethod())) return;
        String token = bearerToken(exchange);
        AdminSession session = token == null ? null : adminSessions.get(token);
        String who = session != null ? session.admin().id() : "-";
        System.out.println("[admin-audit] " + java.time.Instant.now() + " admin=" + who + " ip=" + ip
                + " " + path + " status=" + exchange.getResponseCode());
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
        // 같은 계정의 세션이 너무 많으면 가장 오래된 것부터 정리
        var mine = sessions.entrySet().stream()
                .filter(e -> e.getValue().user().getId().equals(user.getId()))
                .sorted((a, b) -> Long.compare(a.getValue().createdAt(), b.getValue().createdAt()))
                .map(Map.Entry::getKey).toList();
        for (int i = 0; i <= mine.size() - MAX_SESSIONS_PER_USER; i++) sessions.remove(mine.get(i));
        sessions.put(token, new WebSession(user, now, now));
        return token;
    }

    private int countMyItems(String userId) {
        try {
            return itemService.selectByUserId(userId).size();
        } catch (Exception noItems) {
            return 0;
        }
    }

    /** 숫자 파라미터: 숫자가 아니면 내부 예외 문구 대신 일반 안내를 돌려준다 */
    private static int intParam(Map<String, String> form, String key) {
        try {
            return Integer.parseInt(form.getOrDefault(key, "0").trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("요청 값이 올바르지 않습니다.");
        }
    }

    private static String phoneParam(Map<String, String> form) {
        String raw = form.getOrDefault("phone", "").trim();
        String normalized = UserServiceImpl.normalizePhone(raw);
        return normalized != null ? normalized : raw;
    }

    /** 비밀번호가 바뀐 사용자의 로그인 세션을 끊는다 (keepToken 은 유지) */
    private void endSessionsOf(String userId, String keepToken) {
        sessions.entrySet().removeIf(e -> e.getValue().user().getId().equals(userId) && !e.getKey().equals(keepToken));
    }

    /**
     * 요청한 클라이언트 IP.
     * 프록시가 넣어주는 IP 헤더는 클라이언트가 위조할 수 있으므로, 환경변수 TRUSTED_IP_HEADER 로 지정한
     * 헤더만 믿는다 (Render 는 앞단 Cloudflare 가 덮어쓰는 CF-Connecting-IP). 지정이 없으면 실제 접속 주소를 쓴다.
     */
    private static String clientIp(HttpExchange exchange) {
        if (!TRUSTED_IP_HEADER.isEmpty()) {
            String value = exchange.getRequestHeaders().getFirst(TRUSTED_IP_HEADER);
            if (value != null && !value.isBlank()) {
                String[] parts = value.split(",");
                return parts[parts.length - 1].trim();
            }
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
            if (!signupLimiter.tryAcquire(clientIp(exchange))) {
                tooManyRequests(exchange, "회원가입 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
                return;
            }
            Map<String, String> form = readForm(exchange);
            userService.signUp(new UserSignUpRequest(form.get("id"), form.get("password"),
                    form.get("nickname"), form.get("name"), phoneParam(form)));
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
            if (!findIdLimiter.tryAcquire(clientIp(exchange))) {
                tooManyRequests(exchange, "아이디 찾기 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
                return;
            }
            String id = userService.findId(phoneParam(readForm(exchange)));
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
            String phone = phoneParam(form);
            // 같은 계정에 대한 재설정 시도를 제한 (전화번호 대입으로 남의 계정을 바꾸는 것 방지)
            if (!resetLimiter.tryAcquire(id.toLowerCase())) {
                tooManyRequests(exchange, "비밀번호 재설정 시도가 너무 많습니다. 1시간 후 다시 시도해주세요.");
                return;
            }
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
            if (itemName.length() > 50) throw new IllegalArgumentException("물품명은 50자 이내로 입력해주세요.");
            String userId = Session.getInstance().getLoginUser().getId();
            if (countMyItems(userId) >= MAX_ITEMS_PER_USER)
                throw new IllegalArgumentException("물품은 1명당 " + MAX_ITEMS_PER_USER + "개까지 등록할 수 있습니다.");
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
            int itemNum = intParam(form, "itemNum");
            String itemName = form.getOrDefault("itemName", "").trim();
            String smallCategoryCode = form.getOrDefault("smallCategoryCode", "").trim();
            if (itemNum <= 0 || itemName.isBlank() || smallCategoryCode.isBlank())
                throw new IllegalArgumentException("수정할 물품 정보를 확인해주세요.");
            if (itemName.length() > 50) throw new IllegalArgumentException("물품명은 50자 이내로 입력해주세요.");
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
            int itemNum = intParam(readForm(exchange), "itemNum");
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
            int postNum = intParam(form, "postNum");
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
            int rentalNum = intParam(form, "rentalNum");
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
        } catch (NotFoundException e) {
            sendError(exchange, 400, "처리할 수 있는 대여 건이 아닙니다.");
        } catch (Exception e) {
            sendError(exchange, 400, message(e));
        }
    }

    // ===================== 게시글(대여 글) =====================

    private static final java.time.format.DateTimeFormatter DB_DATE_TIME =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 콘솔 PostMenuView.validPostInput 과 같은 규칙. 통과하면 [대여일, 반납일] 을 DB 형식으로 돌려준다 */
    private static String[] validPostInput(String title, String content, String rentDate, String returnDate, String addr) {
        if (title == null || title.isBlank() || title.length() > 50 || content == null || content.isBlank()
                || addr == null || addr.isBlank() || addr.length() > 255)
            throw new IllegalArgumentException("제목(1~50자), 설명, 주소(1~255자)를 올바르게 입력해주세요.");
        try {
            java.time.LocalDateTime start = java.time.LocalDateTime.parse(rentDate.trim().replace(' ', 'T'));
            java.time.LocalDateTime end = java.time.LocalDateTime.parse(returnDate.trim().replace(' ', 'T'));
            if (!end.isAfter(start)) throw new IllegalArgumentException("반납일은 대여일보다 늦어야 합니다.");
            return new String[] { start.format(DB_DATE_TIME), end.format(DB_DATE_TIME) };
        } catch (java.time.format.DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("대여일과 반납일을 올바르게 입력해주세요.");
        }
    }

    private static String postJson(Post post) {
        return "{\"postNum\":" + post.getPostNum() + ",\"itemNum\":" + post.getItemNum()
                + ",\"title\":\"" + json(post.getTitle()) + "\",\"content\":\"" + json(post.getContent())
                + "\",\"rentDate\":\"" + json(post.getRentDate()) + "\",\"returnDate\":\"" + json(post.getReturnDate())
                + "\",\"addr\":\"" + json(post.getAddr()) + "\"}";
    }

    /** 내 게시글 목록 (콘솔: 등록 게시글 조회) */
    private void myPosts(HttpExchange exchange) throws IOException {
        if (!method(exchange, "GET")) return;
        if (!loggedIn(exchange)) return;
        try {
            List<Post> posts;
            try {
                posts = postService.selectById();
            } catch (NotFoundException empty) {
                posts = List.of();
            }
            StringBuilder body = new StringBuilder("{\"ok\":true,\"posts\":[");
            for (int i = 0; i < posts.size(); i++) body.append(i > 0 ? "," : "").append(postJson(posts.get(i)));
            sendJson(exchange, 200, body.append("]}").toString());
        } catch (Exception e) {
            serverError(exchange, e);
        }
    }

    /** 게시글 등록 (콘솔: 새 게시글 등록 / 물품 등록 후 게시글 등록) */
    private void createPost(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        if (!loggedIn(exchange)) return;
        try {
            Map<String, String> form = readForm(exchange);
            int itemNum = intParam(form, "itemNum");
            String title = form.getOrDefault("title", "").trim();
            String content = form.getOrDefault("content", "").trim();
            String addr = form.getOrDefault("addr", "").trim();
            String[] dates = validPostInput(title, content, form.get("rentDate"), form.get("returnDate"), addr);
            PostCreate post = new PostCreate();
            post.setItemNum(itemNum);
            post.setTitle(title);
            post.setContent(content);
            post.setRentDate(dates[0]);
            post.setReturnDate(dates[1]);
            post.setAddr(addr);
            postService.postCreate(post);
            sendJson(exchange, 201, "{\"ok\":true,\"message\":\"대여 글이 등록되었습니다.\"}");
        } catch (Exception e) {
            sendError(exchange, 400, message(e));
        }
    }

    /** 게시글 수정 (콘솔: 등록 게시글 정보 수정) — 본인 물품의 글만 수정됨 */
    private void updatePost(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        if (!loggedIn(exchange)) return;
        try {
            Map<String, String> form = readForm(exchange);
            int postNum = intParam(form, "postNum");
            String title = form.getOrDefault("title", "").trim();
            String content = form.getOrDefault("content", "").trim();
            String addr = form.getOrDefault("addr", "").trim();
            String[] dates = validPostInput(title, content, form.get("rentDate"), form.get("returnDate"), addr);
            PostUpdate post = new PostUpdate();
            post.setPostNum(postNum);
            post.setTitle(title);
            post.setContent(content);
            post.setRentDate(dates[0]);
            post.setReturnDate(dates[1]);
            post.setAddr(addr);
            postService.postUpdate(post);
            sendJson(exchange, 200, "{\"ok\":true,\"message\":\"대여 글이 수정되었습니다.\"}");
        } catch (Exception e) {
            sendError(exchange, 400, message(e));
        }
    }

    /** 게시글 삭제 (콘솔: 등록 게시글 삭제) — 본인 물품의 글만, 대여 내역이 있으면 삭제 불가 */
    private void deletePost(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        if (!loggedIn(exchange)) return;
        try {
            postService.postDelete(intParam(readForm(exchange), "postNum"));
            sendJson(exchange, 200, "{\"ok\":true,\"message\":\"대여 글이 삭제되었습니다.\"}");
        } catch (Exception e) {
            sendError(exchange, 400, message(e));
        }
    }

    /**
     * 게시글 검색 (콘솔: 게시글 검색 — 물품 번호/제목/내용/대여일/주소)
     * 대여 화면에서 쓰므로 지금 빌릴 수 있는 글만 돌려준다.
     */
    private void searchPosts(HttpExchange exchange) throws IOException {
        if (!method(exchange, "GET")) return;
        if (!loggedIn(exchange)) return;
        try {
            Map<String, String> query = queryParams(exchange);
            String type = query.getOrDefault("type", "");
            String keyword = query.getOrDefault("q", "").trim();
            if (keyword.isEmpty() || keyword.length() > 100) throw new IllegalArgumentException("검색어를 1~100자로 입력해주세요.");
            List<Post> found;
            try {
                found = switch (type) {
                    case "title" -> postService.selectByTitleKeyword(keyword);
                    case "content" -> postService.selectByContentKeyword(keyword);
                    case "addr" -> postService.selectByAddr(keyword);
                    case "rentDate" -> postService.selectByRentDate(java.time.LocalDate.parse(keyword).toString());
                    case "itemNum" -> {
                        Post post = postService.selectByItemNum(Integer.parseInt(keyword));
                        yield post == null ? List.of() : List.of(post);
                    }
                    default -> throw new IllegalArgumentException("검색 조건을 선택해주세요.");
                };
            } catch (NotFoundException empty) {
                found = List.of();
            } catch (java.time.format.DateTimeParseException | NumberFormatException badKeyword) {
                throw new IllegalArgumentException("대여일은 2026-10-08, 물품 번호는 숫자로 입력해주세요.");
            }
            java.util.Set<Integer> matched = new java.util.HashSet<>();
            for (Post post : found) matched.add(post.getPostNum());
            List<AvailablePost> available;
            try {
                available = postService.selectAvailablePost();
            } catch (NotFoundException empty) {
                available = List.of();
            }
            StringBuilder body = new StringBuilder("{\"ok\":true,\"posts\":[");
            boolean first = true;
            for (AvailablePost post : available) {
                if (!matched.contains(post.getPostNum())) continue;
                body.append(first ? "" : ",");
                first = false;
                body.append("{\"postNum\":").append(post.getPostNum())
                    .append(",\"title\":\"").append(json(post.getTitle()))
                    .append("\",\"content\":\"").append(json(post.getContent()))
                    .append("\",\"rentDate\":\"").append(json(post.getRentDate()))
                    .append("\",\"returnDate\":\"").append(json(post.getReturnDate()))
                    .append("\",\"addr\":\"").append(json(post.getAddr()))
                    .append("\",\"itemName\":\"").append(json(post.getItemName()))
                    .append("\",\"category\":\"").append(json(post.getCategory())).append("\"}");
            }
            sendJson(exchange, 200, body.append("]}").toString());
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, message(e));
        } catch (Exception e) {
            serverError(exchange, e);
        }
    }

    private static Map<String, String> queryParams(HttpExchange exchange) {
        Map<String, String> values = new HashMap<>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null || raw.isBlank()) return values;
        try {
            for (String pair : raw.split("&")) {
                String[] parts = pair.split("=", 2);
                values.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "");
            }
        } catch (IllegalArgumentException badEncoding) {
            throw new IllegalArgumentException("요청 형식이 올바르지 않습니다.");
        }
        return values;
    }

    // ===================== 관리자 =====================

    /** 관리자 토큰으로 로그인한 관리자를 찾는다. 없으면 401 응답 후 null */
    private AdminAccount requireAdmin(HttpExchange exchange) throws IOException {
        long now = System.currentTimeMillis();
        adminSessions.values().removeIf(s -> now - s.lastSeen() > SESSION_IDLE_MILLIS || now - s.createdAt() > SESSION_MAX_MILLIS);
        String token = bearerToken(exchange);
        AdminSession session = token == null ? null
                : adminSessions.computeIfPresent(token, (key, s) -> new AdminSession(s.admin(), s.createdAt(), now));
        if (session != null) return session.admin();
        sendError(exchange, 401, "관리자 로그인이 필요합니다.");
        return null;
    }

    private void adminLogin(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        try {
            Map<String, String> form = readForm(exchange);
            String failureKey = "admin:" + form.getOrDefault("id", "").trim().toLowerCase();
            if (loginFailures.isBlocked(failureKey)) {
                tooManyRequests(exchange, "로그인 실패가 너무 많습니다. 15분 후 다시 시도해주세요.");
                return;
            }
            AdminAccount admin;
            try {
                admin = adminService.login(form.get("id"), form.get("password"));
            } catch (Exception loginFailed) {
                loginFailures.tryAcquire(failureKey);
                throw loginFailed;
            }
            loginFailures.reset(failureKey);
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            long now = System.currentTimeMillis();
            adminSessions.put(token, new AdminSession(admin, now, now));
            sendJson(exchange, 200, "{\"ok\":true,\"token\":\"" + token + "\",\"admin\":{\"id\":\""
                    + json(admin.id()) + "\",\"name\":\"" + json(admin.name()) + "\"}}");
        } catch (Exception e) {
            sendError(exchange, 401, message(e));
        }
    }

    private void adminLogout(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        String token = bearerToken(exchange);
        if (token != null) adminSessions.remove(token);
        sendJson(exchange, 200, "{\"ok\":true}");
    }

    private void adminSummary(HttpExchange exchange) throws IOException {
        if (!method(exchange, "GET")) return;
        if (requireAdmin(exchange) == null) return;
        try {
            Summary s = adminService.summary();
            sendJson(exchange, 200, "{\"ok\":true,\"summary\":{\"users\":" + s.users() + ",\"items\":" + s.items()
                    + ",\"posts\":" + s.posts() + ",\"rentals\":" + s.rentals() + ",\"requested\":" + s.requested()
                    + ",\"inProgress\":" + s.inProgress() + ",\"completed\":" + s.completed()
                    + ",\"rejected\":" + s.rejected() + "}}");
        } catch (Exception e) {
            serverError(exchange, e);
        }
    }

    private void adminUsers(HttpExchange exchange) throws IOException {
        if (!method(exchange, "GET")) return;
        if (requireAdmin(exchange) == null) return;
        try {
            List<UserRow> users = adminService.users();
            StringBuilder body = new StringBuilder("{\"ok\":true,\"users\":[");
            for (int i = 0; i < users.size(); i++) {
                UserRow u = users.get(i);
                body.append(i > 0 ? "," : "").append("{\"id\":\"").append(json(u.id()))
                    .append("\",\"nickname\":\"").append(json(u.nickName()))
                    .append("\",\"name\":\"").append(json(u.name()))
                    .append("\",\"phone\":\"").append(json(u.phone()))
                    .append("\",\"itemCount\":").append(u.itemCount())
                    .append(",\"rentalCount\":").append(u.rentalCount())
                    .append(",\"locked\":").append(loginFailures.isBlocked(u.id().toLowerCase()))
                    .append(",\"sessions\":").append(sessions.values().stream().filter(s -> s.user().getId().equals(u.id())).count())
                    .append('}');
            }
            sendJson(exchange, 200, body.append("]}").toString());
        } catch (Exception e) {
            serverError(exchange, e);
        }
    }

    /** 회원 임시 비밀번호 발급: 비밀번호를 잊은 회원을 관리자가 도와줄 때 사용. 그 회원의 로그인은 모두 끊는다 */
    private void adminResetPassword(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        if (requireAdmin(exchange) == null) return;
        try {
            Map<String, String> form = readForm(exchange);
            String userId = form.getOrDefault("userId", "").trim();
            adminService.resetUserPassword(userId, form.get("newPassword"));
            endSessionsOf(userId, null);
            loginFailures.reset(userId.toLowerCase());
            sendJson(exchange, 200, "{\"ok\":true,\"message\":\"임시 비밀번호를 설정했습니다.\"}");
        } catch (Exception e) {
            sendError(exchange, 400, message(e));
        }
    }

    private void adminPosts(HttpExchange exchange) throws IOException {
        if (!method(exchange, "GET")) return;
        if (requireAdmin(exchange) == null) return;
        try {
            List<PostRow> posts = adminService.posts();
            StringBuilder body = new StringBuilder("{\"ok\":true,\"posts\":[");
            for (int i = 0; i < posts.size(); i++) {
                PostRow p = posts.get(i);
                body.append(i > 0 ? "," : "").append("{\"postNum\":").append(p.postNum())
                    .append(",\"title\":\"").append(json(p.title()))
                    .append("\",\"itemName\":\"").append(json(p.itemName()))
                    .append("\",\"lenderId\":\"").append(json(p.lenderId()))
                    .append("\",\"rentDate\":\"").append(json(p.rentDate()))
                    .append("\",\"returnDate\":\"").append(json(p.returnDate()))
                    .append("\",\"addr\":\"").append(json(p.addr()))
                    .append("\",\"available\":").append(p.available())
                    .append(",\"rentalCount\":").append(p.rentalCount()).append('}');
            }
            sendJson(exchange, 200, body.append("]}").toString());
        } catch (Exception e) {
            serverError(exchange, e);
        }
    }

    private void adminDeletePost(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        if (requireAdmin(exchange) == null) return;
        try {
            adminService.deletePost(intParam(readForm(exchange), "postNum"));
            sendJson(exchange, 200, "{\"ok\":true,\"message\":\"게시글을 삭제했습니다.\"}");
        } catch (Exception e) {
            sendError(exchange, 400, message(e));
        }
    }

    private void adminRentals(HttpExchange exchange) throws IOException {
        if (!method(exchange, "GET")) return;
        if (requireAdmin(exchange) == null) return;
        try {
            List<RentalRow> rentals = adminService.rentals();
            StringBuilder body = new StringBuilder("{\"ok\":true,\"rentals\":[");
            for (int i = 0; i < rentals.size(); i++) {
                RentalRow r = rentals.get(i);
                String statusName;
                try {
                    statusName = RentalStatus.fromCode(r.status()).getName();
                } catch (Exception unknown) {
                    statusName = String.valueOf(r.status());
                }
                body.append(i > 0 ? "," : "").append("{\"rentalNum\":").append(r.rentalNum())
                    .append(",\"postNum\":").append(r.postNum())
                    .append(",\"itemName\":\"").append(json(r.itemName()))
                    .append("\",\"lenderId\":\"").append(json(r.lenderId()))
                    .append("\",\"borrowerId\":\"").append(json(r.borrowerId()))
                    .append("\",\"status\":").append(r.status())
                    .append(",\"statusName\":\"").append(json(statusName)).append("\"}");
            }
            sendJson(exchange, 200, body.append("]}").toString());
        } catch (Exception e) {
            serverError(exchange, e);
        }
    }

    /** 관리자 POST 작업 공통 처리: 관리자 확인 → 작업 실행 → 결과 메시지 응답 */
    @FunctionalInterface
    private interface AdminWork {
        String run(Map<String, String> form) throws Exception;
    }

    private void adminAction(HttpExchange exchange, AdminWork work) throws IOException {
        if (!method(exchange, "POST")) return;
        if (requireAdmin(exchange) == null) return;
        try {
            String resultMessage = work.run(readForm(exchange));
            sendJson(exchange, 200, "{\"ok\":true,\"message\":\"" + json(resultMessage) + "\"}");
        } catch (Exception e) {
            sendError(exchange, 400, message(e));
        }
    }

    private void adminItems(HttpExchange exchange) throws IOException {
        if (!method(exchange, "GET")) return;
        if (requireAdmin(exchange) == null) return;
        try {
            List<ItemRow> items = adminService.items();
            StringBuilder body = new StringBuilder("{\"ok\":true,\"items\":[");
            for (int i = 0; i < items.size(); i++) {
                ItemRow it = items.get(i);
                body.append(i > 0 ? "," : "").append("{\"itemNum\":").append(it.itemNum())
                    .append(",\"itemName\":\"").append(json(it.itemName()))
                    .append("\",\"lenderId\":\"").append(json(it.lenderId()))
                    .append("\",\"category\":\"").append(json(it.bigCategory() + " · " + it.smallCategory()))
                    .append("\",\"available\":").append(it.available())
                    .append(",\"postCount\":").append(it.postCount())
                    .append(",\"activeRentals\":").append(it.activeRentals()).append('}');
            }
            sendJson(exchange, 200, body.append("]}").toString());
        } catch (Exception e) {
            serverError(exchange, e);
        }
    }

    private void adminCategories(HttpExchange exchange) throws IOException {
        if (!method(exchange, "GET")) return;
        if (requireAdmin(exchange) == null) return;
        try {
            List<CategoryRow> rows = adminService.categories();
            StringBuilder body = new StringBuilder("{\"ok\":true,\"categories\":[");
            for (int i = 0; i < rows.size(); i++) {
                CategoryRow c = rows.get(i);
                body.append(i > 0 ? "," : "").append("{\"bigCode\":\"").append(json(c.bigCode()))
                    .append("\",\"bigName\":\"").append(json(c.bigName()))
                    .append("\",\"smallCode\":").append(c.smallCode() == null ? "null" : "\"" + json(c.smallCode()) + "\"")
                    .append(",\"smallName\":").append(c.smallName() == null ? "null" : "\"" + json(c.smallName()) + "\"")
                    .append(",\"itemCount\":").append(c.itemCount()).append('}');
            }
            sendJson(exchange, 200, body.append("]}").toString());
        } catch (Exception e) {
            serverError(exchange, e);
        }
    }

    private void adminRejectRental(HttpExchange exchange) throws IOException {
        if (!method(exchange, "POST")) return;
        if (requireAdmin(exchange) == null) return;
        try {
            adminService.rejectRental(intParam(readForm(exchange), "rentalNum"));
            sendJson(exchange, 200, "{\"ok\":true,\"message\":\"대여 신청을 거절 처리했습니다.\"}");
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
        try {
            for (String pair : body.split("&")) {
                String[] parts = pair.split("=", 2);
                String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                String value = parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
                values.put(key, value);
            }
        } catch (IllegalArgumentException badEncoding) {
            throw new IllegalArgumentException("요청 형식이 올바르지 않습니다.");
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
