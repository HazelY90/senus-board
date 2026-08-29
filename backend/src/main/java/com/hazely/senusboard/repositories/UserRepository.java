package com.hazely.senusboard.repositories;

import com.hazely.senusboard.entities.UserEntity;
import com.hazely.senusboard.entities.enums.Role;
import com.hazely.senusboard.entities.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByEmail(String email);
    boolean existsByRole(Role role);
    Optional<UserEntity> findByEmail(String email);
    List<UserEntity> findAllByStatus(Status status);
}
