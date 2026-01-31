package com.demo.cloud.controller;

import com.demo.cloud.entity.User;
import com.demo.cloud.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Gestion des utilisateurs")
@CrossOrigin(origins = "*") // pour le front React en dev
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/blocked")
    @Operation(summary = "Lister les utilisateurs bloqués")
    public List<User> getBlockedUsers() {
        return userRepository.findByLockedUntilIsNotNull();
    }

    @GetMapping
    @Operation(summary = "Lister tous les utilisateurs")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
