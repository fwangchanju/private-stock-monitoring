package dev.eolmae.psms.api.dto;

import dev.eolmae.psms.domain.common.InvestorType;
import dev.eolmae.psms.domain.common.MarketType;
import java.math.BigDecimal;

public record IntradayInvestorRankingItem(
	MarketType marketType,
	InvestorType investorType,
	int rank,
	String stockCode,
	String stockName,
	BigDecimal netBuyAmount,
	long tradedVolume
) {
}
