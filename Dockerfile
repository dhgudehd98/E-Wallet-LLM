# 경량화된 JRE 이미지 사용
#FROM eclipse-temurin:17-jre-alpine
FROM eclipse-temurin:17-jdk

WORKDIR /app

# 이미 빌드된 jar 파일 복사, ARG 단순히 COPY 인자 넘겨주는 거라는데 그냥 넣음(아무 상관 없대요)
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 컨테이너에서 열 포트
EXPOSE 8081

# 컨테이너 실행 명령
ENTRYPOINT ["java", "-jar", "app.jar"]