package com.rayk.health.workbench.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.security.service.AuthService;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.security.service.UserAccount;
import com.rayk.health.security.service.UserCatalog;
import com.rayk.health.security.service.WorkbenchOption;
import com.rayk.health.workbench.dto.CurrentWorkbenchData;
import com.rayk.health.workbench.dto.SwitchWorkbenchRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workbenches")
public class WorkbenchController {
    private final AuthService authService;
    private final UserCatalog catalog;

    public WorkbenchController(AuthService authService, UserCatalog catalog) {
        this.authService = authService;
        this.catalog = catalog;
    }

    @GetMapping
    public ApiResponse<List<WorkbenchOption>> list() {
        CurrentPrincipal current = CurrentUser.require();
        UserAccount account = catalog.findByUserId(current.userId());
        return ApiResponse.success(account == null ? List.of() : account.workbenches());
    }

    @GetMapping("/current")
    public ApiResponse<CurrentWorkbenchData> current() {
        return ApiResponse.success(new CurrentWorkbenchData(CurrentUser.require().workbench()));
    }

    @PostMapping("/switch")
    public ApiResponse<CurrentWorkbenchData> switchWorkbench(
            @Valid @RequestBody SwitchWorkbenchRequest request) {
        return ApiResponse.success(new CurrentWorkbenchData(authService.switchWorkbench(request.code())));
    }

    @GetMapping("/current/functions")
    public ApiResponse<List<String>> functions() {
        return ApiResponse.success(CurrentUser.require().permissions());
    }
}

