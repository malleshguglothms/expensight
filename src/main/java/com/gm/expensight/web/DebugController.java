package com.gm.expensight.web;

import com.gm.expensight.domain.model.User;
import com.gm.expensight.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Debug controller to verify user persistence (development only)
 * Remove or secure this in production
 */
@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
public class DebugController {

    private final UserRepository userRepository;

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/users/count")
    public long getUserCount() {
        return userRepository.count();
    }
}

