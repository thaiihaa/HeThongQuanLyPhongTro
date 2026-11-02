package com.quanlyphongtro.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Component
public class AccountBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AccountBootstrapRunner.class);

    private final BootstrapService bootstrapService;
    private final boolean adminEnabled;
    private final String adminUsername;
    private final String adminEmail;
    private final String adminPhone;
    private final String adminPassword;
    private final String adminFullName;
    private final boolean tenantEnabled;
    private final String tenantUsername;
    private final String tenantEmail;
    private final String tenantPhone;
    private final String tenantPassword;
    private final String tenantFullName;
    private final boolean demoUsersEnabled;
    private final String demoAdminPassword;
    private final String demoTenantPassword;
    private final Environment environment;

    public AccountBootstrapRunner(
            BootstrapService bootstrapService,
            @Value("${app.bootstrap.admin.enabled:false}") boolean adminEnabled,
            @Value("${app.bootstrap.admin.username:}") String adminUsername,
            @Value("${app.bootstrap.admin.email:}") String adminEmail,
            @Value("${app.bootstrap.admin.phone:}") String adminPhone,
            @Value("${app.bootstrap.admin.password:}") String adminPassword,
            @Value("${app.bootstrap.admin.full-name:}") String adminFullName,
            @Value("${app.bootstrap.tenant.enabled:false}") boolean tenantEnabled,
            @Value("${app.bootstrap.tenant.username:}") String tenantUsername,
            @Value("${app.bootstrap.tenant.email:}") String tenantEmail,
            @Value("${app.bootstrap.tenant.phone:}") String tenantPhone,
            @Value("${app.bootstrap.tenant.password:}") String tenantPassword,
            @Value("${app.bootstrap.tenant.full-name:}") String tenantFullName,
            @Value("${app.bootstrap.demo-users.enabled:false}") boolean demoUsersEnabled,
            @Value("${app.bootstrap.demo-users.admin-password:}") String demoAdminPassword,
            @Value("${app.bootstrap.demo-users.tenant-password:}") String demoTenantPassword,
            Environment environment
    ) {
        this.bootstrapService = bootstrapService;
        this.adminEnabled = adminEnabled;
        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.adminPhone = adminPhone;
        this.adminPassword = adminPassword;
        this.adminFullName = adminFullName;
        this.tenantEnabled = tenantEnabled;
        this.tenantUsername = tenantUsername;
        this.tenantEmail = tenantEmail;
        this.tenantPhone = tenantPhone;
        this.tenantPassword = tenantPassword;
        this.tenantFullName = tenantFullName;
        this.demoUsersEnabled = demoUsersEnabled;
        this.demoAdminPassword = demoAdminPassword;
        this.demoTenantPassword = demoTenantPassword;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (demoUsersEnabled) {
            rejectProductionProfile();
            requireLocalProfile();
            requireDemoPassword(demoAdminPassword, "Admin");
            requireDemoPassword(demoTenantPassword, "tenant");
            BootstrapService.DemoBootstrapResult result =
                    bootstrapService.createDemoUsersIfAbsent(demoAdminPassword, demoTenantPassword);
            logBootstrapResult("Admin", result.adminCreated());
            logBootstrapResult("tenant", result.tenantCreated());
        }

        if (adminEnabled) {
            requireCredentials(adminUsername, adminPassword, adminFullName, "Admin");
            boolean created = bootstrapService.createAdminIfAbsent(
                    adminUsername, adminEmail, adminPhone, adminPassword, adminFullName
            );
            log.info(created ? "Bootstrap Admin account created." : "Bootstrap Admin username already exists; left unchanged.");
        }

        if (tenantEnabled) {
            requireCredentials(tenantUsername, tenantPassword, tenantFullName, "tenant");
            boolean created = bootstrapService.createTenantIfAbsent(
                    tenantUsername,
                    tenantEmail,
                    tenantPhone,
                    tenantPassword,
                    tenantFullName,
                    null,
                    null,
                    null
            );
            log.info(created ? "Bootstrap tenant account created." : "Bootstrap tenant username already exists; left unchanged.");
        }
    }

    private void requireCredentials(String username, String password, String fullName, String accountType) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password) || !StringUtils.hasText(fullName)) {
            throw new IllegalStateException("Bootstrap " + accountType + " is enabled but its username/password/full-name is missing.");
        }
        if (password.length() < 12
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Bootstrap " + accountType + " password must be 12-72 UTF-8 bytes.");
        }
    }

    private void requireDemoPassword(String password, String accountType) {
        if (!StringUtils.hasText(password)
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 6
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Demo bootstrap " + accountType + " password must be 6-72 UTF-8 bytes.");
        }
    }

    private void rejectProductionProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                throw new IllegalStateException("Demo user bootstrap is not allowed with a production profile.");
            }
        }
    }

    private void requireLocalProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if ("local".equalsIgnoreCase(profile)
                    || "dev".equalsIgnoreCase(profile)
                    || "development".equalsIgnoreCase(profile)) {
                return;
            }
        }
        throw new IllegalStateException(
                "Demo user bootstrap requires an active local, dev, or development profile.");
    }

    private void logBootstrapResult(String accountType, boolean created) {
        log.info(created
                ? "Demo bootstrap {} account created; existing accounts were not modified."
                : "Demo bootstrap {} username already exists; account and role were left unchanged.",
                accountType);
    }
}
