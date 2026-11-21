package com.sankhya.auth.validator;

import com.sankhya.auth.dto.RegisterRequest;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class RegisterValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return RegisterRequest.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        RegisterRequest request = (RegisterRequest) target;

        if (request.getPassword() != null && request.getConfirmPassword() != null) {
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                errors.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
            }
        }

        if (!request.isTermsAccepted()) {
            errors.rejectValue("termsAccepted", "terms.required", "You must agree to the Terms of Service");
        }
    }
}
