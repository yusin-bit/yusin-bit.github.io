# 빌림 API 서버 (Render 배포용)

KOSTA Java 미니 프로젝트(대여 서비스 "빌림")의 Java 웹 서버입니다.
화면은 같은 저장소의 [`/billim/`](../billim/) (GitHub Pages) 에서 제공되고, 이 서버는 `/api/*` 만 처리합니다.

- 구성: Java 21 `com.sun.net.httpserver` + JDBC(MySQL), 외부 프레임워크 없음
- 실행 진입점: `main.java.com.rental.web.LocalWebApplication`
- `src/` 는 원본 프로젝트(kostaJavaMiniProject)의 소스를 그대로 복사한 것입니다.

## 환경변수
| 이름 | 설명 |
|---|---|
| `DB_URL` | 예: `jdbc:mysql://<호스트>:<포트>/defaultdb?sslMode=REQUIRED` |
| `DB_USER` | DB 사용자 |
| `DB_PASSWORD` | DB 비밀번호 |
| `ALLOWED_ORIGINS` | 화면 주소. `https://yusin-bit.github.io` |
| `PORT` | Render 가 자동으로 지정 |

DB 접속 정보는 저장소에 넣지 말고 Render 대시보드의 Environment 에만 입력합니다.
