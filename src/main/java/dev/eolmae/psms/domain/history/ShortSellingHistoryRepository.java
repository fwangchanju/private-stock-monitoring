package dev.eolmae.psms.domain.history;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortSellingHistoryRepository extends JpaRepository<ShortSellingHistory, Long> {

	List<ShortSellingHistory> findByStockCodeAndTradeDateBetweenOrderByTradeDateAsc(String stockCode, LocalDate from, LocalDate to);

	boolean existsByStockCodeAndTradeDate(String stockCode, LocalDate tradeDate);

	Optional<ShortSellingHistory> findByStockCodeAndTradeDate(String stockCode, LocalDate tradeDate);
}
