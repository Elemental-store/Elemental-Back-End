package com.elemental.backend.controller;

import com.elemental.backend.dto.UserProfileResponse;
import com.elemental.backend.dto.UserProfileUpdateRequest;
import com.elemental.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/my/profile")
public class MyUserController {

    private final UserService userService;

    public MyUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserProfileResponse getProfile(Authentication authentication) {
        return userService.getMyProfile(authentication.getName());
    }

    @PutMapping
    public UserProfileResponse updateProfile(
            @Valid @RequestBody UserProfileUpdateRequest request,
            Authentication authentication) {
        return userService.updateMyProfile(authentication.getName(), request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(Authentication authentication) {
        userService.deleteMyAccount(authentication.getName());
    }
}
