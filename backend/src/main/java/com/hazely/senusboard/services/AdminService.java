package com.hazely.senusboard.services;

import com.hazely.senusboard.dtos.CreateUserRequestDto;
import com.hazely.senusboard.dtos.UserDto;
import com.hazely.senusboard.entities.UserEntity;
import com.hazely.senusboard.entities.enums.Role;
import com.hazely.senusboard.entities.enums.Status;
import com.hazely.senusboard.events.AccountStatusEvent;
import com.hazely.senusboard.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Handles administrative account-management workflows. */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final UserPolicy policy;
    private final UserMapper mapper;
    private final ApplicationEventPublisher publisher;

    /** Creates an active ordinary account without registration review. */
    @Transactional
    public UserDto createUser(CreateUserRequestDto request) {
        String email = policy.cleanEmail(request.email());
        policy.validateRole(request.role());
        policy.validateDomain(email);
        policy.validatePassword(request.password(), request.role());

        if (userRepo.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        UserEntity user = new UserEntity();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(encoder.encode(request.password()));
        user.setRole(request.role());
        user.setOrganization(request.organization().trim());
        user.setStatus(Status.ACTIVE);

        return mapper.toDto(userRepo.save(user));
    }

    /** Returns every account awaiting administrative review. */
    @Transactional(readOnly = true)
    public List<UserDto> getPending() {
        return userRepo.findAllByStatus(Status.PENDING).stream()
                .map(mapper::toDto)
                .toList();
    }

    /** Finds an account by its normalised email address. */
    @Transactional(readOnly = true)
    public UserDto searchUser(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        String cleanEmail = policy.cleanEmail(email);
        UserEntity user = userRepo.findByEmail(cleanEmail).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        );
        return mapper.toDto(user);
    }

    /** Applies an approval decision to a pending registration. */
    @Transactional
    public void verifyUser(Long id, boolean isApproved) {
        UserEntity user = getUser(id);
        if (user.getStatus() != Status.PENDING || user.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only pending ordinary accounts can be reviewed"
            );
        }

        user.setStatus(isApproved ? Status.ACTIVE : Status.REJECTED);
        userRepo.save(user);
        if (!isApproved) {
            publisher.publishEvent(new AccountStatusEvent(user.getId(), user.getStatus()));
        }
    }

    /** Disables an active ordinary account. */
    @Transactional
    public void disableUser(Long id) {
        UserEntity user = getUser(id);
        if (user.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Admin accounts cannot be disabled through this endpoint"
            );
        }
        if (user.getStatus() != Status.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only active accounts can be disabled"
            );
        }

        user.setStatus(Status.DISABLED);
        userRepo.save(user);
        publisher.publishEvent(new AccountStatusEvent(user.getId(), user.getStatus()));
    }

    private UserEntity getUser(Long id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID must be positive");
        }
        return userRepo.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found: " + id
        ));
    }
}
