package com.gm.expensight.repository;

import com.gm.expensight.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByOauthUserId(String oauthUserId);

    Optional<User> findByEmail(String email);
}
