package dev.eolmae.psms.external.kiwoom;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class KiwoomApiClient {

	private static final String BASE_URL = "https://openapi.kiwoom.com:9443";

	private final KiwoomProperties properties;
	private final KiwoomTokenManager tokenManager;
	private final RestClient restClient = RestClient.create();

	public JsonNode post(String path, String trId, Object requestBody) {
		log.debug("Kiwoom API 호출: trId={}, path={}", trId, path);

		JsonNode response = restClient.post()
			.uri(BASE_URL + path)
			.contentType(MediaType.APPLICATION_JSON)
			.header("authorization", "Bearer " + tokenManager.getToken())
			.header("appkey", properties.appKey())
			.header("secretkey", properties.secret())
			.header("tr_id", trId)
			.body(requestBody)
			.retrieve()
			.body(JsonNode.class);

		String rtCd = response.path("rt_cd").asText();
		if (!"0".equals(rtCd)) {
			log.warn("Kiwoom API 오류 응답: trId={}, rt_cd={}, msg={}", trId, rtCd, response.path("msg1").asText());
			throw new KiwoomApiException("Kiwoom API 오류: " + response.path("msg1").asText());
		}

		return response;
	}
}
