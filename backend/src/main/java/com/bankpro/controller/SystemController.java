package com.bankpro.controller;

import com.bankpro.config.AppProperties;
import com.bankpro.dto.SystemInfoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Small public endpoint the frontend uses to confirm it can reach the backend.
 * It reveals nothing sensitive: app name, version and server time.
 */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    private static final DateTimeFormatter LOCAL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Clock clock;
    private final AppProperties appProperties;

    public SystemController(Clock clock, AppProperties appProperties) {
        this.clock = clock;
        this.appProperties = appProperties;
    }

    @GetMapping("/info")
    public SystemInfoResponse info() {
        Instant now = clock.instant();
        String local = LOCAL_FORMAT.format(now.atZone(appProperties.businessZone()));
        return new SystemInfoResponse("BankPro", "0.1.0", appProperties.businessZone().getId(), now, local);
    }
}
