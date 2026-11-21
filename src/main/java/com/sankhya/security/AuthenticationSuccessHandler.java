package com.sankhya.security;

import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.repository.WorkspaceRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class AuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    public static final String ACTIVE_WORKSPACE_ID_SESSION_KEY = "ACTIVE_WORKSPACE_ID";

    private final WorkspaceRepository workspaceRepository;

    public AuthenticationSuccessHandler(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
        setDefaultTargetUrl("/dashboard");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            HttpSession session = request.getSession(true);
            Long existingActiveId = (Long) session.getAttribute(ACTIVE_WORKSPACE_ID_SESSION_KEY);

            if (existingActiveId == null) {
                List<Workspace> accessible = workspaceRepository.findAllAccessibleByUserId(userDetails.getId());
                if (!accessible.isEmpty()) {
                    session.setAttribute(ACTIVE_WORKSPACE_ID_SESSION_KEY, accessible.get(0).getId());
                }
            }
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}
