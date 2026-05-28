package dev.eolmae.marketmonitor.domain.dashboard.repository;
import dev.eolmae.marketmonitor.domain.dashboard.*;

import dev.eolmae.marketmonitor.common.enums.MarketType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MarketOverviewSnapshotRepository extends JpaRepository<MarketOverviewSnapshot, Long> {

	Optional<MarketOverviewSnapshot> findByMarketTypeAndSnapshotTime(MarketType marketType, LocalDateTime snapshotTime);

	List<MarketOverviewSnapshot> findBySnapshotTimeOrderByMarketTypeAsc(LocalDateTime snapshotTime);

	Optional<MarketOverviewSnapshot> findTopByMarketTypeOrderBySnapshotTimeDesc(MarketType marketType);

	@Query("SELECT MAX(s.snapshotTime) FROM MarketOverviewSnapshot s")
	Optional<LocalDateTime> findLatestSnapshotTime();
}
