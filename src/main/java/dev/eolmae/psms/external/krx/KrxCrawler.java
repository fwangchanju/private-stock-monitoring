package dev.eolmae.psms.external.krx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.eolmae.psms.exception.BusinessException;
import dev.eolmae.psms.exception.EscalateException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * KRX 데이터 포털 크롤러.
 *
 * 인증 방식: ~/env/krx-login-cookie 파일에서 세션 쿠키를 읽어 요청에 포함.
 * 파일 형식: 각 줄에 Name=Value 형태로 작성.
 *   예) __smVisitorID=xxx
 *       JSESSIONID=xxx
 *       lang=ko_KR
 *       mdc.client_session=true
 *
 * 세션 만료 처리:
 *   - LOGOUT 응답 수신 시 sessionExpired 플래그 세팅 → 이후 요청 스킵
 *   - 쿠키 파일이 갱신되면(수정 시간 변경) 플래그 자동 리셋
 */
@Slf4j
@Component
public class KrxCrawler {

    private static final String KRX_DATA_URL = "https://data.krx.co.kr/comm/bldAttendant/getJsonData.cmd";
    private static final String KRX_REFERER = "https://data.krx.co.kr/contents/MDC/MDI/outerLoader/index.cmd";

    @Value("${krx.cookie-file}")
    private String cookieFilePath;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile boolean sessionExpired = false;
    private volatile long lastCookieFileMtime = 0;

    public List<JsonNode> fetch(Map<String, String> params) {
        String cookie = resolveSessionCookie();
        if (cookie == null) {
            throw new EscalateException(
                "KRX 세션 쿠키 없음 — ~/env/krx-login-cookie 파일 확인 필요"
            );
        }

        MultiValueMap<String, String> formData = toMultiValueMap(params);

        String responseBody = restClient.post()
            .uri(KRX_DATA_URL)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0")
            .header("Referer", KRX_REFERER)
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Cookie", cookie)
            .body(formData)
            .retrieve()
            .body(String.class);

        if ("LOGOUT".equalsIgnoreCase(responseBody != null ? responseBody.trim() : "")) {
            if (!sessionExpired) {
                sessionExpired = true;
                throw new EscalateException(
                    "KRX 세션 만료(LOGOUT) — ~/env/krx-login-cookie 쿠키 갱신 필요: bld=" + params.get("bld")
                );
            }
            throw new BusinessException("KRX 세션 만료 상태 지속 중 — 알림 생략");
        }

        return parseOutBlock(responseBody, params.get("bld"));
    }

    /**
     * 쿠키 파일을 읽어 Cookie 헤더 문자열로 반환.
     * 파일 수정 시간이 변경된 경우 sessionExpired 플래그를 리셋한다.
     */
    private String resolveSessionCookie() {
        Path cookieFile = Paths.get(cookieFilePath);

        if (!Files.exists(cookieFile)) {
            log.warn("KRX 쿠키 파일 없음: {}", cookieFile);
            return null;
        }

        try {
            long mtime = Files.getLastModifiedTime(cookieFile).toMillis();
            if (mtime != lastCookieFileMtime) {
                lastCookieFileMtime = mtime;
                if (sessionExpired) {
                    log.info("KRX 쿠키 파일 갱신 감지 — 세션 만료 플래그 리셋");
                    sessionExpired = false;
                }
            }
        } catch (IOException e) {
            log.warn("KRX 쿠키 파일 수정 시간 확인 실패: {}", e.getMessage());
        }

        if (sessionExpired) {
            log.warn("KRX 세션 만료 상태 — 쿠키 갱신 전까지 수집 스킵: {}", cookieFile);
            return null;
        }

        try {
            return Files.readAllLines(cookieFile).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .collect(Collectors.joining("; "));
        } catch (IOException e) {
            log.warn("KRX 쿠키 파일 읽기 실패: {}", e.getMessage());
            return null;
        }
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
