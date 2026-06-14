package com.tainted.authuser;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4").withDatabaseName("authuser");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
        r.add("spring.data.redis.host", redis::getHost);
        r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void guestLoginThenMe() {
        String token = given().when().post("/api/v1/auth/guest")
                .then().statusCode(200)
                .body("displayName", equalTo("익명"))
                .body("accessToken", not(emptyOrNullString()))
                .extract().path("accessToken");

        given().header("Authorization", "Bearer " + token)
                .when().get("/api/v1/me")
                .then().statusCode(200)
                .body("displayName", equalTo("익명"))
                .body("socialProvider", equalTo("guest"))
                .body("userId", not(emptyOrNullString()));
    }

    @Test
    void socialLoginThenVerifyAndGetUser() {
        String token = given().contentType(ContentType.JSON)
                .body("{\"provider\":\"kakao\",\"providerToken\":\"valid-kakao-u1\"}")
                .when().post("/api/v1/auth/login")
                .then().statusCode(200)
                .extract().path("accessToken");

        String userId = given().contentType(ContentType.JSON)
                .body("{\"token\":\"" + token + "\"}")
                .when().post("/internal/auth/verify")
                .then().statusCode(200)
                .body("active", equalTo(true))
                .body("socialProvider", equalTo("kakao"))
                .extract().path("userId");

        given().when().get("/internal/users/" + userId)
                .then().statusCode(200)
                .body("socialProvider", equalTo("kakao"))
                .body("id", equalTo(userId));
    }

    @Test
    void invalidTokenReturnsProblemJson() {
        given().header("Authorization", "Bearer not-a-real-token")
                .when().get("/api/v1/me")
                .then().statusCode(401)
                .contentType("application/problem+json")
                .body("title", equalTo("Invalid token"));
    }

    @Test
    void verifyUnknownTokenReturnsInactive() {
        given().contentType(ContentType.JSON)
                .body("{\"token\":\"ghost\"}")
                .when().post("/internal/auth/verify")
                .then().statusCode(200)
                .body("active", equalTo(false));
    }

    @Test
    void unsupportedProviderRejectedWithProblemJson() {
        given().contentType(ContentType.JSON)
                .body("{\"provider\":\"facebook\",\"providerToken\":\"valid-facebook-u1\"}")
                .when().post("/api/v1/auth/login")
                .then().statusCode(401)
                .contentType("application/problem+json");
    }

    @Test
    void googleMockLoginSucceeds() {
        // google 은 이제 지원 provider(기본 mock 모드). valid-google-<seed> 로그인 성공.
        given().contentType(ContentType.JSON)
                .body("{\"provider\":\"google\",\"providerToken\":\"valid-google-u1\"}")
                .when().post("/api/v1/auth/login")
                .then().statusCode(200)
                .body("accessToken", not(emptyOrNullString()));
    }
}
