# auth-user-service

소셜/게스트 로그인, 불투명 토큰 발급·검증, 익명 사용자 조회를 담당하는 MSA 인증 서비스.

> 전체 시스템: [tainted-spring-msa](https://github.com/baekchangjoon/tainted-spring-msa)

---

## 역할

| 기능 | 설명 |
|------|------|
| 소셜 로그인 | Google · Kakao · Naver · Toss. provider별 **이기종 검증**(실제 OAuth) + provider별 `mock\|real` 토글. 기본 mock(외부 호출 없음). 자세한 내용은 아래 [소셜 로그인](#소셜-로그인-mock--real) |
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
| 통합 테스트 | Testcontainers (MySQL + Redis) + RestAssured · verifier 단위테스트(MockRestServiceServer/RSA 서명) — 전체 28 tests |
| 소셜 검증 의존성 | `com.auth0:java-jwt` + `jwks-rsa` (Google ID토큰), Spring `RestClient` (Kakao/Naver) |

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
| POST | `/api/v1/auth/login` | 소셜 로그인 — `{provider, providerToken}`. mock 모드는 `valid-<provider>-<seed>`, real 모드는 provider별 실제 토큰/코드([소셜 로그인](#소셜-로그인-mock--real)) |
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

## 소셜 로그인 (mock / real)

테스트베드 철학(의도적 이기종성)에 맞춰 **provider마다 서로 다른 실제 OAuth 검증
메커니즘**을 사용한다. `provider` 값으로 `CompositeSocialVerifier` 가 분기한다.

| Provider | OAuth 방식 | `providerToken` 의미 | 검증 내용 | secret |
|---|---|---|---|---|
| **google** | OIDC ID-token | ID token(JWT) | Google JWKS 서명검증 + `aud`/`iss`/`exp`, `sub` 추출 | 불필요(clientId만) |
| **kakao** | Authorization Code | 인가 `code` | `kauth/token` code+secret 교환 → `kapi/v2/user/me` | 필요 |
| **naver** | Access-token + userinfo | access token | `openapi/v1/nid/me` Bearer 호출 | 토큰은 클라가 획득 |
| **toss** | (mock 고정) | `valid-toss-<seed>` | 형식만 검증 (B2B 제휴 필요로 실연동 제외) | — |

### mock 모드 (기본)

자격증명 없이 동작. `providerToken` 이 `valid-<provider>-<seed>` 형식이면 통과하며
`externalId = "<provider>:<seed>"`. 기존 테스트/블랙박스가 그대로 통과한다.

```bash
curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"provider":"google","providerToken":"valid-google-42"}'
```

### real 모드 (실제 OAuth)

provider별 `SOCIAL_<PROVIDER>_MODE=real` 과 자격증명을 환경변수로 주입한다(미설정 시 mock).

```bash
# Google: ID 토큰 JWKS 검증 (secret 불필요)
SOCIAL_GOOGLE_MODE=real
SOCIAL_GOOGLE_CLIENT_ID=<google-oauth-client-id>

# Kakao: 인가 code 교환
SOCIAL_KAKAO_MODE=real
SOCIAL_KAKAO_CLIENT_ID=<rest-api-key>
SOCIAL_KAKAO_CLIENT_SECRET=<client-secret>
SOCIAL_KAKAO_REDIRECT_URI=<등록한 redirect uri>

# Naver: access token → nid/me
SOCIAL_NAVER_MODE=real
SOCIAL_NAVER_CLIENT_ID=<client-id>
SOCIAL_NAVER_CLIENT_SECRET=<client-secret>
```

real 모드에서 `providerToken` 은 각 provider 의 실제 토큰/코드여야 한다(위 표). 프론트
(`peace_of_mind`)는 provider SDK 로 해당 값을 받아 전달한다. 각 provider 콘솔에서 앱 등록 +
redirect/허용 도메인 설정이 선행되어야 한다. 설계 상세: [tainted-spring-msa
`docs/superpowers/specs/2026-06-14-real-social-login-heterogeneous-oauth.md`](https://github.com/baekchangjoon/tainted-spring-msa).

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
