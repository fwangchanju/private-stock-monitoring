package dev.eolmae.psms.external.kiwoom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class KiwoomApiClient {

	private static final String BASE_URL = "https://api.kiwoom.com";

	private final KiwoomProperties properties;
	private final KiwoomTokenManager tokenManager;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final RestClient restClient = RestClient.create();

	/**
	 * 타입 안전 API 호출. request DTO가 직렬화되어 요청 바디로 전송된다.
	 * 응답 전체(rt_cd, msg1 포함 root object)를 dataClass 타입으로 역직렬화해 반환한다.
	 * 호출마다 callIntervalMs 딜레이를 적용한다.
	 */
	public <T> T post(KiwoomRequest request, Class<T> dataClass) {
		log.debug("Kiwoom API 호출: apiId={}, path={}", request.apiId(), request.path());

		sleep(properties.callIntervalMs());

		JsonNode response = callApi(request.path(), request.apiId(), request);
		return objectMapper.convertValue(response, dataClass);
	}

	/**
	 * @deprecated {@link #post(KiwoomRequest, Class)} 사용 권장.
	 */
	@Deprecated
	public JsonNode post(String path, String trId, Object requestBody) {
		log.debug("Kiwoom API 호출: trId={}, path={}", trId, path);
		return callApi(path, trId, requestBody);
	}

	private JsonNode callApi(String path, String apiId, Object requestBody) {
		JsonNode response = restClient.post()
			.uri(BASE_URL + path)
			.contentType(MediaType.APPLICATION_JSON)
			.header("authorization", "Bearer " + tokenManager.getToken())
			.header("appkey", properties.appKey())
			.header("secretkey", properties.secret())
			.header("api-id", apiId)
			.body(requestBody)
			.retrieve()
			.body(JsonNode.class);

		String rtCd = response.path("rt_cd").asText();
		if (!"0".equals(rtCd)) {
			log.warn("Kiwoom API 오류 응답: apiId={}, rt_cd={}, msg={}", apiId, rtCd, response.path("msg1").asText());
			throw new KiwoomApiException("Kiwoom API 오류: " + response.path("msg1").asText());
		}

		return response;
	}

	private void sleep(long millis) {
		if (millis <= 0) {
			return;
		}
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
