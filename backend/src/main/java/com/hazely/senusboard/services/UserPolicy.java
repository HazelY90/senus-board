package com.hazely.senusboard.services;

import com.hazely.senusboard.entities.enums.Role;
import com.hazely.senusboard.entities.enums.Status;
import com.hazely.senusboard.security.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Applies shared account validation and availability rules. */
@Component
@RequiredArgsConstructor
public class UserPolicy {

    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern NUMBER = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9\\s]");

    private final AuthProperties props;

    /** Normalises an email address for lookup and persistence. */
    public String cleanEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /** Rejects roles that cannot be assigned to an ordinary account. */
    public void validateRole(Role role) {
        if (role == null || role == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ordinary role is required");
        }
    }

    /** Requires an exact match against the configured enterprise domains. */
    public void validateDomain(String email) {
        List<String> domains = props.getEmailDomains().stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList();
        if (domains.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Registration email domains are not configured"
            );
        }

        int at = email.lastIndexOf('@');
        String domain = at < 0 ? "" : email.substring(at + 1);
        if (!domains.contains(domain)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email domain is not allowed");
        }
    }

    /** Applies the password strength required by the account role. */
    public void validatePassword(String password, Role role) {
        int minLength = role == Role.ADMIN ? 16 : 10;
        boolean isStrong = password.length() >= minLength
                && UPPER.matcher(password).find()
                && LOWER.matcher(password).find()
                && NUMBER.matcher(password).find()
                && SPECIAL.matcher(password).find();
        if (!isStrong) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    ("Password must contain at least %d characters, including uppercase, lowercase, "
                            + "number, and special characters").formatted(minLength)
            );
        }
    }

    /** Identifies account states that retain platform access. */
    public boolean isAvailable(Status status) {
        return status == Status.PENDING || status == Status.ACTIVE;
    }
}
