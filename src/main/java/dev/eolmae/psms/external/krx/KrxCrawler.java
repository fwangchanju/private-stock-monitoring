package dev.eolmae.psms.external.krx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.eolmae.psms.exception.EscalateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * KRX 데이터 포털 크롤러.
 * POST https://data.krx.co.kr/comm/bldAttendant/getJsonData.cmd 에 form 파라미터를 전송하고
 * 응답의 OutBlock_1 배열을 JsonNode 리스트로 반환한다.
 *
 * OutBlock_1이 없거나 null이면 KRX 인터페이스 구조 변경으로 판단하여 EscalateException을 발생시킨다.
 */
@Slf4j
@Component
public class KrxCrawler {

    private static final String KRX_DATA_URL = "https://data.krx.co.kr/comm/bldAttendant/getJsonData.cmd";

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @param params bld 포함 요청 파라미터 전체
     * @return OutBlock_1 배열의 각 항목을 JsonNode 리스트로 반환
     * @throws EscalateException OutBlock_1이 없거나 null인 경우 (KRX 응답 구조 변경 의심)
     */
    public List<JsonNode> fetch(Map<String, String> params) {
        MultiValueMap<String, String> formData = toMultiValueMap(params);

        String responseBody = restClient.post()
            .uri(KRX_DATA_URL)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .header("User-Agent", "Mozilla/5.0")
            .body(formData)
            .retrieve()
            .body(String.class);

        return parseOutBlock(responseBody, params.get("bld"));
    }

    private List<JsonNode> parseOutBlock(String responseBody, String bld) {
        JsonNode root;
        try {
            root = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            throw new EscalateException("KRX 응답 JSON 파싱 실패: bld=" + bld, e);
        }

        JsonNode outBlock = root.get("OutBlock_1");
        if (outBlock == null || outBlock.isNull()) {
            throw new EscalateException(
                "KRX 응답에 OutBlock_1이 없음 — 인터페이스 구조 변경 의심: bld=" + bld
            );
        }

        List<JsonNode> result = new ArrayList<>();
        outBlock.forEach(result::add);
        log.debug("KRX 응답 수신: bld={}, 항목수={}", bld, result.size());
        return result;
    }

    private MultiValueMap<String, String> toMultiValueMap(Map<String, String> params) {
        MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
        params.forEach(multiValueMap::add);
        return multiValueMap;
    }
}
