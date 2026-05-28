package dev.eolmae.marketmonitor.collector;

import dev.eolmae.marketmonitor.common.enums.AmtQtyType;
import dev.eolmae.marketmonitor.common.enums.InvestorType;
import dev.eolmae.marketmonitor.common.enums.MarketType;
import dev.eolmae.marketmonitor.common.enums.StexType;
import dev.eolmae.marketmonitor.common.util.NumberParser;
import dev.eolmae.marketmonitor.domain.dashboard.InvestorTradingSummarySnapshot;
import dev.eolmae.marketmonitor.domain.dashboard.repository.InvestorTradingSummarySnapshotRepository;
import dev.eolmae.marketmonitor.external.kiwoom.client.KiwoomApiClient;
import dev.eolmae.marketmonitor.external.kiwoom.dto.Ka10051Request;
import dev.eolmae.marketmonitor.external.kiwoom.dto.Ka10051Response;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final String AMT_QTY_TP_AMOUNT = "0"; // ka10051 amt_qty_tp: 0=금액

	private final KiwoomApiClient kiwoomApiClient;
	private final InvestorTradingSummarySnapshotRepository investorTradingSummarySnapshotRepository;

	@Transactional
	public void collect(LocalDateTime snapshotTime) {
		for (MarketType marketType : MarketType.storableValues()) {
			try {
				collectForMarket(marketType, snapshotTime);
			} catch (Exception e) {
				log.error("투자자별매매종합 수집 실패: market={}", marketType, e);
			}
		}
	}

	private void collectForMarket(MarketType marketType, LocalDateTime snapshotTime) {
		Market m = Market.valueOf(marketType.name());
		var request = new Ka10051Request(m.mrktTp, AMT_QTY_TP_AMOUNT, snapshotTime.format(DATE_FMT), StexType.KRX_NXT.code());
		var response = kiwoomApiClient.post(request, Ka10051Response.class);

		if (response.indsNetprps() == null || response.indsNetprps().isEmpty()) {
			log.warn("투자자별매매종합 응답 없음: market={}", marketType);
			return;
		}

		String indsCd = Market.valueOf(marketType.name()).indsCd;
		Ka10051Response.IndsNetprps compositeItem = response.indsNetprps().stream()
			.filter(item -> item.indsCd() != null && item.indsCd().startsWith(indsCd))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException(
				"투자자별매매종합 종합지수 행 없음: market=" + marketType + ", indsCd=" + indsCd));

		LocalDateTime now = LocalDateTime.now();

		// ka10051은 순매수만 제공하므로 매수/매도는 ZERO로 저장
		saveSnapshot(marketType, InvestorType.PERSONAL, NumberParser.parseBigDecimal(compositeItem.indNetprps()), snapshotTime, now);
		saveSnapshot(marketType, InvestorType.FOREIGNER, NumberParser.parseBigDecimal(compositeItem.frgnrNetprps()), snapshotTime, now);
		saveSnapshot(marketType, InvestorType.INSTITUTION, NumberParser.parseBigDecimal(compositeItem.orgnNetprps()), snapshotTime, now);
		saveSnapshot(marketType, InvestorType.FINANCIAL_INVESTMENT, NumberParser.parseBigDecimal(compositeItem.scNetprps()), snapshotTime, now);
		saveSnapshot(marketType, InvestorType.TRUST, NumberParser.parseBigDecimal(compositeItem.invtrtNetprps()), snapshotTime, now);
		saveSnapshot(marketType, InvestorType.PENSION_FUND, NumberParser.parseBigDecimal(compositeItem.endwNetprps()), snapshotTime, now);
		saveSnapshot(marketType, InvestorType.PRIVATE_FUND, NumberParser.parseBigDecimal(compositeItem.samoFundNetprps()), snapshotTime, now);
		saveSnapshot(marketType, InvestorType.INSURANCE, NumberParser.parseBigDecimal(compositeItem.insrncNetprps()), snapshotTime, now);
		saveSnapshot(marketType, InvestorType.BANK, NumberParser.parseBigDecimal(compositeItem.bankNetprps()), snapshotTime, now);
		saveSnapshot(marketType, InvestorType.OTHER_CORP, NumberParser.parseBigDecimal(compositeItem.etcCorpNetprps()), snapshotTime, now);
		saveSnapshot(marketType, InvestorType.GOVERNMENT, NumberParser.parseBigDecimal(compositeItem.natnNetprps()), snapshotTime, now);
		saveSnapshot(marketType, InvestorType.OTHER_FINANCE, NumberParser.parseBigDecimal(compositeItem.jnsinkmNetprps()), snapshotTime, now);
		saveSnapshot(marketType, InvestorType.FOREIGN_COMPANY, NumberParser.parseBigDecimal(compositeItem.nativeTrmtFrgnrNetprps()), snapshotTime, now);

		log.debug("투자자별매매종합 수집 완료: market={}", marketType);
	}

	private enum Market {
		KOSPI("0", "001"),
		KOSDAQ("1", "101");
		final String mrktTp;  // ka10051 mrkt_tp
		final String indsCd;  // ka10051 inds_cd (응답 필터용)
		Market(String mrktTp, String indsCd) { this.mrktTp = mrktTp; this.indsCd = indsCd; }
	}

	private void saveSnapshot(MarketType marketType, InvestorType investorType,
		BigDecimal netBuyAmount, LocalDateTime snapshotTime, LocalDateTime now) {

		if (investorTradingSummarySnapshotRepository
			.findByMarketTypeAndInvestorTypeAndAmtQtyTypeAndSnapshotTime(
				marketType, investorType, AmtQtyType.AMOUNT, snapshotTime)
			.isEmpty()) {
			investorTradingSummarySnapshotRepository.save(
				InvestorTradingSummarySnapshot.create(
					marketType, investorType, AmtQtyType.AMOUNT, snapshotTime, now,
					BigDecimal.ZERO, BigDecimal.ZERO, netBuyAmount));
		}
	}
}
