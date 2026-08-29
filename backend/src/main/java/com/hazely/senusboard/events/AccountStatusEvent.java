package com.hazely.senusboard.events;

import com.hazely.senusboard.entities.enums.Status;

/** Carries a committed account status change that requires client sign-out. */
public record AccountStatusEvent(Long userId, Status status) {
}
