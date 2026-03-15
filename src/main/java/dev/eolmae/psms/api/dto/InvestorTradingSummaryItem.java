package dev.eolmae.psms.api.dto;

import dev.eolmae.psms.domain.common.InvestorType;
import dev.eolmae.psms.domain.common.MarketType;
import java.math.BigDecimal;

public record InvestorTradingSummaryItem(
	MarketType marketType,
	InvestorType investorType,
	BigDecimal buyAmount,
	BigDecimal sellAmount,
	BigDecimal netBuyAmount
) {
}
