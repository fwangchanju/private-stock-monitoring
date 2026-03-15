package dev.eolmae.psms.domain.dashboard;

import dev.eolmae.psms.domain.common.InvestorType;
import dev.eolmae.psms.domain.common.MarketType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@Entity
@Table(name = "investor_trading_summary")
public class InvestorTradingSummary {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MarketType marketType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private InvestorType investorType;

	@Column(nullable = false)
	private LocalDateTime snapshotTime;

	@Column(nullable = false)
	private LocalDateTime lastCollectedAt;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal buyAmount;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal sellAmount;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal netBuyAmount;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	protected InvestorTradingSummary() {
	}
}
