package com.sankhya.auth.controller;

import com.sankhya.auth.dto.RegisterRequest;
import com.sankhya.auth.service.AuthService;
import com.sankhya.auth.validator.RegisterValidator;
import com.sankhya.common.exception.DuplicateResourceException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AuthService authService;
    private final RegisterValidator registerValidator;

    public AuthController(AuthService authService, RegisterValidator registerValidator) {
        this.authService = authService;
        this.registerValidator = registerValidator;
    }

    private boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    @GetMapping("/login")
    public String showLoginPage(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                @RequestParam(value = "expired", required = false) String expired,
                                @RequestParam(value = "registered", required = false) String registered,
                                Model model) {
        if (isAuthenticated()) {
            return "redirect:/dashboard";
        }

        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("infoMessage", "You have been successfully signed out.");
        }
        if (expired != null) {
            model.addAttribute("warningMessage", "Your session has expired. Please sign in again.");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Account created successfully! Please sign in.");
        }

        return "auth/login";
    }

    @GetMapping("/favicon.ico")
    @ResponseBody
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/register")
    public String showRegistrationPage(Model model) {
        if (isAuthenticated()) {
            return "redirect:/dashboard";
        }

        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegistration(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
                                     BindingResult bindingResult,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        registerValidator.validate(registerRequest, bindingResult);

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            authService.registerUser(registerRequest);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Welcome to Sankhya! Your account and initial supermarket workspace are ready. Please sign in.");
            return "redirect:/login";
        } catch (DuplicateResourceException ex) {
            bindingResult.rejectValue("email", "email.duplicate", ex.getMessage());
            return "auth/register";
        }
    }
}
