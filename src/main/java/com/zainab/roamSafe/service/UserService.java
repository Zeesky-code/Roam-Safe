package com.zainab.roamSafe.service;

import com.zainab.roamSafe.dto.LoginRequest;
import com.zainab.roamSafe.dto.RegisterRequest;
import com.zainab.roamSafe.model.User;
import com.zainab.roamSafe.model.UserRole;
import com.zainab.roamSafe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    public boolean registerUser(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            return false;
        }
        
        // Validate password confirmation
        if (!request.isPasswordMatching()) {
            return false;
        }
        
        // Create new user
        User user = new User();
        user.setEmail(request.getEmail().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        
        try {
            userRepository.save(user);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public Optional<User> authenticateUser(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByEmailAndEnabledTrue(request.getEmail().toLowerCase());
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                // Update last login
                user.setLastLoginAt(LocalDateTime.now());
                userRepository.save(user);
                return Optional.of(user);
            }
        }
        
        return Optional.empty();
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase());
    }
    
    public boolean updateUserPreferences(Long userId, String preferredCity, String notificationPreferences) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPreferredCity(preferredCity);
            user.setNotificationPreferences(notificationPreferences);
            userRepository.save(user);
            return true;
        }
        return false;
    }
    
    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(currentPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }
    
    public boolean deleteAccount(Long userId, String password) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                userRepository.delete(user);
                return true;
            }
        }
        return false;
    }
} 