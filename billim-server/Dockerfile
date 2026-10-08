# 빌림 Java API 서버 (Render 무료 플랜용)
# 1단계: src 전체를 javac 로 컴파일
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
ADD https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar lib/mysql-connector-j-8.4.0.jar
RUN echo "d77962877d010777cff997015da90ee689f0f4bb76848340e1488f2b83332af5  lib/mysql-connector-j-8.4.0.jar" | sha256sum -c -
COPY src ./src
RUN mkdir classes && javac -encoding UTF-8 -cp lib/mysql-connector-j-8.4.0.jar -d classes $(find src -name "*.java")

# 2단계: 실행용 JRE 이미지
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN java --list-modules | grep -q jdk.httpserver
COPY --from=build /build/classes ./classes
COPY --from=build /build/lib ./lib
# DB_URL / DB_USER / DB_PASSWORD / ALLOWED_ORIGINS 는 Render 환경변수로 넣는다. PORT 는 Render 가 자동으로 넣어준다.
ENV HOST=0.0.0.0 OPEN_BROWSER=false
EXPOSE 8787
CMD ["java", "-XX:MaxRAMPercentage=75", "-Djava.awt.headless=true", "-cp", "classes:lib/mysql-connector-j-8.4.0.jar", "main.java.com.rental.web.LocalWebApplication"]
