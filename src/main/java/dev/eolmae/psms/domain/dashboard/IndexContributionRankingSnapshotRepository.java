package dev.eolmae.psms.domain.dashboard;

import dev.eolmae.psms.domain.common.MarketType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IndexContributionRankingSnapshotRepository extends JpaRepository<IndexContributionRankingSnapshot, Long> {

	List<IndexContributionRankingSnapshot> findBySnapshotTimeAndMarketTypeOrderByRankAsc(LocalDateTime snapshotTime, MarketType marketType);
}
