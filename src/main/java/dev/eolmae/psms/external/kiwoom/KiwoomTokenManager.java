package dev.eolmae.psms.external.kiwoom;

import dev.eolmae.psms.external.kiwoom.dto.TokenResponse;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class KiwoomTokenManager {

	private static final String TOKEN_URL = "https://api.kiwoom.com/oauth2/token";

	private final KiwoomProperties properties;
	private final RestClient restClient = RestClient.create();

	private volatile String cachedToken;
	private volatile Instant tokenExpiry = Instant.MIN;

	public synchronized String getToken() {
		if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
			return cachedToken;
		}
		return refreshToken();
	}

	private String refreshToken() {
		log.info("Kiwoom 토큰 발급 요청");
		var body = Map.of(
			"grant_type", "client_credentials",
			"appkey", properties.appKey(),
			"secretkey", properties.secret()
		);

		var response = restClient.post()
			.uri(TOKEN_URL)
			.contentType(MediaType.APPLICATION_JSON)
			.body(body)
			.retrieve()
			.body(TokenResponse.class);

		cachedToken = response.accessToken();
		tokenExpiry = Instant.now().plusSeconds(response.expiresIn() - 300);
		log.info("Kiwoom 토큰 발급 완료. 만료 예정: {}", tokenExpiry);
		return cachedToken;
	}
}
