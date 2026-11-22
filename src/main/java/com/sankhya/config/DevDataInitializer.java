package com.sankhya.config;

import com.sankhya.common.enums.UserRole;
import com.sankhya.common.enums.WorkspaceRole;
import com.sankhya.generator.dto.GeneratorRequest;
import com.sankhya.generator.service.SyntheticDataGeneratorService;
import com.sankhya.user.entity.User;
import com.sankhya.user.repository.UserRepository;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.entity.WorkspaceMembership;
import com.sankhya.workspace.repository.WorkspaceMembershipRepository;
import com.sankhya.workspace.repository.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final SyntheticDataGeneratorService syntheticDataGeneratorService;

    public DevDataInitializer(UserRepository userRepository,
                              WorkspaceRepository workspaceRepository,
                              WorkspaceMembershipRepository membershipRepository,
                              PasswordEncoder passwordEncoder,
                              SyntheticDataGeneratorService syntheticDataGeneratorService) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.syntheticDataGeneratorService = syntheticDataGeneratorService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() == 0) {
            log.info("Seeding initial development supermarket workspace and user...");
            User admin = new User(
                    "Rajesh Sharma",
                    "admin@sankhya.com",
                    passwordEncoder.encode("Password@123"),
                    UserRole.ROLE_ADMIN
            );
            User savedAdmin = userRepository.save(admin);

            // Workspace 1: Primary Supermarket
            Workspace ws1 = new Workspace(
                    "Green Basket Supermarket",
                    "Primary supermarket store with fresh produce, dairy, groceries, and packaged items.",
                    savedAdmin
            );
            Workspace savedWs1 = workspaceRepository.save(ws1);
            membershipRepository.save(new WorkspaceMembership(savedWs1, savedAdmin, WorkspaceRole.OWNER));

            // Workspace 2: Express Branch (Multi-store testing)
            Workspace ws2 = new Workspace(
                    "Metro Fresh Express",
                    "Express convenience branch located at downtown metro station.",
                    savedAdmin
            );
            Workspace savedWs2 = workspaceRepository.save(ws2);
            membershipRepository.save(new WorkspaceMembership(savedWs2, savedAdmin, WorkspaceRole.OWNER));

            // Generate synthetic dataset for primary store (12 months of retail data)
            try {
                GeneratorRequest genReq = new GeneratorRequest(12, "MEDIUM", true);
                syntheticDataGeneratorService.generateSupermarketData(savedWs1, genReq);
                log.info("Synthetic supermarket dataset successfully generated for {}", savedWs1.getName());
            } catch (Exception e) {
                log.warn("Synthetic data generation failed during dev seed: {}", e.getMessage());
            }

            log.info("================================================================================");
            log.info("SANKHYA DEV ENVIRONMENT READY");
            log.info("Local URL     : http://localhost:8080/");
            log.info("Email         : admin@sankhya.com");
            log.info("Password      : Password@123");
            log.info("Active Stores : Green Basket Supermarket & Metro Fresh Express");
            log.info("================================================================================");
        }
    }
}
