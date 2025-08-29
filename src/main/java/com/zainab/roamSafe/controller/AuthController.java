package com.zainab.roamSafe.controller;

import com.zainab.roamSafe.dto.LoginRequest;
import com.zainab.roamSafe.dto.RegisterRequest;
import com.zainab.roamSafe.model.User;
import com.zainab.roamSafe.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }
    
    @PostMapping("/login")
    public String login(@Valid LoginRequest loginRequest, 
                       BindingResult bindingResult,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            return "login";
        }
        
        Optional<User> userOpt = userService.authenticateUser(loginRequest);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            session.setAttribute("user", user);
            session.setAttribute("userId", user.getId());
            session.setAttribute("userEmail", user.getEmail());
            session.setAttribute("userName", user.getFullName());
            
            redirectAttributes.addFlashAttribute("success", "Welcome back, " + user.getFirstName() + "! ✨");
            return "redirect:/dashboard";
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid email or password");
            return "redirect:/login?error=true";
        }
    }
    
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }
    
    @PostMapping("/register")
    public String register(@Valid RegisterRequest registerRequest,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            return "register";
        }
        
        // Check if passwords match
        if (!registerRequest.isPasswordMatching()) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/register";
        }
        
        boolean success = userService.registerUser(registerRequest);
        
        if (success) {
            redirectAttributes.addFlashAttribute("success", "Account created successfully! Please log in. ✨");
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("error", "Email already exists or registration failed");
            return "redirect:/register";
        }
    }
    
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "You have been logged out successfully");
        return "redirect:/";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("user", user);
        return "dashboard";
    }
    
    @PostMapping("/dashboard/preferences")
    public String updatePreferences(@RequestParam String preferredCity,
                                   @RequestParam String notificationPreferences,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        boolean success = userService.updateUserPreferences(user.getId(), preferredCity, notificationPreferences);
        
        if (success) {
            // Update session user
            user.setPreferredCity(preferredCity);
            user.setNotificationPreferences(notificationPreferences);
            session.setAttribute("user", user);
            
            redirectAttributes.addFlashAttribute("success", "Preferences updated successfully! ✨");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to update preferences");
        }
        
        return "redirect:/dashboard";
    }
    
    @PostMapping("/dashboard/delete")
    public String deleteAccount(@RequestParam String password,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        boolean success = userService.deleteAccount(user.getId(), password);
        
        if (success) {
            session.invalidate();
            redirectAttributes.addFlashAttribute("success", "Account deleted successfully");
            return "redirect:/";
        } else {
            redirectAttributes.addFlashAttribute("error", "Incorrect password or deletion failed");
            return "redirect:/dashboard";
        }
    }
} 