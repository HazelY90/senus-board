package com.hazely.senusboard.security;

import com.hazely.senusboard.entities.enums.Status;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Manages authenticated SSE connections and account-access events. */
@Service
public class AccountEventService {

    private static final long TIMEOUT = 30 * 60 * 1000L;

    private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /** Registers one browser connection for an authenticated account. */
    public SseEmitter connect(Long id) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.computeIfAbsent(id, key -> ConcurrentHashMap.newKeySet()).add(emitter);

        Runnable cleanup = () -> remove(id, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ex -> cleanup.run());

        try {
            emitter.send(SseEmitter.event().name("connected").data("connected"));
        } catch (IOException | IllegalStateException ex) {
            remove(id, emitter);
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    /** Notifies every active browser connection that account access has ended. */
    public void revoke(Long id, Status status) {
        Set<SseEmitter> targets = emitters.remove(id);
        if (targets == null) {
            return;
        }

        for (SseEmitter emitter : targets) {
            try {
                emitter.send(SseEmitter.event()
                        .name("account-access-revoked")
                        .data(Map.of("status", status.name())));
                emitter.complete();
            } catch (IOException | IllegalStateException ex) {
                emitter.completeWithError(ex);
            }
        }
    }

    private void remove(Long id, SseEmitter emitter) {
        emitters.computeIfPresent(id, (key, values) -> {
            values.remove(emitter);
            return values.isEmpty() ? null : values;
        });
    }
}
