package main.java.com.rental.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 메모리 기반 요청 횟수 제한 (서버 한 대 기준, 고정 시간 창).
 * 같은 키(IP, 아이디 등)로 windowMillis 동안 limit 번을 넘으면 막는다.
 */
final class RequestLimiter {
    private record Window(long start, int count) { }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int limit;
    private final long windowMillis;

    RequestLimiter(int limit, long windowMillis) {
        this.limit = limit;
        this.windowMillis = windowMillis;
    }

    /** 한 번 기록하고, 아직 허용 범위면 true */
    boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        cleanUp(now);
        Window window = windows.compute(key, (k, old) -> old == null || now - old.start() > windowMillis
                ? new Window(now, 1)
                : new Window(old.start(), old.count() + 1));
        return window.count() <= limit;
    }

    /** 기록하지 않고 현재 막힌 상태인지만 확인 */
    boolean isBlocked(String key) {
        Window window = windows.get(key);
        return window != null && System.currentTimeMillis() - window.start() <= windowMillis && window.count() >= limit;
    }

    void reset(String key) {
        windows.remove(key);
    }

    private void cleanUp(long now) {
        if (windows.size() > 10_000) windows.values().removeIf(w -> now - w.start() > windowMillis);
    }
}
