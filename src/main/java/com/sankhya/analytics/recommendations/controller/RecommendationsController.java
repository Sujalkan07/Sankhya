package com.sankhya.analytics.recommendations.controller;

import com.sankhya.analytics.common.dto.AnalyticsReadinessResponse;
import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.enums.DatePreset;
import com.sankhya.analytics.recommendations.entity.BusinessRecommendation;
import com.sankhya.analytics.recommendations.enums.RecommendationCategory;
import com.sankhya.analytics.recommendations.enums.RecommendationSeverity;
import com.sankhya.analytics.recommendations.enums.RecommendationStatus;
import com.sankhya.analytics.recommendations.repository.BusinessRecommendationRepository;
import com.sankhya.analytics.recommendations.service.BusinessRecommendationService;
import com.sankhya.dataset.service.DatasetQueryService;
import com.sankhya.security.CustomUserDetails;
import com.sankhya.workspace.dto.WorkspaceResponse;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.service.WorkspaceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/analytics/recommendations")
public class RecommendationsController {

    private final WorkspaceService workspaceService;
    private final BusinessRecommendationService businessRecommendationService;
    private final BusinessRecommendationRepository businessRecommendationRepository;
    private final DatasetQueryService datasetQueryService;

    public RecommendationsController(WorkspaceService workspaceService,
                                     BusinessRecommendationService businessRecommendationService,
                                     BusinessRecommendationRepository businessRecommendationRepository,
                                     DatasetQueryService datasetQueryService) {
        this.workspaceService = workspaceService;
        this.businessRecommendationService = businessRecommendationService;
        this.businessRecommendationRepository = businessRecommendationRepository;
        this.datasetQueryService = datasetQueryService;
    }

    @GetMapping
    public String showRecommendations(@AuthenticationPrincipal CustomUserDetails userDetails,
                                      HttpSession session,
                                      @RequestParam(required = false) RecommendationCategory category,
                                      @RequestParam(required = false) RecommendationStatus status,
                                      Model model) {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        List<WorkspaceResponse> workspaces = workspaceService.getUserWorkspaces(
                userDetails.getId(),
                activeWorkspace != null ? activeWorkspace.getId() : null
        );

        model.addAttribute("currentUser", userDetails);
        model.addAttribute("activeWorkspace", activeWorkspace);
        model.addAttribute("workspaces", workspaces);
        model.addAttribute("currentPage", "recommendations");
        model.addAttribute("pageTitle", "Actionable Business Recommendations");
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("categories", RecommendationCategory.values());
        model.addAttribute("statuses", RecommendationStatus.values());

        if (activeWorkspace != null) {
            Long wsId = activeWorkspace.getId();
            AnalyticsReadinessResponse readiness = datasetQueryService.getAnalyticsReadiness(wsId);
            model.addAttribute("readiness", readiness);

            List<BusinessRecommendation> recs = businessRecommendationService.getRecommendations(
                    wsId, category, status);
            model.addAttribute("recommendations", recs);

            // Compute summary counters
            long countNew = businessRecommendationRepository.countByWorkspaceIdAndStatus(wsId, RecommendationStatus.NEW);
            long countReviewed = businessRecommendationRepository.countByWorkspaceIdAndStatus(wsId, RecommendationStatus.REVIEWED);
            long countResolved = businessRecommendationRepository.countByWorkspaceIdAndStatus(wsId, RecommendationStatus.RESOLVED);
            long countDismissed = businessRecommendationRepository.countByWorkspaceIdAndStatus(wsId, RecommendationStatus.DISMISSED);

            List<BusinessRecommendation> activeList = businessRecommendationRepository.findActiveByWorkspaceId(wsId);
            long countCritical = activeList.stream()
                    .filter(r -> r.getSeverity() == RecommendationSeverity.CRITICAL)
                    .count();
            long countHigh = activeList.stream()
                    .filter(r -> r.getSeverity() == RecommendationSeverity.HIGH)
                    .count();

            model.addAttribute("totalActive", countNew + countReviewed);
            model.addAttribute("totalCritical", countCritical);
            model.addAttribute("totalHigh", countHigh);
            model.addAttribute("totalReviewed", countReviewed);
            model.addAttribute("totalResolved", countResolved);
            model.addAttribute("totalDismissed", countDismissed);
        }

        return "analytics/recommendations";
    }

    @PostMapping("/generate")
    public String generateRecommendations(@AuthenticationPrincipal CustomUserDetails userDetails,
                                          HttpSession session,
                                          @RequestParam(required = false, defaultValue = "LAST_30_DAYS") DatePreset preset,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                          RedirectAttributes redirectAttributes) {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (activeWorkspace != null) {
            DateRangeFilter filter = DateRangeFilter.of(preset, startDate, endDate);
            List<BusinessRecommendation> generated = businessRecommendationService.generateRecommendations(activeWorkspace.getId(), filter);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Recommendation scan complete. " + generated.size() + " total action items evaluated.");
        }
        return "redirect:/analytics/recommendations";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@AuthenticationPrincipal CustomUserDetails userDetails,
                               HttpSession session,
                               @PathVariable Long id,
                               @RequestParam RecommendationStatus status,
                               RedirectAttributes redirectAttributes) {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (activeWorkspace != null) {
            businessRecommendationService.updateStatus(activeWorkspace.getId(), id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Recommendation status updated to " + status.name() + ".");
        }
        return "redirect:/analytics/recommendations";
    }

    @GetMapping("/api")
    @ResponseBody
    public List<BusinessRecommendation> getRecommendationsApi(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                              HttpSession session,
                                                              @RequestParam(required = false) RecommendationCategory category,
                                                              @RequestParam(required = false) RecommendationStatus status) {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (activeWorkspace == null) return List.of();
        return businessRecommendationService.getRecommendations(activeWorkspace.getId(), category, status);
    }
}
