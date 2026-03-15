package dev.eolmae.psms.domain.stock;

import dev.eolmae.psms.domain.common.MarketType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@Entity
@Table(name = "stock_master")
public class StockMaster {

	@Id
	@Column(length = 20)
	private String stockCode;

	@Column(nullable = false, length = 100)
	private String stockName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MarketType marketType;

	@Column(nullable = false)
	private boolean active;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	protected StockMaster() {
	}
}
