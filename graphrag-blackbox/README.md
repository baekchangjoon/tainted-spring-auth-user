# auth-user — graph-rag 블랙박스 테스트 (Method 1)

`graph-rag-test-generator`(도구 1 = builder / 도구 2 = test-generator)를 **analysis 모드**로
`auth-user` 마이크로서비스에 적용해 out-of-process 블랙박스 RestAssured 테스트를 결정적으로
생성하고, MySQL + Redis + JWT 인증이 실제로 동작하는 docker-compose 환경에서 GREEN으로 실행한
산출물이다. auth-user는 이 MSA에서 **MySQL + Redis + JWT 인증**을 모두 갖춘 첫 서비스다.

생성 도구 안에는 LLM이 없다. 도구 1이 SUT를 외부 프로세스로 띄워 호출해 보며 사실
(엔드포인트·분기·발행 SQL·Redis 명령·DB 스키마)을 `graph.json`으로 캡처하고, 도구 2가 그
그래프로 RestAssured 테스트를 합성한다.

## 결과 요약

- **엔드포인트 5개** 인덱싱, **14 path** 캡처 → **테스트 클래스 4개 / 테스트 7개**.
- **7 passed / 0 skipped / 0 failed / 0 errors** — quarantine 없음(`KNOWN-LIMITATIONS.md` 불필요).
- 캡처: **MySQL SELECT 2건**(`user_account`) + **Redis GET 2건**(`auth:token:*`) + Kafka 0(해당
  서비스는 Kafka 미발행 — N/A).

| 테스트 클래스 | 엔드포인트 | 시나리오 | authMode |
|---|---|---|---|
| `GetApiV1MeTest` | `GET /api/v1/me` | s200_1 | `REAL` (Bearer 필요) |
| `GetInternalUsersByIdTest` | `GET /internal/users/{id}` | s404_1, s404_2 | `DISABLED` |
| `PostApiV1AuthLoginTest` | `POST /api/v1/auth/login` | s401_1, s400_1 | `DISABLED` |
| `PostInternalAuthVerifyTest` | `POST /internal/auth/verify` | s200_1, s400_1 | `DISABLED` |

> `POST /api/v1/auth/guest` 는 도구 1이 **스킵**한다(`no @RequestBody shape and no path param` —
> 본문도 path 변수도 없는 POST라 요청을 합성할 수 없음). 이 엔드포인트는 테스트로 생성되지 않고
> **인증 토큰 소스**로만 쓰인다(아래 "인증 처리" 참고).

## 인증 처리 (핵심)

auth-user는 자기 자신이 인증 서비스다. 보호 엔드포인트(`GET /api/v1/me`)는
`Authorization: Bearer <token>` 을 요구한다. 토큰은 **게스트 로그인**으로 얻는다 — 외부 소셜 IdP
없이 자격증명 없이 토큰을 발급하는 가장 단순한 경로다.

- `POST /api/v1/auth/guest` 는 **본문 없이** 호출하면 `{"accessToken","displayName"}` 를 반환한다.
  builder/testlib의 로그인 클라이언트는 `{"username","password"}` 본문을 POST하는데, guest는 이
  여분 필드를 무시(Jackson)하고 정상적으로 토큰을 발급한다. 따라서 별도 패치 없이 그대로 쓸 수 있다.
- 응답의 토큰 필드명이 기본값 `token` 이 아니라 **`accessToken`** 이므로
  `--auth-token-field accessToken`(도구 1) / `AUTH_TOKEN_FIELD=accessToken`(실행) 으로 지정한다.

**도구 1(탐색)** 은 `--auth-login-path /api/v1/auth/guest --auth-token-field accessToken` 로
시작 시 1회 게스트 로그인해 Bearer 토큰을 캐시하고, 보호 엔드포인트 탐색에 재사용한다
(`negative-auth get-api-v1-me -> status 401` 로 무토큰 경로도 확인).

**실행(생성 테스트)** 은 `.authenticated()` 가 `AUTH_ADAPTER=real` → `JwtAuthClient` 로 매 실행 시
게스트 로그인해 신선한 토큰을 붙인다. 공개 엔드포인트(login/verify/users)는 그래프가
`authRequired:true` 로 표시해 생성 테스트에 `.authenticated()` 가 붙지만, 해당 엔드포인트는 여분
Bearer 헤더를 무시하므로 무해하다.

소셜 로그인(`POST /api/v1/auth/login`, provider=google/kakao/naver/toss)은 실제 외부 IdP 검증이
필요해 happy-path를 실연동으로 도달할 수 없다. 대신 도구 1은 **결정적 음성 경로**(401 unsupported
provider, 400 validation)를 합성한다. mock 모드(`SOCIAL_*_MODE=mock`, `valid-<provider>-<seed>`
토큰)로 happy-path를 확장할 수 있으나, 본 산출물은 외부 의존 없는 결정적 경로만 커버한다.

## JWT 결정성

토큰(`accessToken`)·사용자 UUID(`userId`)·`createdAt` 은 비결정적이다. 다행히 도구 2는 이들을
**하드코딩하지 않는다** — `GET /api/v1/me` 의 200 경로는 `userId`/`displayName`/`socialProvider`
를 `notNullValue()` 로만 단언한다(캡처된 UUID 미사용). 나머지 경로는 모두 결정적 에러 바디
(401/400/404)다. 따라서 **JWT/UUID 비결정성으로 인한 quarantine 대상이 없다**.

## 정확한 재현 절차

전제: Docker 실행 중, JDK 17(corretto-17), graph-rag 저장소 클론
(`/Users/changjoonbaek/github_graph-rag-test-generator/graph-rag`, jar 빌드됨).

```bash
export JAVA_HOME=/Users/changjoonbaek/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home

# 0) SUT jar 빌드 (Maven, JDK 17) — target/auth-user-service-0.1.0.jar
cd /Users/changjoonbaek/github_tainted-spring/tainted-spring-auth-user
mvn -q -DskipTests package

# 1) 도구 1 (builder, analysis 모드): MySQL + Redis + 게스트 로그인 인증 주입
cd /Users/changjoonbaek/github_graph-rag-test-generator/graph-rag
W=$PWD/.work/authuser ; rm -rf "$W/graph-out" ; mkdir -p "$W"
./gradlew :graph-rag-builder:run --args="build \
  --sut-src   /Users/changjoonbaek/github_tainted-spring/tainted-spring-auth-user/src/main/java \
  --sut-resources /Users/changjoonbaek/github_tainted-spring/tainted-spring-auth-user/src/main/resources \
  --sut-jar   /Users/changjoonbaek/github_tainted-spring/tainted-spring-auth-user/target/auth-user-service-0.1.0.jar \
  --sut-compose /Users/changjoonbaek/github_tainted-spring/tainted-spring-platform/docker-compose.yml \
  --sut-id authuser --db-service mysql --with-redis \
  --sut-java-home $JAVA_HOME \
  --auth-login-path /api/v1/auth/guest --auth-user guest --auth-pass guest --auth-token-field accessToken \
  --commit-sha authuser-m1 \
  --out $W/graph-out"

# 2) 도구 2 (test-generator): 엔드포인트별 테스트 생성
#    requests/*.json 의 endpointId/testClassName/packageName/authMode 사용
mkdir -p "$W/requests" ; rm -rf "$W/generated"
# (이 디렉터리의 requests/req-*.json 을 $W/requests/ 로 복사해 사용)
for r in req-me req-users-id req-login req-verify; do
  ./gradlew -q :test-generator:run --args="generate \
    --request $W/requests/$r.json --graph $W/graph-out --out $W/generated"
done

# 3) docker-compose 기동 (MySQL + Redis + auth-user; project 이름 grauthuser)
cd /Users/changjoonbaek/github_tainted-spring/tainted-spring-platform
docker compose -p grauthuser -f docker-compose.yml up -d --build auth-user
# auth-user health 대기
for i in $(seq 1 40); do curl -fsS http://localhost:8081/actuator/health 2>/dev/null | grep -q UP && break; sleep 3; done

# 4) 생성 테스트를 e2e 하니스에 복사 후 실행
cd /Users/changjoonbaek/github_graph-rag-test-generator/graph-rag
rm -rf e2e/build/generated-tests/* ; mkdir -p e2e/build/generated-tests
cp -R "$W/generated/io" e2e/build/generated-tests/
cp "$W/generated/junit-platform.properties" e2e/src/test/resources/
APP_BASE_URI=http://localhost:8081 \
JDBC_URL=jdbc:mysql://localhost:3306/authuser JDBC_USER=root JDBC_PASS=rootpw \
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
AUTH_ADAPTER=real AUTH_LOGIN_PATH=/api/v1/auth/guest AUTH_TOKEN_FIELD=accessToken \
AUTH_USER=guest AUTH_PASS=guest \
./gradlew :e2e:test --rerun
# 기대: BUILD SUCCESSFUL, tests=7 skipped=0 failures=0 errors=0
```

### 실행 좌표 (platform docker-compose.yml + application-docker.yml)

| 항목 | 값 |
|---|---|
| auth-user host port | `8081` |
| MySQL host port / db / user / pw | `3306` / `authuser` / `root` / `rootpw` |
| Redis host port | `6379` |
| SUT jar | `target/auth-user-service-0.1.0.jar` |
| 게스트 로그인 path / 토큰 필드 | `/api/v1/auth/guest` / `accessToken` |

## 정리 (cleanup)

```bash
# e2e 하니스에 복사한 생성 테스트 제거
cd /Users/changjoonbaek/github_graph-rag-test-generator/graph-rag
rm -rf e2e/build/generated-tests/*
rm -f e2e/src/test/resources/junit-platform.properties

# docker-compose 스택 + 볼륨 제거
docker compose -p grauthuser -f /Users/changjoonbaek/github_tainted-spring/tainted-spring-platform/docker-compose.yml down -v
```

## 디렉터리 구성

```
graphrag-blackbox/
├── README.md                      # 이 파일
├── generated-tests/               # 도구 2 생성 RestAssured 소스 (그대로 e2e 하니스에 복사해 실행)
│   ├── io/graphrag/generated/authuser/*.java
│   └── junit-platform.properties
├── generation-result.json         # 도구 2 결과(파일 목록 + 병렬 안전성)
├── graph/
│   ├── graph.json                 # 도구 1 캡처 사실(엔드포인트·path·SQL·Redis·seed)
│   ├── exploration-report.json    # 분기 커버리지 보고
│   └── global.json
└── requests/                      # 도구 2 입력 요청 스펙(endpointId/authMode 등)
    └── req-*.json
```
