# auth-user-service

소셜/게스트 로그인, 불투명 토큰 발급·검증, 익명 사용자 조회를 담당하는 MSA 인증 서비스.

> 전체 시스템: [tainted-spring-msa](https://github.com/baekchangjoon/tainted-spring-msa)

---

## 역할

| 기능 | 설명 |
|------|------|
| 소셜 로그인 | Kakao · Naver · Toss 공급자 지원 (MockSocialVerifier — 외부 호출 없음) |
| 게스트 로그인 | 별도 자격증명 없이 익명 사용자 생성 |
| 토큰 발급 | 불투명 토큰(opaque token) 생성 후 Redis에 TTL 저장 |
| 토큰 검증 | Introspection 엔드포인트로 토큰 유효성 확인 |
| 사용자 조회 | 모든 사용자의 표시명은 **"익명"** 으로 고정 |

---

## 기술 스택

| 항목 | 버전 / 값 |
|------|----------|
| Language | Java 17 |
| Build | Maven |
| Framework | Spring Boot 3.3.5 |
| 주 DB | MySQL (`authuser` 데이터베이스) |
| 캐시 / 토큰 저장 | Redis (index 0) |
| 서버 포트 | **8081** |
| 관찰성 | Spring Boot Actuator (`/actuator/health` 전용, 추적 라이브러리 없음) |
| 통합 테스트 | Testcontainers (MySQL + Redis) + RestAssured (8 tests) |

---

## 빌드 & 테스트

Docker가 실행 중이어야 Testcontainers 통합 테스트가 동작합니다.

```bash
# Java 17 홈 설정 (macOS)
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# 빌드 + 전체 테스트 (단위 + 통합)
mvn verify
```

---

## 주요 API

### 공개 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/v1/auth/login` | 소셜 로그인 — `providerToken` 형식: `valid-<provider>-<seed>` |
| POST | `/api/v1/auth/guest` | 게스트 로그인 |
| GET  | `/api/v1/me` | 내 정보 조회 (Bearer 토큰 필요) |

### 내부 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/internal/auth/verify` | 토큰 검증 (introspection) |
| GET  | `/internal/users/{id}` | 사용자 정보 조회 |

### curl 예시

```bash
# 소셜 로그인 (kakao)
curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"provider":"kakao","providerToken":"valid-kakao-42"}'

# 게스트 로그인
curl -s -X POST http://localhost:8081/api/v1/auth/guest

# 내 정보 조회
TOKEN="<발급된 토큰>"
curl -s http://localhost:8081/api/v1/me \
  -H "Authorization: Bearer $TOKEN"

# 토큰 검증 (내부)
curl -s -X POST http://localhost:8081/internal/auth/verify \
  -H "Content-Type: application/json" \
  -d '{"token":"'"$TOKEN"'"}'

# 사용자 조회 (내부)
curl -s http://localhost:8081/internal/users/1
```

---

## Docker

```bash
# 이미지 빌드
docker build -t tainted-spring/auth-user:0.1.0 .

# 실행 예시 (MySQL · Redis는 별도로 기동 필요)
docker run -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/authuser \
  -e SPRING_DATA_REDIS_HOST=host.docker.internal \
  tainted-spring/auth-user:0.1.0
```

---

## 라이선스

[MIT](LICENSE) © 2026 baekchangjoon
