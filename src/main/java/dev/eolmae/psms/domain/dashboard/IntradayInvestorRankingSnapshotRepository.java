package dev.eolmae.psms.domain.dashboard;

import dev.eolmae.psms.domain.common.IntradayRankingType;
import dev.eolmae.psms.domain.common.InvestorType;
import dev.eolmae.psms.domain.common.MarketType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IntradayInvestorRankingSnapshotRepository extends JpaRepository<IntradayInvestorRankingSnapshot, Long> {

	List<IntradayInvestorRankingSnapshot> findBySnapshotTimeAndMarketTypeAndInvestorTypeAndRankingTypeOrderByRankAsc(
		LocalDateTime snapshotTime,
		MarketType marketType,
		InvestorType investorType,
		IntradayRankingType rankingType
	);

	@Query("SELECT MAX(s.snapshotTime) FROM IntradayInvestorRankingSnapshot s")
	Optional<LocalDateTime> findLatestSnapshotTime();
}
