package dev.eolmae.psms.domain.dashboard;

import dev.eolmae.psms.domain.common.ProgramRankingType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramTradingRankingSnapshotRepository extends JpaRepository<ProgramTradingRankingSnapshot, Long> {

	List<ProgramTradingRankingSnapshot> findBySnapshotTimeAndRankingTypeOrderByRankAsc(
		LocalDateTime snapshotTime,
		ProgramRankingType rankingType
	);
}
