package dev.eolmae.psms.domain.dashboard;

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

	@Query("SELECT MAX(s.snapshotTime) FROM ProgramTradingRankingSnapshot s")
	Optional<LocalDateTime> findLatestSnapshotTime();
}
