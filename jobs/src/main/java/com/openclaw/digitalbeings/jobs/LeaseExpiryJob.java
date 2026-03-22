package com.openclaw.digitalbeings.jobs;

import com.openclaw.digitalbeings.application.lease.LeaseService;
import com.openclaw.digitalbeings.domain.being.Being;
import com.openclaw.digitalbeings.domain.runtime.AuthorityLease;
import com.openclaw.digitalbeings.application.support.BeingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LeaseExpiryJob {
    private static final Logger log = LoggerFactory.getLogger(LeaseExpiryJob.class);
    private final LeaseService leaseService;
    private final BeingStore beingStore;

    public LeaseExpiryJob(LeaseService leaseService, BeingStore beingStore) {
        this.leaseService = leaseService;
        this.beingStore = beingStore;
    }

    @Scheduled(fixedRate = 60000)
    public void scanAndExpireLeases() {
        log.debug("Scanning for expired leases...");
        for (Being being : beingStore.findAll()) {
            being.authorityLeases().stream()
                .filter(AuthorityLease::isExpired)
                .forEach(lease -> {
                    try {
                        leaseService.expireLease(being.beingId().value(), lease.leaseId(), "SYSTEM");
                        log.info("Expired lease {} for being {}", lease.leaseId(), being.beingId());
                    } catch (Exception e) {
                        log.error("Failed to expire lease {} for being {}", lease.leaseId(), being.beingId(), e);
                    }
                });
        }
    }
}
