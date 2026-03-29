package dev.eolmae.psms.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.eolmae.psms.external.kiwoom.KiwoomApiClient;
import dev.eolmae.psms.external.kiwoom.KiwoomProperties;
import dev.eolmae.psms.external.kiwoom.KiwoomTokenManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * ka20001 업종현재가요청 — 시장종합 응답 구조 확인
 *
 * 확인 목표:
 *   - 응답 최상위 래퍼 필드명 (output? 또는 루트 직접?)
 *   - cur_prc / flu_rt / pred_pre / trde_prica 필드명
 *   - rising / stdns / fall 필드명
 *   - mrkt_stat_cls_code 필드명 및 값 형태
 *
 * 결과: docs/test/ 폴더에 JSON 파일로 저장됨
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Ka20001MarketOverviewTest {

    private KiwoomApiClient kiwoomApiClient;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final Path OUTPUT_DIR = Paths.get("docs/test");

    @BeforeAll
    void setUp() {
        var props = new KiwoomProperties(
            System.getenv("KIWOOM_APP_KEY"),
            System.getenv("KIWOOM_SECRET")
        );
        kiwoomApiClient = new KiwoomApiClient(props, new KiwoomTokenManager(props));
    }

    @Test
    void 코스피_응답확인() throws Exception {
        JsonNode response = kiwoomApiClient.post(
            "/api/dostk/sect",
            "ka20001",
            Map.of("mrkt_tp", "0", "inds_cd", "001")
        );
        writeResponse("ka20001_kospi.json", response);
    }

    @Test
    void 코스닥_응답확인() throws Exception {
        JsonNode response = kiwoomApiClient.post(
            "/api/dostk/sect",
            "ka20001",
            Map.of("mrkt_tp", "1", "inds_cd", "101")
        );
        writeResponse("ka20001_kosdaq.json", response);
    }

    private void writeResponse(String filename, JsonNode response) throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        Path out = OUTPUT_DIR.resolve(filename);
        Files.writeString(out, mapper.writeValueAsString(response));
        System.out.println("저장 완료: " + out.toAbsolutePath());
    }
}
