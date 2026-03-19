package dev.eolmae.psms.collector;

import com.fasterxml.jackson.databind.JsonNode;
import dev.eolmae.psms.domain.common.InvestorType;
import dev.eolmae.psms.domain.common.MarketType;
import dev.eolmae.psms.domain.dashboard.InvestorTradingSummary;
import dev.eolmae.psms.domain.dashboard.InvestorTradingSummaryRepository;
import dev.eolmae.psms.domain.dashboard.InvestorTradingSummarySnapshot;
import dev.eolmae.psms.domain.dashboard.InvestorTradingSummarySnapshotRepository;
import dev.eolmae.psms.external.kiwoom.KiwoomApiClient;
import dev.eolmae.psms.external.kiwoom.KiwoomResponseParser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvestorTradingSummaryCollector {

	// TODO: 키움 Open API 포털에서 확인 후 수정 필요
	// 투자자별 매매동향 API - KOSPI와 KOSDAQ 각각 호출하여 개인/외국인/기관 파싱
	private static final String API_PATH = "/api/dostk/investratio";
	private static final String TR_ID_KOSPI = "FHKST01010900";   // TODO: 확인 필요
	private static final String TR_ID_KOSDAQ = "FHKST01010900";  // TODO: 확인 필요 (동일 tr_id에 파라미터로 구분하거나 별도 tr_id일 수 있음)

	private final KiwoomApiClient kiwoomApiClient;
	private final InvestorTradingSummaryRepository investorTradingSummaryRepository;
	private final InvestorTradingSummarySnapshotRepository investorTradingSummarySnapshotRepository;

	@Transactional
	public void collect(LocalDateTime snapshotTime) {
		for (MarketType marketType : MarketType.values()) {
			try {
				collectForMarket(marketType, snapshotTime);
			} catch (Exception e) {
				log.error("투자자별매매종합 수집 실패: market={}", marketType, e);
			}
		}
	}

	private void collectForMarket(MarketType marketType, LocalDateTime snapshotTime) {
		// TODO: 시장구분 파라미터 값 확인 필요 (KOSPI: "0001" 또는 "J", KOSDAQ: "1001" 또는 "Q" 등)
		String marketCode = marketType == MarketType.KOSPI ? "0001" : "1001";
		String trId = marketType == MarketType.KOSPI ? TR_ID_KOSPI : TR_ID_KOSDAQ;

		JsonNode response = kiwoomApiClient.post(
			API_PATH,
			trId,
			Map.of("market_code", marketCode)  // TODO: 요청 파라미터 필드명 확인 필요
		);

		LocalDateTime now = LocalDateTime.now();

		// TODO: 응답 구조 확인 필요.
		// 한 번의 API 호출로 개인/외국인/기관 전체가 오는 구조라면 output 배열에서 파싱,
		// 투자자별로 각각 호출해야 하는 구조라면 아래 로직 변경 필요
		JsonNode outputList = response.path("output");

		for (JsonNode item : outputList) {
			// TODO: 투자자 구분 코드 값 및 필드명 확인 필요
			String investorCode = KiwoomResponseParser.parseString(item, "invt_obj_cls_code");
			InvestorType investorType = mapToInvestorType(investorCode);
			if (investorType == null) {
				continue;
			}

			BigDecimal buyAmount = KiwoomResponseParser.parseBigDecimal(item, "buy_tr_pbmn");
			BigDecimal sellAmount = KiwoomResponseParser.parseBigDecimal(item, "seln_tr_pbmn");
			BigDecimal netBuyAmount = KiwoomResponseParser.parseBigDecimal(item, "ntby_tr_pbmn");

			InvestorTradingSummary summary = investorTradingSummaryRepository
				.findByMarketTypeAndInvestorType(marketType, investorType)
				.map(existing -> {
					existing.update(snapshotTime, now, buyAmount, sellAmount, netBuyAmount);
					return existing;
				})
				.orElseGet(() -> InvestorTradingSummary.create(
					marketType, investorType, snapshotTime, now, buyAmount, sellAmount, netBuyAmount));

			investorTradingSummaryRepository.save(summary);

			if (investorTradingSummarySnapshotRepository
				.findByMarketTypeAndInvestorTypeAndSnapshotTime(marketType, investorType, snapshotTime)
				.isEmpty()) {
				investorTradingSummarySnapshotRepository.save(InvestorTradingSummarySnapshot.from(summary));
			}
		}

		log.debug("투자자별매매종합 수집 완료: market={}", marketType);
	}

	// TODO: 키움 API의 실제 투자자 구분 코드 값 확인 후 수정 필요
	private InvestorType mapToInvestorType(String investorCode) {
		return switch (investorCode) {
			case "01" -> InvestorType.PERSONAL;       // TODO: 실제 코드 확인
			case "04" -> InvestorType.FOREIGNER;      // TODO: 실제 코드 확인
			case "10" -> InvestorType.INSTITUTION;    // TODO: 실제 코드 확인 (기관계 합산 코드)
			default -> null;
		};
	}
}
