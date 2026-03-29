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
 * ka10099 종목정보리스트요청 — 기준정보 응답 구조 확인
 *
 * 확인 목표:
 *   - 응답 래퍼 필드명 (list? list_stk_info? 루트 직접?)
 *   - 종목 코드/이름 필드명 (code? stk_cd? / name? stk_nm?)
 *   - 코스닥 mrkt_tp 파라미터값 (10? 1?)
 *
 * 결과: docs/test/ 폴더에 JSON 파일로 저장됨
 * 주의: 응답이 수백~수천 건이므로 처음 5개만 별도 파일에 저장
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Ka10099StockMasterTest {

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
            "/api/dostk/stkinfo",
            "ka10099",
            Map.of("mrkt_tp", "0")
        );
        writeResponse("ka10099_kospi_full.json", response);
        writeFirst5("ka10099_kospi_first5.json", response);
    }

    @Test
    void 코스닥_응답확인() throws Exception {
        JsonNode response = kiwoomApiClient.post(
            "/api/dostk/stkinfo",
            "ka10099",
            Map.of("mrkt_tp", "10")
        );
        writeResponse("ka10099_kosdaq_full.json", response);
        writeFirst5("ka10099_kosdaq_first5.json", response);
    }

    private void writeFirst5(String filename, JsonNode response) throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        for (String field : new String[]{"list", "stk_list", "output", "list_stk_info"}) {
            JsonNode arr = response.path(field);
            if (arr.isArray() && arr.size() > 0) {
                var node = mapper.createObjectNode();
                node.put("wrapper_field", field);
                node.put("total_count", arr.size());
                var first5 = mapper.createArrayNode();
                for (int i = 0; i < Math.min(5, arr.size()); i++) first5.add(arr.get(i));
                node.set("first_5", first5);
                Files.writeString(OUTPUT_DIR.resolve(filename), mapper.writeValueAsString(node));
                System.out.println("저장 완료 (first5): " + filename + " [래퍼=" + field + ", 총 " + arr.size() + "건]");
                return;
            }
        }
        System.out.println("[경고] 알려진 래퍼 필드 없음 — full 파일 참조");
    }

    private void writeResponse(String filename, JsonNode response) throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        Path out = OUTPUT_DIR.resolve(filename);
        Files.writeString(out, mapper.writeValueAsString(response));
        System.out.println("저장 완료: " + out.toAbsolutePath());
    }
}
