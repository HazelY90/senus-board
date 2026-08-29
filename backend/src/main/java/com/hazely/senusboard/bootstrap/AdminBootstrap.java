package com.hazely.senusboard.bootstrap;

import com.hazely.senusboard.entities.UserEntity;
import com.hazely.senusboard.entities.enums.Role;
import com.hazely.senusboard.entities.enums.Status;
import com.hazely.senusboard.repositories.UserRepository;
import com.hazely.senusboard.services.UserPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the initial Admin account when bootstrap is explicitly enabled. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.admin.bootstrap", name = "enabled", havingValue = "true")
public class AdminBootstrap implements ApplicationRunner {

    private final AdminBootstrapProperties props;
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final UserPolicy policy;

    /** Creates one Admin after migrations and application context initialisation. */
    @Override
    public void run(ApplicationArguments args) {
        try {
            if (userRepo.existsByRole(Role.ADMIN)) {
                return;
            }
            validateConfig();

            String email = policy.cleanEmail(props.getEmail());
            policy.validateDomain(email);
            policy.validatePassword(props.getPassword(), Role.ADMIN);
            if (userRepo.existsByEmail(email)) {
                throw new IllegalStateException("Admin bootstrap email is already registered");
            }

            UserEntity user = new UserEntity();
            user.setName(props.getName().trim());
            user.setEmail(email);
            user.setPassword(encoder.encode(props.getPassword()));
            user.setRole(Role.ADMIN);
            user.setOrganization(props.getOrganization().trim());
            user.setStatus(Status.ACTIVE);
            userRepo.save(user);
        } finally {
            props.setPassword(null);
        }
    }

    private void validateConfig() {
        if (isBlank(props.getEmail())
                || isBlank(props.getPassword())
                || isBlank(props.getName())
                || isBlank(props.getOrganization())) {
            throw new IllegalStateException("Admin bootstrap configuration is incomplete");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
