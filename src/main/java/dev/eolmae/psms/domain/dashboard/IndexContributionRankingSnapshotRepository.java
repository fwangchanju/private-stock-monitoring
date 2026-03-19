package dev.eolmae.psms.domain.dashboard;

import dev.eolmae.psms.domain.common.MarketType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IndexContributionRankingSnapshotRepository extends JpaRepository<IndexContributionRankingSnapshot, Long> {

	List<IndexContributionRankingSnapshot> findBySnapshotTimeAndMarketTypeOrderByRankAsc(LocalDateTime snapshotTime, MarketType marketType);

	@Query("SELECT MAX(s.snapshotTime) FROM IndexContributionRankingSnapshot s")
	Optional<LocalDateTime> findLatestSnapshotTime();
}
