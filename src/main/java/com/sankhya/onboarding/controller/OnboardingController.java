package com.sankhya.onboarding.controller;

import com.sankhya.analytics.common.dto.AnalyticsReadinessResponse;
import com.sankhya.dataset.entity.Dataset;
import com.sankhya.dataset.enums.DatasetType;
import com.sankhya.dataset.schema.DatasetSchemaDefinition;
import com.sankhya.dataset.service.DatasetQueryService;
import com.sankhya.dataset.service.DatasetService;
import com.sankhya.security.AuthenticationSuccessHandler;
import com.sankhya.security.CustomUserDetails;
import com.sankhya.user.entity.User;
import com.sankhya.user.repository.UserRepository;
import com.sankhya.workspace.dto.WorkspaceCreateRequest;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.enums.StoreFormat;
import com.sankhya.workspace.repository.WorkspaceRepository;
import com.sankhya.workspace.service.WorkspaceService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/onboarding")
public class OnboardingController {

    private final WorkspaceService workspaceService;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final DatasetService datasetService;
    private final DatasetQueryService datasetQueryService;

    public OnboardingController(WorkspaceService workspaceService,
                                WorkspaceRepository workspaceRepository,
                                UserRepository userRepository,
                                DatasetService datasetService,
                                DatasetQueryService datasetQueryService) {
        this.workspaceService = workspaceService;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.datasetService = datasetService;
        this.datasetQueryService = datasetQueryService;
    }

    // Step 2: Supermarket Setup Wizard
    @GetMapping("/store-setup")
    public String showStoreSetupPage(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     HttpSession session,
                                     Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        WorkspaceCreateRequest request = new WorkspaceCreateRequest();

        if (activeWorkspace != null) {
            request.setName(activeWorkspace.getName());
            request.setStoreFormat(activeWorkspace.getStoreFormat());
            request.setCity(activeWorkspace.getCity());
            request.setState(activeWorkspace.getState());
            request.setCountry(activeWorkspace.getCountry());
            request.setDescription(activeWorkspace.getDescription());
        } else {
            request.setName(userDetails.getFullName() + " Supermarket");
            request.setStoreFormat("Supermarket");
            request.setCountry("India");
        }

        model.addAttribute("workspaceRequest", request);
        model.addAttribute("storeFormats", StoreFormat.values());
        model.addAttribute("activeWorkspace", activeWorkspace);
        return "onboarding/store-setup";
    }

    @PostMapping("/store-setup")
    public String handleStoreSetup(@Valid @ModelAttribute("workspaceRequest") WorkspaceCreateRequest request,
                                   BindingResult bindingResult,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   HttpSession session,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("storeFormats", StoreFormat.values());
            return "onboarding/store-setup";
        }

        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());

        if (activeWorkspace != null) {
            activeWorkspace.setName(request.getName().trim());
            activeWorkspace.setStoreFormat(request.getStoreFormat());
            activeWorkspace.setCity(request.getCity() != null ? request.getCity().trim() : null);
            activeWorkspace.setState(request.getState() != null ? request.getState().trim() : null);
            activeWorkspace.setCountry(request.getCountry() != null ? request.getCountry().trim() : "India");
            activeWorkspace.setDescription(request.getDescription() != null ? request.getDescription().trim() : "");
            activeWorkspace.setOnboardingStatus("DATASET_ONBOARDING");
            workspaceRepository.save(activeWorkspace);
        } else {
            User user = userRepository.findById(userDetails.getId()).orElseThrow();
            Workspace newWorkspace = workspaceService.createWorkspace(request, user);
            newWorkspace.setOnboardingStatus("DATASET_ONBOARDING");
            activeWorkspace = workspaceRepository.save(newWorkspace);
            session.setAttribute(AuthenticationSuccessHandler.ACTIVE_WORKSPACE_ID_SESSION_KEY, activeWorkspace.getId());
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Supermarket profile for \"" + activeWorkspace.getName() + "\" configured successfully. Now ingest your business datasets.");
        return "redirect:/onboarding/datasets";
    }

    // Step 3: Dataset Ingestion Wizard
    @GetMapping("/datasets")
    public String showDatasetOnboardingPage(@AuthenticationPrincipal CustomUserDetails userDetails,
                                            HttpSession session,
                                            Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (activeWorkspace == null) {
            return "redirect:/onboarding/store-setup";
        }

        List<Dataset> existingDatasets = datasetService.getDatasetsByWorkspace(activeWorkspace.getId());
        Map<DatasetType, Dataset> datasetMap = new EnumMap<>(DatasetType.class);
        for (Dataset d : existingDatasets) {
            datasetMap.put(d.getDatasetType(), d);
        }

        // Schema specifications for required/optional fields
        Map<String, List<?>> schemaSpecs = new LinkedHashMap<>();
        for (DatasetType type : DatasetType.values()) {
            schemaSpecs.put(type.name(), DatasetSchemaDefinition.getFields(type));
        }

        model.addAttribute("workspace", activeWorkspace);
        model.addAttribute("datasetMap", datasetMap);
        model.addAttribute("schemaSpecs", schemaSpecs);
        model.addAttribute("datasetTypes", DatasetType.values());

        // Count completed datasets
        long importedCount = existingDatasets.stream()
                .filter(d -> d.getRecordCount() != null && d.getRecordCount() > 0)
                .count();
        model.addAttribute("importedCount", importedCount);

        return "onboarding/datasets";
    }

    // Step 4: Complete Onboarding
    @PostMapping("/complete")
    public String completeOnboarding(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (activeWorkspace != null) {
            AnalyticsReadinessResponse readiness = datasetQueryService.getAnalyticsReadiness(activeWorkspace.getId());
            if (readiness != null && readiness.isOverallReady()) {
                activeWorkspace.setOnboardingStatus("COMPLETED");
                redirectAttributes.addFlashAttribute("successMessage",
                        "Onboarding completed successfully! Welcome to your Sankhya intelligence command center.");
            } else if (readiness != null && (readiness.isSalesReady() || readiness.isProductReady()
                    || readiness.isCustomerReady() || readiness.isInventoryReady())) {
                activeWorkspace.setOnboardingStatus("PARTIALLY_COMPLETED");
                redirectAttributes.addFlashAttribute("infoMessage",
                        "Store setup completed with partial datasets. Upload complete datasets to unlock all analytics modules.");
            } else {
                activeWorkspace.setOnboardingStatus("COMPLETED_WITHOUT_DATA");
                redirectAttributes.addFlashAttribute("infoMessage",
                        "Store profile saved without datasets. Upload your Unified Sales Mart CSV from the Datasets page to activate analytics.");
            }
            workspaceRepository.save(activeWorkspace);
        }
        return "redirect:/dashboard";
    }

    // Step 4b: Skip Onboarding Temporarily
    @PostMapping("/skip")
    public String skipOnboarding(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (activeWorkspace != null) {
            activeWorkspace.setOnboardingStatus("SKIPPED");
            workspaceRepository.save(activeWorkspace);
        }
        redirectAttributes.addFlashAttribute("infoMessage",
                "You have skipped the dataset wizard. You can upload or generate sample supermarket data anytime from the Datasets menu.");
        return "redirect:/dashboard";
    }
}
