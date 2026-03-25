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
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvestorTradingSummaryCollector {

	// ka10051: 업종별투자자순매수요청 (업종 카테고리)
	// 업종코드 001(코스피종합)/101(코스닥) 기준으로 시장 전체 투자자별 순매수 조회
	private static final String API_PATH = "/api/dostk/sect";
	private static final String TR_ID = "ka10051";
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

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
		// mrkt_tp: 0=코스피, 1=코스닥
		String mrktTp = marketType == MarketType.KOSPI ? "0" : "1";

		String baseDt = snapshotTime.format(DATE_FMT);
		JsonNode response = kiwoomApiClient.post(
			API_PATH,
			TR_ID,
			Map.of(
				"mrkt_tp", mrktTp,
				"amt_qty_tp", "0",  // 0=금액
				"base_dt", baseDt,
				"stex_tp", "1"      // 1=KRX
			)
		);

		// 응답 배열: inds_netprps (업종별 투자자 순매수 목록)
		// 첫 번째 항목이 종합지수(코스피/코스닥 전체) 데이터
		JsonNode indsNetprpsList = response.path("inds_netprps");
		JsonNode compositeItem = findCompositeItem(indsNetprpsList, marketType);
		if (compositeItem == null || compositeItem.isMissingNode()) {
			log.warn("투자자별매매종합 종합지수 항목 없음: market={}", marketType);
			return;
		}

		LocalDateTime now = LocalDateTime.now();

		// 응답에서 순매수 금액만 제공 (매수/매도 금액은 미제공)
		// ind_netprps=개인, frgnr_netprps=외국인, orgn_netprps=기관계
		saveInvestorSummary(marketType, InvestorType.PERSONAL,
			KiwoomResponseParser.parseBigDecimal(compositeItem, "ind_netprps"),
			snapshotTime, now);
		saveInvestorSummary(marketType, InvestorType.FOREIGNER,
			KiwoomResponseParser.parseBigDecimal(compositeItem, "frgnr_netprps"),
			snapshotTime, now);
		saveInvestorSummary(marketType, InvestorType.INSTITUTION,
			KiwoomResponseParser.parseBigDecimal(compositeItem, "orgn_netprps"),
			snapshotTime, now);

		log.debug("투자자별매매종합 수집 완료: market={}", marketType);
	}

	private JsonNode findCompositeItem(JsonNode list, MarketType marketType) {
		// 종합지수 inds_cd: KOSPI=001, KOSDAQ=101
		String compositeCode = marketType == MarketType.KOSPI ? "001" : "101";
		for (JsonNode item : list) {
			if (compositeCode.equals(item.path("inds_cd").asText())) {
				return item;
			}
		}
		// 찾지 못하면 첫 번째 항목 사용
		return list.size() > 0 ? list.get(0) : null;
	}

	private void saveInvestorSummary(MarketType marketType, InvestorType investorType,
		BigDecimal netBuyAmount, LocalDateTime snapshotTime, LocalDateTime now) {

		// ka10051은 순매수만 제공하므로 매수/매도는 ZERO로 저장
		BigDecimal zero = BigDecimal.ZERO;

		InvestorTradingSummary summary = investorTradingSummaryRepository
			.findByMarketTypeAndInvestorType(marketType, investorType)
			.map(existing -> {
				existing.update(snapshotTime, now, zero, zero, netBuyAmount);
				return existing;
			})
			.orElseGet(() -> InvestorTradingSummary.create(
				marketType, investorType, snapshotTime, now, zero, zero, netBuyAmount));

		investorTradingSummaryRepository.save(summary);

		if (investorTradingSummarySnapshotRepository
			.findByMarketTypeAndInvestorTypeAndSnapshotTime(marketType, investorType, snapshotTime)
			.isEmpty()) {
			investorTradingSummarySnapshotRepository.save(InvestorTradingSummarySnapshot.from(summary));
		}
	}
}
