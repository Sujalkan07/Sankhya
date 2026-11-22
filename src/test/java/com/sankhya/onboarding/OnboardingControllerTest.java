package com.sankhya.onboarding;

import com.sankhya.analytics.common.dto.AnalyticsReadinessResponse;
import com.sankhya.dataset.entity.Dataset;
import com.sankhya.dataset.enums.DatasetType;
import com.sankhya.dataset.service.DatasetQueryService;
import com.sankhya.dataset.service.DatasetService;
import com.sankhya.onboarding.controller.OnboardingController;
import com.sankhya.security.AuthenticationSuccessHandler;
import com.sankhya.security.CustomUserDetails;
import com.sankhya.user.entity.User;
import com.sankhya.user.repository.UserRepository;
import com.sankhya.workspace.dto.WorkspaceCreateRequest;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.enums.StoreFormat;
import com.sankhya.workspace.repository.WorkspaceRepository;
import com.sankhya.workspace.service.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OnboardingController Unit Tests")
class OnboardingControllerTest {

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DatasetService datasetService;

    @Mock
    private DatasetQueryService datasetQueryService;

    private OnboardingController onboardingController;

    private User testUser;
    private CustomUserDetails testUserDetails;
    private Workspace testWorkspace;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        onboardingController = new OnboardingController(
                workspaceService,
                workspaceRepository,
                userRepository,
                datasetService,
                datasetQueryService
        );

        testUser = new User();
        testUser.setId(10L);
        testUser.setEmail("retailer@sankhya.com");
        testUser.setFullName("Retailer User");

        testUserDetails = new CustomUserDetails(testUser);

        testWorkspace = new Workspace();
        testWorkspace.setId(100L);
        testWorkspace.setName("Metro Supermarket");
        testWorkspace.setStoreFormat("Supermarket");
        testWorkspace.setCity("Mumbai");
        testWorkspace.setState("Maharashtra");
        testWorkspace.setCountry("India");
        testWorkspace.setOnboardingStatus("STORE_SETUP");
        testWorkspace.setOwner(testUser);

        session = new MockHttpSession();
    }

    @Test
    @DisplayName("showStoreSetupPage - redirects to /login when user is not authenticated")
    void testShowStoreSetupPage_Unauthenticated() {
        Model model = new ConcurrentModel();
        String view = onboardingController.showStoreSetupPage(null, session, model);
        assertEquals("redirect:/login", view);
    }

    @Test
    @DisplayName("showStoreSetupPage - renders store-setup template with workspace data")
    void testShowStoreSetupPage_Authenticated() {
        when(workspaceService.getActiveWorkspace(session, 10L)).thenReturn(testWorkspace);

        Model model = new ConcurrentModel();
        String view = onboardingController.showStoreSetupPage(testUserDetails, session, model);

        assertEquals("onboarding/store-setup", view);
        assertTrue(model.containsAttribute("workspaceRequest"));
        assertTrue(model.containsAttribute("storeFormats"));
        assertEquals(testWorkspace, model.getAttribute("activeWorkspace"));
    }

    @Test
    @DisplayName("handleStoreSetup - validation error returns to form")
    void testHandleStoreSetup_ValidationError() {
        WorkspaceCreateRequest request = new WorkspaceCreateRequest();
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "workspaceRequest");
        bindingResult.rejectValue("name", "NotBlank", "Name is required");

        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = onboardingController.handleStoreSetup(request, bindingResult, testUserDetails, session, model, redirectAttributes);

        assertEquals("onboarding/store-setup", view);
        assertTrue(model.containsAttribute("storeFormats"));
    }

    @Test
    @DisplayName("handleStoreSetup - updates workspace attributes and advances to DATASET_ONBOARDING")
    void testHandleStoreSetup_Success() {
        when(workspaceService.getActiveWorkspace(session, 10L)).thenReturn(testWorkspace);
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(testWorkspace);

        WorkspaceCreateRequest request = new WorkspaceCreateRequest();
        request.setName("Metro Supermarket Bandra");
        request.setStoreFormat("Hypermarket");
        request.setCity("Mumbai");
        request.setState("Maharashtra");
        request.setCountry("India");

        BindingResult bindingResult = new BeanPropertyBindingResult(request, "workspaceRequest");
        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = onboardingController.handleStoreSetup(request, bindingResult, testUserDetails, session, model, redirectAttributes);

        assertEquals("redirect:/onboarding/datasets", view);
        assertEquals("DATASET_ONBOARDING", testWorkspace.getOnboardingStatus());
        assertEquals("Metro Supermarket Bandra", testWorkspace.getName());
        assertEquals("Hypermarket", testWorkspace.getStoreFormat());
        verify(workspaceRepository).save(testWorkspace);
    }

    @Test
    @DisplayName("showDatasetOnboardingPage - loads dataset cards and readiness")
    void testShowDatasetOnboardingPage_Success() {
        when(workspaceService.getActiveWorkspace(session, 10L)).thenReturn(testWorkspace);

        Dataset salesDataset = new Dataset();
        salesDataset.setDatasetType(DatasetType.SALES_TRANSACTIONS);
        salesDataset.setRecordCount(1250L);

        when(datasetService.getDatasetsByWorkspace(100L)).thenReturn(List.of(salesDataset));

        Model model = new ConcurrentModel();
        String view = onboardingController.showDatasetOnboardingPage(testUserDetails, session, model);

        assertEquals("onboarding/datasets", view);
        assertTrue(model.containsAttribute("workspace"));
        assertTrue(model.containsAttribute("datasetMap"));
        assertTrue(model.containsAttribute("schemaSpecs"));
        assertTrue(model.containsAttribute("datasetTypes"));
        assertEquals(1L, model.getAttribute("importedCount"));

        @SuppressWarnings("unchecked")
        Map<DatasetType, Dataset> map = (Map<DatasetType, Dataset>) model.getAttribute("datasetMap");
        assertNotNull(map);
        assertTrue(map.containsKey(DatasetType.SALES_TRANSACTIONS));
        assertEquals(1250L, map.get(DatasetType.SALES_TRANSACTIONS).getRecordCount());
    }

    @Test
    @DisplayName("completeOnboarding - updates status to COMPLETED when data is ready and redirects to /dashboard")
    void testCompleteOnboarding_Success() {
        when(workspaceService.getActiveWorkspace(session, 10L)).thenReturn(testWorkspace);
        AnalyticsReadinessResponse ready = new AnalyticsReadinessResponse();
        ready.setOverallReady(true);
        ready.setSalesReady(true);
        ready.setProductReady(true);
        when(datasetQueryService.getAnalyticsReadiness(100L)).thenReturn(ready);
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(testWorkspace);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = onboardingController.completeOnboarding(testUserDetails, session, redirectAttributes);

        assertEquals("redirect:/dashboard", view);
        assertEquals("COMPLETED", testWorkspace.getOnboardingStatus());
        verify(workspaceRepository).save(testWorkspace);
    }

    @Test
    @DisplayName("completeOnboarding - updates status to COMPLETED_WITHOUT_DATA when no data imported")
    void testCompleteOnboarding_WithoutData() {
        when(workspaceService.getActiveWorkspace(session, 10L)).thenReturn(testWorkspace);
        when(datasetQueryService.getAnalyticsReadiness(100L)).thenReturn(new AnalyticsReadinessResponse());
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(testWorkspace);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = onboardingController.completeOnboarding(testUserDetails, session, redirectAttributes);

        assertEquals("redirect:/dashboard", view);
        assertEquals("COMPLETED_WITHOUT_DATA", testWorkspace.getOnboardingStatus());
        verify(workspaceRepository).save(testWorkspace);
    }

    @Test
    @DisplayName("skipOnboarding - sets status to SKIPPED and redirects to /dashboard")
    void testSkipOnboarding_Success() {
        when(workspaceService.getActiveWorkspace(session, 10L)).thenReturn(testWorkspace);
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(testWorkspace);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = onboardingController.skipOnboarding(testUserDetails, session, redirectAttributes);

        assertEquals("redirect:/dashboard", view);
        assertEquals("SKIPPED", testWorkspace.getOnboardingStatus());
        verify(workspaceRepository).save(testWorkspace);
    }
}
