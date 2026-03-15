package dev.eolmae.psms.domain.dashboard;

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
@Table(name = "market_overview_snapshot")
public class MarketOverviewSnapshot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MarketType marketType;

	@Column(nullable = false)
	private LocalDateTime snapshotTime;

	@Column(nullable = false)
	private LocalDateTime lastCollectedAt;

	@Column(nullable = false, length = 30)
	private String marketStatus;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal indexValue;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal changeValue;

	@Column(nullable = false, precision = 9, scale = 4)
	private BigDecimal changeRate;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal tradingValue;

	@Column(nullable = false)
	private int advancers;

	@Column(nullable = false)
	private int decliners;

	@Column(nullable = false)
	private int unchangedCount;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	protected MarketOverviewSnapshot() {
	}
}
