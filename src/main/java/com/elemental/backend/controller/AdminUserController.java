package com.elemental.backend.controller;

import com.elemental.backend.dto.AdminUserUpdateRequest;
import com.elemental.backend.dto.UserProfileResponse;
import com.elemental.backend.service.UserService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserProfileResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUserById(id);
    }
    @PutMapping("/{id}")
    public UserProfileResponse updateUser(@PathVariable Long id,
                                          @RequestBody AdminUserUpdateRequest request) {
        return userService.updateUserById(id, request);
    }
}