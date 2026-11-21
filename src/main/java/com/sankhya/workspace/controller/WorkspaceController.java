package com.sankhya.workspace.controller;

import com.sankhya.security.CustomUserDetails;
import com.sankhya.user.entity.User;
import com.sankhya.user.service.UserService;
import com.sankhya.workspace.dto.WorkspaceCreateRequest;
import com.sankhya.workspace.dto.WorkspaceResponse;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.service.WorkspaceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final UserService userService;

    public WorkspaceController(WorkspaceService workspaceService, UserService userService) {
        this.workspaceService = workspaceService;
        this.userService = userService;
    }

    @GetMapping
    public String listWorkspaces(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 HttpSession session,
                                 Model model) {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        Long activeId = activeWorkspace != null ? activeWorkspace.getId() : null;

        List<WorkspaceResponse> workspaces = workspaceService.getUserWorkspaces(userDetails.getId(), activeId);

        model.addAttribute("currentUser", userDetails);
        model.addAttribute("activeWorkspace", activeWorkspace);
        model.addAttribute("workspaces", workspaces);
        model.addAttribute("currentPage", "workspaces");

        return "workspace/list";
    }

    @GetMapping("/create")
    public String showCreateWorkspacePage(@AuthenticationPrincipal CustomUserDetails userDetails,
                                          HttpSession session,
                                          Model model) {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        List<WorkspaceResponse> workspaces = workspaceService.getUserWorkspaces(
                userDetails.getId(),
                activeWorkspace != null ? activeWorkspace.getId() : null
        );

        if (!model.containsAttribute("workspaceRequest")) {
            model.addAttribute("workspaceRequest", new WorkspaceCreateRequest());
        }

        model.addAttribute("currentUser", userDetails);
        model.addAttribute("activeWorkspace", activeWorkspace);
        model.addAttribute("workspaces", workspaces);
        model.addAttribute("currentPage", "workspaces");

        return "workspace/create";
    }

    @PostMapping("/create")
    public String handleCreateWorkspace(@AuthenticationPrincipal CustomUserDetails userDetails,
                                        @Valid @ModelAttribute("workspaceRequest") WorkspaceCreateRequest request,
                                        BindingResult bindingResult,
                                        HttpSession session,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
            List<WorkspaceResponse> workspaces = workspaceService.getUserWorkspaces(
                    userDetails.getId(),
                    activeWorkspace != null ? activeWorkspace.getId() : null
            );
            model.addAttribute("currentUser", userDetails);
            model.addAttribute("activeWorkspace", activeWorkspace);
            model.addAttribute("workspaces", workspaces);
            model.addAttribute("currentPage", "workspaces");
            return "workspace/create";
        }

        User owner = userService.findById(userDetails.getId());
        Workspace created = workspaceService.createWorkspace(request, owner);

        // Make newly created workspace the active workspace
        workspaceService.setActiveWorkspace(created.getId(), session, userDetails.getId());

        redirectAttributes.addFlashAttribute("successMessage",
                "Supermarket workspace '" + created.getName() + "' created and activated successfully.");

        return "redirect:/workspaces";
    }

    @PostMapping("/{id}/select")
    public String selectActiveWorkspace(@PathVariable("id") Long workspaceId,
                                        @AuthenticationPrincipal CustomUserDetails userDetails,
                                        HttpSession session,
                                        HttpServletRequest request,
                                        RedirectAttributes redirectAttributes) {
        Workspace workspace = workspaceService.getWorkspaceByIdAndValidateAccess(workspaceId, userDetails.getId());
        workspaceService.setActiveWorkspace(workspace.getId(), session, userDetails.getId());

        redirectAttributes.addFlashAttribute("successMessage",
                "Switched active workspace to '" + workspace.getName() + "'.");

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank() && !referer.contains("/workspaces/create")) {
            return "redirect:" + referer;
        }

        return "redirect:/dashboard";
    }
}
