package com.hazely.senusboard.security;

import com.hazely.senusboard.entities.enums.Status;
import com.hazely.senusboard.events.AccountStatusEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountStatusListenerTest {

    @Mock
    private AccountEventService eventService;

    @Test
    void handleSendsCommittedStatus() {
        AccountStatusListener listener = new AccountStatusListener(eventService);

        listener.handle(new AccountStatusEvent(7L, Status.REJECTED));

        verify(eventService).revoke(7L, Status.REJECTED);
    }
}
