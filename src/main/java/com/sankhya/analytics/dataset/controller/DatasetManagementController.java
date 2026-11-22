package com.sankhya.analytics.dataset.controller;

import com.sankhya.customers.service.CustomerDataService;
import com.sankhya.dataset.entity.Dataset;
import com.sankhya.dataset.entity.DatasetImport;
import com.sankhya.dataset.entity.ImportError;
import com.sankhya.dataset.enums.DatasetType;
import com.sankhya.dataset.enums.ImportStatus;
import com.sankhya.dataset.parser.ParsedFileData;
import com.sankhya.dataset.schema.CanonicalField;
import com.sankhya.dataset.schema.DatasetSchemaDefinition;
import com.sankhya.dataset.service.DatasetImportService;
import com.sankhya.dataset.service.DatasetQueryService;
import com.sankhya.dataset.service.DatasetService;
import com.sankhya.generator.dto.GeneratorRequest;
import com.sankhya.generator.dto.GeneratorSummaryResponse;
import com.sankhya.generator.service.SyntheticDataGeneratorService;
import com.sankhya.inventory.service.InventoryDataService;
import com.sankhya.products.service.ProductDataService;
import com.sankhya.sales.service.SalesDataService;
import com.sankhya.security.CustomUserDetails;
import com.sankhya.workspace.dto.WorkspaceResponse;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.service.WorkspaceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.*;

@Controller
@RequestMapping("/datasets")
public class DatasetManagementController {

    private final WorkspaceService workspaceService;
    private final DatasetService datasetService;
    private final DatasetImportService importService;
    private final DatasetQueryService queryService;
    private final SyntheticDataGeneratorService generatorService;
    private final ProductDataService productDataService;
    private final CustomerDataService customerDataService;
    private final SalesDataService salesDataService;
    private final InventoryDataService inventoryDataService;

    public DatasetManagementController(WorkspaceService workspaceService,
                                       DatasetService datasetService,
                                       DatasetImportService importService,
                                       DatasetQueryService queryService,
                                       SyntheticDataGeneratorService generatorService,
                                       ProductDataService productDataService,
                                       CustomerDataService customerDataService,
                                       SalesDataService salesDataService,
                                       InventoryDataService inventoryDataService) {
        this.workspaceService = workspaceService;
        this.datasetService = datasetService;
        this.importService = importService;
        this.queryService = queryService;
        this.generatorService = generatorService;
        this.productDataService = productDataService;
        this.customerDataService = customerDataService;
        this.salesDataService = salesDataService;
        this.inventoryDataService = inventoryDataService;
    }

    private Workspace populateBaseModel(CustomUserDetails userDetails, HttpSession session, Model model) {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        List<WorkspaceResponse> workspaces = workspaceService.getUserWorkspaces(
                userDetails.getId(),
                activeWorkspace != null ? activeWorkspace.getId() : null
        );

        model.addAttribute("currentUser", userDetails);
        model.addAttribute("activeWorkspace", activeWorkspace);
        model.addAttribute("workspaces", workspaces);
        model.addAttribute("activeModule", "datasets");
        model.addAttribute("activeSubPage", "overview");
        model.addAttribute("currentPage", "dataset-management");
        return activeWorkspace;
    }

    // 1. Overview Dashboard
    @GetMapping
    public String showDatasetOverview(@AuthenticationPrincipal CustomUserDetails userDetails,
                                      HttpSession session,
                                      Model model) {
        Workspace workspace = populateBaseModel(userDetails, session, model);
        if (workspace == null) {
            return "redirect:/workspaces";
        }

        List<Dataset> datasets = datasetService.getDatasetsByWorkspace(workspace.getId());
        List<DatasetImport> recentImports = importService.getRecentImports(workspace.getId());
        Map<String, Object> healthSummary = queryService.getDatasetHealthSummary(workspace.getId());

        model.addAttribute("datasets", datasets);
        model.addAttribute("recentImports", recentImports);
        model.addAttribute("healthSummary", healthSummary);
        model.addAttribute("datasetTypes", DatasetType.values());

        // Domain record counts
        model.addAttribute("productCount", productDataService.getTotalProductsCount(workspace.getId()));
        model.addAttribute("customerCount", customerDataService.getTotalCustomersCount(workspace.getId()));
        model.addAttribute("salesCount", salesDataService.getTotalLineItemsCount(workspace.getId()));
        model.addAttribute("inventoryCount", inventoryDataService.getTotalSnapshotsCount(workspace.getId()));

        return "dataset/overview";
    }

    // 2. Upload Page
    @GetMapping("/upload")
    public String showUploadPage(@RequestParam(name = "type", required = false) DatasetType datasetType,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 HttpSession session,
                                 Model model) {
        Workspace workspace = populateBaseModel(userDetails, session, model);
        if (workspace == null) return "redirect:/workspaces";

        model.addAttribute("selectedType", datasetType != null ? datasetType : DatasetType.UNIFIED_MART);
        model.addAttribute("datasetTypes", DatasetType.values());
        return "dataset/upload";
    }

    // Handle Upload
    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("datasetType") DatasetType datasetType,
                                   @RequestParam("file") MultipartFile file,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        Workspace workspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (workspace == null) return "redirect:/workspaces";

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a valid CSV or Excel file to upload.");
            return "redirect:/datasets/upload?type=" + datasetType.name();
        }

        try {
            DatasetImport stagedImport = importService.stageUpload(workspace, datasetType, file);
            return "redirect:/datasets/imports/" + stagedImport.getId() + "/mapping";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Upload failed: " + e.getMessage());
            return "redirect:/datasets/upload?type=" + datasetType.name();
        }
    }

    // 3. Column Mapping Page
    @GetMapping("/imports/{importId}/mapping")
    public String showMappingPage(@PathVariable("importId") Long importId,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  HttpSession session,
                                  Model model) {
        Workspace workspace = populateBaseModel(userDetails, session, model);
        if (workspace == null) return "redirect:/workspaces";

        DatasetImport datasetImport = importService.getImport(importId, workspace.getId());
        Map<String, String> currentMapping = importService.getColumnMapping(datasetImport);
        List<CanonicalField> canonicalFields = DatasetSchemaDefinition.getFields(datasetImport.getDatasetType());

        try {
            ParsedFileData previewData = importService.getPreviewData(datasetImport, 5);
            model.addAttribute("fileHeaders", previewData.getHeaders());
            model.addAttribute("previewRows", previewData.getRows());
        } catch (IOException e) {
            model.addAttribute("fileHeaders", Collections.emptyList());
            model.addAttribute("previewRows", Collections.emptyList());
        }

        model.addAttribute("datasetImport", datasetImport);
        model.addAttribute("currentMapping", currentMapping);
        model.addAttribute("canonicalFields", canonicalFields);
        return "dataset/mapping";
    }

    // Save Mapping & Proceed to Preview
    @PostMapping("/imports/{importId}/mapping")
    public String saveMapping(@PathVariable("importId") Long importId,
                              @RequestParam Map<String, String> allParams,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Workspace workspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (workspace == null) return "redirect:/workspaces";

        DatasetImport datasetImport = importService.getImport(importId, workspace.getId());
        List<CanonicalField> canonicalFields = DatasetSchemaDefinition.getFields(datasetImport.getDatasetType());

        Map<String, String> mapping = new HashMap<>();
        for (CanonicalField field : canonicalFields) {
            String selectedHeader = allParams.get("col_" + field.canonicalName());
            if (selectedHeader != null && !selectedHeader.isBlank()) {
                mapping.put(field.canonicalName(), selectedHeader.trim());
            }
        }

        importService.saveColumnMapping(importId, workspace.getId(), mapping);
        redirectAttributes.addFlashAttribute("successMessage", "Column mapping saved successfully.");
        return "redirect:/datasets/imports/" + importId + "/preview";
    }

    // 4. Preview Page
    @GetMapping("/imports/{importId}/preview")
    public String showPreviewPage(@PathVariable("importId") Long importId,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  HttpSession session,
                                  Model model) {
        Workspace workspace = populateBaseModel(userDetails, session, model);
        if (workspace == null) return "redirect:/workspaces";

        DatasetImport datasetImport = importService.getImport(importId, workspace.getId());
        Map<String, String> mapping = importService.getColumnMapping(datasetImport);
        List<CanonicalField> canonicalFields = DatasetSchemaDefinition.getFields(datasetImport.getDatasetType());

        try {
            ParsedFileData previewData = importService.getPreviewData(datasetImport, 15);
            model.addAttribute("previewData", previewData);
        } catch (IOException e) {
            model.addAttribute("errorMessage", "Could not load preview data: " + e.getMessage());
        }

        model.addAttribute("datasetImport", datasetImport);
        model.addAttribute("mapping", mapping);
        model.addAttribute("canonicalFields", canonicalFields);
        return "dataset/preview";
    }

    // 5. Trigger Validation
    @PostMapping("/imports/{importId}/validate")
    public String triggerValidation(@PathVariable("importId") Long importId,
                                    @AuthenticationPrincipal CustomUserDetails userDetails,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        Workspace workspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (workspace == null) return "redirect:/workspaces";

        try {
            importService.validateImport(importId, workspace.getId());
            return "redirect:/datasets/imports/" + importId + "/quality-report";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation failed: " + e.getMessage());
            return "redirect:/datasets/imports/" + importId + "/preview";
        }
    }

    // 6. Quality Report Page
    @GetMapping("/imports/{importId}/quality-report")
    public String showQualityReport(@PathVariable("importId") Long importId,
                                    @AuthenticationPrincipal CustomUserDetails userDetails,
                                    HttpSession session,
                                    Model model) {
        Workspace workspace = populateBaseModel(userDetails, session, model);
        if (workspace == null) return "redirect:/workspaces";

        DatasetImport datasetImport = importService.getImport(importId, workspace.getId());
        List<ImportError> errors = importService.getImportErrors(importId, workspace.getId());

        model.addAttribute("datasetImport", datasetImport);
        model.addAttribute("errors", errors);
        return "dataset/quality_report";
    }

    // 7. Confirm Import
    @PostMapping("/imports/{importId}/confirm")
    public String confirmImport(@PathVariable("importId") Long importId,
                                @RequestParam(name = "skipInvalidRows", defaultValue = "true") boolean skipInvalidRows,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Workspace workspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (workspace == null) return "redirect:/workspaces";

        try {
            importService.confirmImport(importId, workspace.getId(), skipInvalidRows);
            redirectAttributes.addFlashAttribute("successMessage", "Data ingestion completed successfully!");
            return "redirect:/datasets/imports/" + importId + "/status";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to confirm import: " + e.getMessage());
            return "redirect:/datasets/imports/" + importId + "/quality-report";
        }
    }

    // 8. Import Status / Completed Summary Page
    @GetMapping("/imports/{importId}/status")
    public String showImportStatus(@PathVariable("importId") Long importId,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   HttpSession session,
                                   Model model) {
        Workspace workspace = populateBaseModel(userDetails, session, model);
        if (workspace == null) return "redirect:/workspaces";

        DatasetImport datasetImport = importService.getImport(importId, workspace.getId());
        model.addAttribute("datasetImport", datasetImport);
        return "dataset/import_status";
    }

    // 9. Cancel Import
    @PostMapping("/imports/{importId}/cancel")
    public String cancelImport(@PathVariable("importId") Long importId,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Workspace workspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (workspace == null) return "redirect:/workspaces";

        importService.cancelImport(importId, workspace.getId());
        redirectAttributes.addFlashAttribute("infoMessage", "Import job was cancelled.");
        return "redirect:/datasets";
    }

    // 10. Download Error CSV
    @GetMapping("/imports/{importId}/errors/download")
    public ResponseEntity<byte[]> downloadErrorCsv(@PathVariable("importId") Long importId,
                                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                                   HttpSession session) {
        Workspace workspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (workspace == null) return ResponseEntity.notFound().build();

        byte[] csvData = importService.exportErrorsCsv(importId, workspace.getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"import_errors_" + importId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    // 11. Download Canonical Template CSV
    @GetMapping("/templates/{datasetType}")
    public ResponseEntity<byte[]> downloadTemplateCsv(@PathVariable("datasetType") DatasetType datasetType) {
        byte[] templateBytes = datasetService.generateTemplateCsv(datasetType);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + datasetType.name().toLowerCase() + "_template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(templateBytes);
    }

    // 12. Ingestion History Page
    @GetMapping("/history")
    public String showHistoryPage(@AuthenticationPrincipal CustomUserDetails userDetails,
                                  HttpSession session,
                                  Model model) {
        Workspace workspace = populateBaseModel(userDetails, session, model);
        if (workspace == null) return "redirect:/workspaces";

        List<DatasetImport> imports = importService.getImportHistory(workspace.getId());
        model.addAttribute("imports", imports);
        return "dataset/history";
    }

    // 13. Dataset Details Page
    @GetMapping("/{id}")
    public String showDatasetDetails(@PathVariable("id") Long id,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     HttpSession session,
                                     Model model) {
        Workspace workspace = populateBaseModel(userDetails, session, model);
        if (workspace == null) return "redirect:/workspaces";

        Dataset dataset = datasetService.getDatasetById(id, workspace.getId())
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + id));

        model.addAttribute("dataset", dataset);
        model.addAttribute("schemaFields", DatasetSchemaDefinition.getFields(dataset.getDatasetType()));

        // Sample domain data
        switch (dataset.getDatasetType()) {
            case PRODUCT_CATALOG -> model.addAttribute("sampleData", productDataService.getProducts(workspace.getId(), 0, 15).getContent());
            case CUSTOMER_REGISTRY -> model.addAttribute("sampleData", customerDataService.getCustomers(workspace.getId(), 0, 15).getContent());
            case SALES_TRANSACTIONS -> model.addAttribute("sampleData", salesDataService.getTransactions(workspace.getId(), 0, 15).getContent());
            case INVENTORY_STOCK -> model.addAttribute("sampleData", inventoryDataService.getSnapshots(workspace.getId(), 0, 15).getContent());
        }

        return "dataset/details";
    }

    // 14. Synthetic Data Generator Page (Aliased to both /sample-generator and /generate for robustness)
    @GetMapping({"/sample-generator", "/generate"})
    public String showGeneratorPage(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    HttpSession session,
                                    Model model) {
        Workspace workspace = populateBaseModel(userDetails, session, model);
        if (workspace == null) return "redirect:/workspaces";

        Map<String, Object> healthSummary = queryService.getDatasetHealthSummary(workspace.getId());
        model.addAttribute("healthSummary", healthSummary);
        model.addAttribute("generatorRequest", new GeneratorRequest(24, "MEDIUM", false));
        return "dataset/generator";
    }

    // Execute Synthetic Data Generation
    @PostMapping("/sample-generator/generate")
    public String generateSampleData(@ModelAttribute GeneratorRequest request,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        Workspace workspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (workspace == null) return "redirect:/workspaces";

        try {
            GeneratorSummaryResponse response = generatorService.generateSupermarketData(workspace, request);
            redirectAttributes.addFlashAttribute("successMessage", response.getMessage());
            redirectAttributes.addFlashAttribute("generatorResponse", response);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Data generation failed: " + e.getMessage());
        }

        return "redirect:/datasets";
    }
}
