package dev.eolmae.psms.domain.dashboard;

import dev.eolmae.psms.domain.common.MarketType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketOverviewSnapshotRepository extends JpaRepository<MarketOverviewSnapshot, Long> {

	Optional<MarketOverviewSnapshot> findByMarketTypeAndSnapshotTime(MarketType marketType, LocalDateTime snapshotTime);
}
