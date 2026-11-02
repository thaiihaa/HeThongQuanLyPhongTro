package com.quanlyphongtro.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AccountBootstrapRunnerTest {

    @Test
    void demoBootstrapIsDisabledByDefault() {
        BootstrapService bootstrapService = mock(BootstrapService.class);
        AccountBootstrapRunner runner = runner(bootstrapService, false, "123456", "123456", new MockEnvironment());

        runner.run(null);

        verifyNoInteractions(bootstrapService);
    }

    @Test
    void demoBootstrapRejectsProductionProfile() {
        BootstrapService bootstrapService = mock(BootstrapService.class);
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "production");
        environment.setActiveProfiles("dev", "production");
        AccountBootstrapRunner runner = runner(bootstrapService, true, "123456", "123456", environment);

        assertThrows(IllegalStateException.class, () -> runner.run(null));
        verifyNoInteractions(bootstrapService);
    }

    @Test
    void demoBootstrapRequiresExplicitLocalOrDevelopmentProfile() {
        BootstrapService bootstrapService = mock(BootstrapService.class);
        AccountBootstrapRunner runner = runner(bootstrapService, true, "123456", "123456", new MockEnvironment());

        assertThrows(IllegalStateException.class, () -> runner.run(null));
        verifyNoInteractions(bootstrapService);
    }

    @Test
    void demoBootstrapUsesConfiguredPasswordsForFixedDemoUsers() {
        BootstrapService bootstrapService = mock(BootstrapService.class);
        when(bootstrapService.createDemoUsersIfAbsent("123456", "123456"))
                .thenReturn(new BootstrapService.DemoBootstrapResult(true, true));
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "dev");
        environment.setActiveProfiles("dev");
        AccountBootstrapRunner runner = runner(bootstrapService, true, "123456", "123456", environment);

        runner.run(null);

        verify(bootstrapService).createDemoUsersIfAbsent("123456", "123456");
    }

    private AccountBootstrapRunner runner(
            BootstrapService service,
            boolean demoEnabled,
            String adminPassword,
            String tenantPassword,
            MockEnvironment environment
    ) {
        return new AccountBootstrapRunner(
                service,
                false, "", "", "", "", "",
                false, "", "", "", "", "",
                demoEnabled, adminPassword, tenantPassword, environment
        );
    }
}
