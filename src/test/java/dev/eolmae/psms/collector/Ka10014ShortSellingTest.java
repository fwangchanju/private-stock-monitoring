package dev.eolmae.psms.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.eolmae.psms.external.kiwoom.KiwoomApiClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * ka10014 공매도추이요청 — 종목별 공매도추이 응답 구조 확인
 *
 * 확인 목표:
 *   - 응답 배열 필드명 (shrts_trnsn?)
 *   - shrts_qty / trde_wght / shrts_trde_prica 필드명 및 값 형태
 *   - 첫 번째 항목이 당일(가장 최신) 데이터인지 확인
 *
 * 결과: docs/test/ 폴더에 JSON 파일로 저장됨
 */
@SpringBootTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class Ka10014ShortSellingTest {

    @Autowired
    private KiwoomApiClient kiwoomApiClient;

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Path OUTPUT_DIR = Paths.get("docs/test");

    @Test
    void 삼성전자_당일_응답확인() throws Exception {
        String today = LocalDate.now().format(DATE_FMT);
        JsonNode response = kiwoomApiClient.post(
            "/api/dostk/shsa",
            "ka10014",
            Map.of(
                "stk_cd", "005930",
                "tm_tp", "2",       // 2=일별
                "strt_dt", today,
                "end_dt", today
            )
        );
        writeResponse("ka10014_005930_today.json", response);
    }

    @Test
    void 삼성전자_기간_응답확인() throws Exception {
        String today = LocalDate.now().format(DATE_FMT);
        String weekAgo = LocalDate.now().minusDays(7).format(DATE_FMT);
        JsonNode response = kiwoomApiClient.post(
            "/api/dostk/shsa",
            "ka10014",
            Map.of(
                "stk_cd", "005930",
                "tm_tp", "2",
                "strt_dt", weekAgo,
                "end_dt", today
            )
        );
        writeResponse("ka10014_005930_week.json", response);
    }

    private void writeResponse(String filename, JsonNode response) throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        Path out = OUTPUT_DIR.resolve(filename);
        Files.writeString(out, mapper.writeValueAsString(response));
        System.out.println("저장 완료: " + out.toAbsolutePath());
    }
}
