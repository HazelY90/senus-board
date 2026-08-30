package com.hazely.senusboard.controllers;

import com.hazely.senusboard.dtos.CreateUserRequestDto;
import com.hazely.senusboard.dtos.UserDto;
import com.hazely.senusboard.dtos.VerifyUserRequestDto;
import com.hazely.senusboard.services.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Exposes administrative account-management operations. */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService service;

    /** Creates an active ordinary account. */
    @PostMapping("/create-user")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createUser(request));
    }

    /** Returns every account awaiting administrative review. */
    @GetMapping("/get-pending")
    public List<UserDto> getPending() {
        return service.getPending();
    }

    /** Finds an account by its normalised email address. */
    @GetMapping("/search-user")
    public UserDto searchUser(@RequestParam String email) {
        return service.searchUser(email);
    }

    /** Approves or rejects a pending registration. */
    @PostMapping("/verify-user/{id}")
    public ResponseEntity<Void> verifyUser(
            @PathVariable Long id,
            @Valid @RequestBody VerifyUserRequestDto request
    ) {
        service.verifyUser(id, request.isApproved());
        return ResponseEntity.noContent().build();
    }

    /** Disables an active ordinary account. */
    @PostMapping("/disable-user/{id}")
    public ResponseEntity<Void> disableUser(@PathVariable Long id) {
        service.disableUser(id);
        return ResponseEntity.noContent().build();
    }
}
