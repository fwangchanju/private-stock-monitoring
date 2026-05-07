package dev.eolmae.psms.domain.dashboard;

import dev.eolmae.psms.domain.common.AmtQtyType;
import dev.eolmae.psms.domain.common.MarketType;
import dev.eolmae.psms.domain.common.ProgramRankingType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProgramTradingRankingSnapshotRepository extends JpaRepository<ProgramTradingRankingSnapshot, Long> {

	List<ProgramTradingRankingSnapshot> findBySnapshotTimeAndRankingTypeOrderByRankAsc(
		LocalDateTime snapshotTime,
		ProgramRankingType rankingType
	);

	boolean existsBySnapshotTimeAndMarketTypeAndRankingTypeAndAmtQtyType(
		LocalDateTime snapshotTime,
		MarketType marketType,
		ProgramRankingType rankingType,
		AmtQtyType amtQtyType
	);

	List<ProgramTradingRankingSnapshot> findBySnapshotTimeAndMarketTypeAndRankingTypeAndAmtQtyTypeOrderByRankAsc(
		LocalDateTime snapshotTime,
		MarketType marketType,
		ProgramRankingType rankingType,
		AmtQtyType amtQtyType
	);

	@Query("SELECT MAX(s.snapshotTime) FROM ProgramTradingRankingSnapshot s")
	Optional<LocalDateTime> findLatestSnapshotTime();
}
