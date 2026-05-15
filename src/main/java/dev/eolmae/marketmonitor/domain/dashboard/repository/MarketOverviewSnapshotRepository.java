package dev.eolmae.marketmonitor.domain.dashboard.repository;
import dev.eolmae.marketmonitor.domain.dashboard.*;

import dev.eolmae.marketmonitor.common.enums.MarketType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketOverviewSnapshotRepository extends JpaRepository<MarketOverviewSnapshot, Long> {

	Optional<MarketOverviewSnapshot> findByMarketTypeAndSnapshotTime(MarketType marketType, LocalDateTime snapshotTime);
}
