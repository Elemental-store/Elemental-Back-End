package com.elemental.backend.service;

import com.elemental.backend.dto.AdminUserUpdateRequest;
import com.elemental.backend.dto.AuthResponse;
import com.elemental.backend.dto.EmailChangeRequest;
import com.elemental.backend.dto.PasswordChangeRequest;
import com.elemental.backend.dto.UserProfileResponse;
import com.elemental.backend.dto.UserProfileUpdateRequest;

import java.util.List;

public interface UserService {
    UserProfileResponse getMyProfile(String email);
    UserProfileResponse updateMyProfile(String email, UserProfileUpdateRequest request);
    AuthResponse changeMyEmail(String email, EmailChangeRequest request);
    void changeMyPassword(String email, PasswordChangeRequest request);
    void deleteMyAccount(String email);

    List<UserProfileResponse> getAllUsers();
    void deleteUserById(Long id);

    UserProfileResponse updateUserById(Long id, AdminUserUpdateRequest request);
}
