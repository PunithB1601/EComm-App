package com.example.identity.service.impl;

import com.example.identity.dto.response.*;
import com.example.identity.enums.AccountStatus;
import com.example.identity.exception.ResourceNotFoundException;
import com.example.identity.mapper.UserMapper;
import com.example.identity.repository.*;
import com.example.identity.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
  private final UserRepository users;
  private final RefreshTokenRepository tokens;
  private final UserMapper mapper;

  @Override
  @PreAuthorize("#userId == principal.userId")
  public UserResponse getCurrentUser(Long userId) {
    return find(userId);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN') or #id == principal.userId")
  public UserResponse getUserById(Long id) {
    return find(id);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public UserPageResponse getAllUsers(int page, int size) {
    var result = users.findAll(PageRequest.of(page, size, Sort.by("userId")));
    return new UserPageResponse(
        result.stream().map(mapper::toResponse).toList(),
        page,
        size,
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Override
  @Transactional
  @PreAuthorize("hasRole('ADMIN')")
  public UserResponse updateAccountStatus(Long id, AccountStatus status) {
    var user =
        users.lockById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    user.setAccountStatus(status);
    if (status != AccountStatus.ACTIVE) tokens.revokeAllForUser(id);
    users.flush();
    return mapper.toResponse(user);
  }

  private UserResponse find(Long id) {
    return mapper.toResponse(
        users
            .findWithRolesById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found")));
  }
}
