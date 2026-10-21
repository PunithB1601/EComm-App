package com.example.identity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.identity.entity.User;
import com.example.identity.enums.AccountStatus;
import com.example.identity.exception.ResourceNotFoundException;
import com.example.identity.mapper.UserMapper;
import com.example.identity.repository.*;
import com.example.identity.service.impl.UserServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
  @Mock UserRepository users;
  @Mock RefreshTokenRepository tokens;
  @Mock UserMapper mapper;
  @InjectMocks UserServiceImpl service;

  @Test
  void disablingAccountRevokesAllSessions() {
    User user = new User();
    user.setUserId(1L);
    when(users.lockById(1L)).thenReturn(Optional.of(user));
    service.updateAccountStatus(1L, AccountStatus.INACTIVE);
    assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.INACTIVE);
    verify(tokens).revokeAllForUser(1L);
    verify(users).flush();
  }

  @Test
  void missingAccountProducesNotFound() {
    when(users.findWithRolesById(42L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.getUserById(42L))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
