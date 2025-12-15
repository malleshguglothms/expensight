package com.gm.expensight.service;

import com.gm.expensight.domain.model.OAuthProvider;
import com.gm.expensight.domain.model.User;
import com.gm.expensight.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Optional<User> getCurrentUser(OAuth2User oauth2User) {
        if (oauth2User == null) {
            log.debug("OAuth2User is null");
            return Optional.empty();
        }
        
        // Try to get user by oauthUserId (sub attribute)
        String oauthUserId = oauth2User.getAttribute("sub");
        if (oauthUserId != null) {
            log.debug("Looking up user by oauthUserId: {}", oauthUserId);
            Optional<User> userByOauthId = userRepository.findByOauthUserId(oauthUserId);
            if (userByOauthId.isPresent()) {
                return userByOauthId;
            }
            log.debug("User not found by oauthUserId: {}", oauthUserId);
        } else {
            log.debug("OAuth2User 'sub' attribute is null. Available attributes: {}", oauth2User.getAttributes().keySet());
        }
        
        // Fallback: try to get user by email
        String email = oauth2User.getAttribute("email");
        if (email != null) {
            log.debug("Fallback: Looking up user by email: {}", email);
            Optional<User> userByEmail = userRepository.findByEmail(email);
            if (userByEmail.isPresent()) {
                return userByEmail;
            }
            log.debug("User not found by email: {}", email);
        }
        
        return Optional.empty();
    }

    @Transactional
    public User saveOrUpdateUser(String oauthUserId, String email, String name, OAuthProvider provider) {
        User user = userRepository
                .findByOauthUserId(oauthUserId)
                .orElseGet(() -> {
                    log.info("Creating new user with oauthUserId: {}", oauthUserId);
                    return User.builder()
                            .oauthProvider(provider)
                            .oauthUserId(oauthUserId)
                            .email(email)
                            .name(name)
                            .build();
                });

        // Update mutable fields
        if (user.getEmail() == null || !email.equals(user.getEmail())) {
            user.setEmail(email);
        }
        if (user.getName() == null || !name.equals(user.getName())) {
            user.setName(name);
        }

        User saved = userRepository.saveAndFlush(user);
        log.info("User saved - ID: {}, Email: {}, New: {}", saved.getId(), saved.getEmail(), saved.getId() != null && user.getId() == null);
        return saved;
    }
}

