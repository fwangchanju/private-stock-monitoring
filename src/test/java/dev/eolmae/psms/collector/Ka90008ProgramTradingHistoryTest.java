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
 * ka90008 종목시간별프로그램매매추이요청 — 시간별 추이 응답 구조 확인
 *
 * 확인 목표:
 *   - 응답 배열 필드명 (stk_tm_prm_trde_trnsn?)
 *   - 시각 필드명 (tm?)
 *   - 프로그램 매수/매도/순매수 필드명
 *   - 배열 정렬 순서: 처음 3개 + 마지막 3개 비교로 오름/내림차순 판별
 *
 * 결과: docs/test/ 폴더에 JSON 파일로 저장됨
 * 주의: 장중(09:00~15:30)에 실행해야 시간별 데이터 존재
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Ka90008ProgramTradingHistoryTest {

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
    void 삼성전자_시간별_응답확인() throws Exception {
        JsonNode response = kiwoomApiClient.post(
            "/api/dostk/mrkcond",
            "ka90008",
            Map.of("stk_cd", "005930")
        );
        writeResponse("ka90008_005930_full.json", response);

        for (String field : new String[]{"stk_tm_prm_trde_trnsn", "output", "list"}) {
            JsonNode arr = response.path(field);
            if (arr.isArray() && arr.size() > 0) {
                int size = arr.size();
                var node = mapper.createObjectNode();
                node.put("wrapper_field", field);
                node.put("total_count", size);
                var first3 = mapper.createArrayNode();
                for (int i = 0; i < Math.min(3, size); i++) first3.add(arr.get(i));
                var last3 = mapper.createArrayNode();
                for (int i = Math.max(0, size - 3); i < size; i++) last3.add(arr.get(i));
                node.set("first_3", first3);
                node.set("last_3", last3);
                node.put("hint", "tm 필드 비교: first[0].tm < last[0].tm 이면 오름차순(최초→최신)");
                Files.createDirectories(OUTPUT_DIR);
                Files.writeString(OUTPUT_DIR.resolve("ka90008_005930_order_check.json"), mapper.writeValueAsString(node));
                System.out.println("저장 완료 (순서확인): ka90008_005930_order_check.json [배열=" + field + ", 총 " + size + "건]");
                break;
            }
        }
    }

    private void writeResponse(String filename, JsonNode response) throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        Path out = OUTPUT_DIR.resolve(filename);
        Files.writeString(out, mapper.writeValueAsString(response));
        System.out.println("저장 완료: " + out.toAbsolutePath());
    }
}
