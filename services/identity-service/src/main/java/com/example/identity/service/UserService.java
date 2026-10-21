package com.example.identity.service;

import com.example.identity.dto.response.*;
import com.example.identity.enums.AccountStatus;

public interface UserService {
  UserResponse getCurrentUser(Long userId);

  UserResponse getUserById(Long id);

  UserPageResponse getAllUsers(int page, int size);

  UserResponse updateAccountStatus(Long id, AccountStatus status);
}
