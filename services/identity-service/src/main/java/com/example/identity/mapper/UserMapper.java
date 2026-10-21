package com.example.identity.mapper;

import com.example.identity.dto.response.UserResponse;
import com.example.identity.entity.User;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
  public UserResponse toResponse(User u) {
    return new UserResponse(
        u.getUserId(),
        u.getFullName(),
        u.getEmail(),
        u.getAccountStatus(),
        u.getRoles().stream().map(r -> r.getRoleName().name()).collect(Collectors.toSet()),
        u.getCreatedAt(),
        u.getUpdatedAt());
  }
}
