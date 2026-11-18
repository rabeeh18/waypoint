package com.parcelo.common.time;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.parcelo.common.time.OperationalClock;
import com.parcelo.config.AppMode;
import com.parcelo.settings.SettingsService;

/**
 * Demo: restores the saved clock on start-up and saves it periodically, so the demo day survives
 * restarts. Production: the clock is plain real time.
 */
@Component
public class ClockKeeper {

    private static final Logger log = LoggerFactory.getLogger(ClockKeeper.class);

    private final OperationalClock clock;
    private final SettingsService settings;
    private final AppMode mode;
    private volatile boolean restored;

    public ClockKeeper(OperationalClock clock, SettingsService settings, AppMode mode) {
        this.clock = clock;
        this.settings = settings;
        this.mode = mode;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(0)
    public void restore() {
        settings.reload();
        if (!mode.demo()) {
            clock.restore(Instant.now(), 1, false);
            log.info("Operational clock on real time");
            return;
        }
        String saved = settings.get("clock.saved");
        Instant savedAt = saved.isBlank() ? null : Instant.parse(saved);
        clock.restore(savedAt, 1, false);
        restored = true;
        log.info("Operational clock at {}", clock.now());
    }

    @Scheduled(fixedDelay = 15_000, initialDelay = 15_000)
    public void save() {
        if (restored) {
            settings.writeInternal("clock.saved", clock.now().toString());
        }
    }

    public void saveControls() {
        save();
    }
}
