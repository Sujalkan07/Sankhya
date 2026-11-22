package com.sankhya.dashboard.controller;

import com.sankhya.security.CustomUserDetails;
import com.sankhya.workspace.dto.WorkspaceResponse;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.service.WorkspaceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final WorkspaceService workspaceService;

    public SettingsController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public String showSettingsPage(@AuthenticationPrincipal CustomUserDetails userDetails,
                                  HttpSession session,
                                  Model model) {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        List<WorkspaceResponse> workspaces = workspaceService.getUserWorkspaces(
                userDetails.getId(),
                activeWorkspace != null ? activeWorkspace.getId() : null
        );

        model.addAttribute("currentUser", userDetails);
        model.addAttribute("activeWorkspace", activeWorkspace);
        model.addAttribute("workspaces", workspaces);
        model.addAttribute("activeModule", "settings");
        model.addAttribute("activeSubPage", "settings");
        model.addAttribute("currentPage", "settings");

        return "settings/index";
    }
}
