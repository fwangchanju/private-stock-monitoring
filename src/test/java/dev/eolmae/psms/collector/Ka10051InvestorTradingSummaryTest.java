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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * ka10051 업종별투자자순매수요청 — 투자자별매매종합 응답 구조 확인
 *
 * 확인 목표:
 *   - 응답 배열 필드명 (inds_netprps?)
 *   - 배열 내 inds_cd 값 — 종합지수 행이 "001"(코스피) / "101"(코스닥)인지
 *   - ind_netprps / frgnr_netprps / orgn_netprps 필드명 및 값 형태
 *
 * 결과: docs/test/ 폴더에 JSON 파일로 저장됨
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Ka10051InvestorTradingSummaryTest {

    private KiwoomApiClient kiwoomApiClient;
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Path OUTPUT_DIR = Paths.get("docs/test");

    @BeforeAll
    void setUp() {
        var props = new KiwoomProperties(
            System.getenv("KIWOOM_APP_KEY"),
            System.getenv("KIWOOM_SECRET"),
            100L
        );
        kiwoomApiClient = new KiwoomApiClient(props, new KiwoomTokenManager(props), new ObjectMapper());
    }

    @Test
    void 코스피_응답확인() throws Exception {
        String today = LocalDate.now().format(DATE_FMT);
        JsonNode response = kiwoomApiClient.post(
            "/api/dostk/sect",
            "ka10051",
            Map.of("mrkt_tp", "0", "amt_qty_tp", "0", "base_dt", today, "stex_tp", "1")
        );
        writeResponse("ka10051_kospi.json", response);
    }

    @Test
    void 코스닥_응답확인() throws Exception {
        String today = LocalDate.now().format(DATE_FMT);
        JsonNode response = kiwoomApiClient.post(
            "/api/dostk/sect",
            "ka10051",
            Map.of("mrkt_tp", "1", "amt_qty_tp", "0", "base_dt", today, "stex_tp", "1")
        );
        writeResponse("ka10051_kosdaq.json", response);
    }

    private void writeResponse(String filename, JsonNode response) throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        Path out = OUTPUT_DIR.resolve(filename);
        Files.writeString(out, mapper.writeValueAsString(response));
        System.out.println("저장 완료: " + out.toAbsolutePath());
    }
}
