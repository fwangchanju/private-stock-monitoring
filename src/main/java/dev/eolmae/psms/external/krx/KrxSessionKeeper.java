package dev.eolmae.psms.external.krx;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * KRX 세션 유지 컴포넌트.
 * KRX 세션은 30분 비활동 시 만료되므로, 20~25분 사이 랜덤 간격으로 extendSession.cmd 를 호출한다.
 * 쿠키 파일이 없거나 세션 만료 상태이면 호출을 스킵한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KrxSessionKeeper {

    private static final int MIN_DELAY_MINUTES = 20;
    private static final int MAX_DELAY_MINUTES = 25;

    private final KrxCrawler krxCrawler;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
        r -> new Thread(r, "krx-session-keeper")
    );

    @PostConstruct
    public void start() {
        scheduleNext();
        log.info("KRX 세션 유지 스케줄러 시작 ({}~{}분 랜덤 간격)", MIN_DELAY_MINUTES, MAX_DELAY_MINUTES);
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdown();
    }

    private void scheduleNext() {
        int delay = ThreadLocalRandom.current().nextInt(MIN_DELAY_MINUTES, MAX_DELAY_MINUTES + 1);
        scheduler.schedule(this::extendAndReschedule, delay, TimeUnit.MINUTES);
        log.debug("KRX 세션 연장 다음 실행 예약: {}분 후", delay);
    }

    private void extendAndReschedule() {
        try {
            krxCrawler.extendSession();
        } catch (Exception e) {
            log.warn("KRX 세션 연장 중 예외: {}", e.getMessage());
        } finally {
            scheduleNext();
        }
    }
}
