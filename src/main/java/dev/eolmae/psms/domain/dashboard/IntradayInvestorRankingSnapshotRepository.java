package dev.eolmae.psms.domain.dashboard;

import dev.eolmae.psms.domain.common.IntradayRankingType;
import dev.eolmae.psms.domain.common.InvestorType;
import dev.eolmae.psms.domain.common.MarketType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntradayInvestorRankingSnapshotRepository extends JpaRepository<IntradayInvestorRankingSnapshot, Long> {

	List<IntradayInvestorRankingSnapshot> findBySnapshotTimeAndMarketTypeAndInvestorTypeAndRankingTypeOrderByRankAsc(
		LocalDateTime snapshotTime,
		MarketType marketType,
		InvestorType investorType,
		IntradayRankingType rankingType
	);
}
