package dev.eolmae.psms.domain.history;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramTradingHistoryRepository extends JpaRepository<ProgramTradingHistory, Long> {

	List<ProgramTradingHistory> findByStockCodeAndSnapshotTimeBetweenOrderBySnapshotTimeAsc(
		String stockCode,
		LocalDateTime from,
		LocalDateTime to
	);
}
