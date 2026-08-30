package com.hazely.senusboard.security;

import com.hazely.senusboard.events.AccountStatusEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Sends account-access events only after the status transaction commits. */
@Component
@RequiredArgsConstructor
public class AccountStatusListener {

    private final AccountEventService eventService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AccountStatusEvent event) {
        eventService.revoke(event.userId(), event.status());
    }
}
