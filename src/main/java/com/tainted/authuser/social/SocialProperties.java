package com.tainted.authuser.social;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 소셜 로그인 설정. provider별 mode(mock|real)와 자격증명을 담는다.
 * 자격증명은 환경변수로 주입되며, 미설정(mode=mock) 시 결정론적 mock 으로 동작한다.
 */
@ConfigurationProperties("social")
public class SocialProperties {

    private Provider google = new Provider();
    private Provider kakao = new Provider();
    private Provider naver = new Provider();

    public Provider getGoogle() { return google; }
    public void setGoogle(Provider google) { this.google = google; }
    public Provider getKakao() { return kakao; }
    public void setKakao(Provider kakao) { this.kakao = kakao; }
    public Provider getNaver() { return naver; }
    public void setNaver(Provider naver) { this.naver = naver; }

    /** provider 별 mode 조회. 알 수 없으면 "mock". */
    public String modeOf(String provider) {
        return switch (provider == null ? "" : provider) {
            case "google" -> google.getMode();
            case "kakao" -> kakao.getMode();
            case "naver" -> naver.getMode();
            default -> "mock";
        };
    }

    public static class Provider {
        private String mode = "mock";
        private String clientId = "";
        private String clientSecret = "";
        private String redirectUri = "";

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    }
}
