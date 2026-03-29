package dev.eolmae.psms.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.eolmae.psms.external.kiwoom.KiwoomApiClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * ka90003 프로그램순매수상위50요청 — 랭킹 응답 구조 확인
 *
 * 확인 목표:
 *   - 응답 배열 필드명 (prm_netprps_upper_50?)
 *   - 종목코드/종목명 필드명 (stk_cd? / stk_nm?)
 *   - 순매수금액 필드명 (netprps_amt? / prm_netprps_amt?)
 *   - 순매수수량 필드명 (netprps_qty? / prm_netprps_qty?)
 *   - mrkt_cd 파라미터: 코스피=P00101, 코스닥=P10102 맞는지
 *   - trde_tp: 1=순매수, 2=순매도 / amt_qty_tp: 1=금액, 2=수량 맞는지
 *
 * 결과: docs/test/ 폴더에 JSON 파일로 저장됨
 */
@SpringBootTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class Ka90003ProgramTradingRankingTest {

    @Autowired
    private KiwoomApiClient kiwoomApiClient;

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final Path OUTPUT_DIR = Paths.get("docs/test");

    @Test
    void 코스피_순매수_금액_응답확인() throws Exception {
        JsonNode response = kiwoomApiClient.post(
            "/api/dostk/stkinfo",
            "ka90003",
            Map.of(
                "mrkt_cd", "P00101",
                "trde_tp", "1",       // 1=순매수
                "amt_qty_tp", "1"     // 1=금액
            )
        );
        writeResponse("ka90003_kospi_buy_amt.json", response);
    }

    @Test
    void 코스피_순매도_금액_응답확인() throws Exception {
        JsonNode response = kiwoomApiClient.post(
            "/api/dostk/stkinfo",
            "ka90003",
            Map.of(
                "mrkt_cd", "P00101",
                "trde_tp", "2",       // 2=순매도
                "amt_qty_tp", "1"
            )
        );
        writeResponse("ka90003_kospi_sell_amt.json", response);
    }

    @Test
    void 코스피_순매수_수량_응답확인() throws Exception {
        JsonNode response = kiwoomApiClient.post(
            "/api/dostk/stkinfo",
            "ka90003",
            Map.of(
                "mrkt_cd", "P00101",
                "trde_tp", "1",
                "amt_qty_tp", "2"     // 2=수량
            )
        );
        writeResponse("ka90003_kospi_buy_qty.json", response);
    }

    @Test
    void 코스닥_순매수_금액_응답확인() throws Exception {
        JsonNode response = kiwoomApiClient.post(
            "/api/dostk/stkinfo",
            "ka90003",
            Map.of(
                "mrkt_cd", "P10102",
                "trde_tp", "1",
                "amt_qty_tp", "1"
            )
        );
        writeResponse("ka90003_kosdaq_buy_amt.json", response);
    }

    private void writeResponse(String filename, JsonNode response) throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        Path out = OUTPUT_DIR.resolve(filename);
        Files.writeString(out, mapper.writeValueAsString(response));
        System.out.println("저장 완료: " + out.toAbsolutePath());
    }
}
