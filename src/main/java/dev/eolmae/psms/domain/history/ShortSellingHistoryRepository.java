package dev.eolmae.psms.domain.history;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortSellingHistoryRepository extends JpaRepository<ShortSellingHistory, Long> {

	List<ShortSellingHistory> findByStockCodeAndTradeDateBetweenOrderByTradeDateAsc(String stockCode, LocalDate from, LocalDate to);
}
