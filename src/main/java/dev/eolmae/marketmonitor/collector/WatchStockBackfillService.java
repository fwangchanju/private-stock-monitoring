package dev.eolmae.marketmonitor.collector;

import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchStockBackfillService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final ShortSellingCollector shortSellingCollector;
	private final ProgramTradingCollector programTradingCollector;

	@Async
	public void backfill(String stockCode) {
		LocalDateTime snapshotTime = LocalDateTime.now(KST).withMinute(0).withSecond(0).withNano(0);
		log.info("관심종목 백필 시작: stockCode={}, snapshotTime={}", stockCode, snapshotTime);

		try {
			shortSellingCollector.backfill(stockCode, snapshotTime);
		} catch (Exception e) {
			log.error("공매도 백필 실패: stockCode={}", stockCode, e);
		}

		try {
			programTradingCollector.backfill(stockCode, snapshotTime);
		} catch (Exception e) {
			log.error("프로그램매매 백필 실패: stockCode={}", stockCode, e);
		}

		log.info("관심종목 백필 완료: stockCode={}", stockCode);
	}
}
