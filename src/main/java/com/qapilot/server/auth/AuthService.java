package com.qapilot.server.auth;

import com.qapilot.server.auth.domain.UserAccount;
import com.qapilot.server.auth.dto.AccessTokenResponse;
import com.qapilot.server.auth.dto.AuthResponse;
import com.qapilot.server.auth.dto.LoginRequest;
import com.qapilot.server.auth.dto.LogoutRequest;
import com.qapilot.server.auth.dto.RefreshRequest;
import com.qapilot.server.auth.dto.RegisterRequest;
import com.qapilot.server.auth.dto.UserResponse;
import com.qapilot.server.auth.persistence.UserEntity;
import com.qapilot.server.auth.persistence.UserRepository;
import com.qapilot.server.auth.security.JwtClaims;
import com.qapilot.server.auth.security.JwtTokenProvider;
import com.qapilot.server.auth.store.RevokedRefreshTokenStore;
import com.qapilot.server.auth.store.UserFileStore;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.organization.OrganizationService;
import com.qapilot.server.service.ServiceDomainService;
import com.qapilot.server.service.domain.ServiceMember;
import com.qapilot.server.service.domain.QapilotService;
import com.qapilot.server.service.store.MemberFileStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 인증/회원가입 유스케이스.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserFileStore userFileStore;
    private final MemberFileStore memberFileStore;
    private final ServiceDomainService serviceDomainService;
    private final RevokedRefreshTokenStore revokedRefreshTokenStore;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final OrganizationService organizationService;

    public AuthService(
            UserFileStore userFileStore,
            MemberFileStore memberFileStore,
            ServiceDomainService serviceDomainService,
            RevokedRefreshTokenStore revokedRefreshTokenStore,
            JwtTokenProvider tokenProvider,
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            OrganizationService organizationService
    ) {
        this.userFileStore = userFileStore;
        this.memberFileStore = memberFileStore;
        this.serviceDomainService = serviceDomainService;
        this.revokedRefreshTokenStore = revokedRefreshTokenStore;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.organizationService = organizationService;
    }

    public AuthResponse register(RegisterRequest request) {
        List<UserAccount> users = userFileStore.loadUsers();
        ensureEmailAvailable(users, request.email());
        if (users.isEmpty()) {
            return tokensFor(createUser(users, request, "admin"));
        }
        QapilotService service = serviceDomainService.serviceByProjectSlugAndToken(
                required(request.projectSlug(), "project_slug 필드가 필요합니다."),
                required(request.serverAuthToken(), "server_auth_token 필드가 필요합니다.")
        );
        UserAccount user = createUser(users, request, "member");
        addMember(service.serviceId(), user.userId(), "member");
        return tokensFor(user);
    }

    public AuthResponse login(LoginRequest request) {
        UserAccount user = userFileStore.findByEmail(request.email())
                .orElseThrow(() -> new QapilotException(ErrorCode.AUTH_001));
        if (!passwordEncoder.matches(request.password(), user.hashedPassword())) {
            throw new QapilotException(ErrorCode.AUTH_001);
        }
        return tokensFor(user);
    }

    public AccessTokenResponse refresh(RefreshRequest request) {
        JwtClaims claims = tokenProvider.verify(request.refreshToken());
        if (!"refresh".equals(claims.type()) || revokedRefreshTokenStore.isRevoked(claims.jti())) {
            throw new QapilotException(ErrorCode.AUTH_003);
        }
        UserAccount user = getUserById(claims.subject());
        return new AccessTokenResponse(tokenProvider.createAccessToken(user));
    }

    public void logout(LogoutRequest request) {
        JwtClaims claims = tokenProvider.verify(request.refreshToken());
        if (!"refresh".equals(claims.type())) {
            throw new QapilotException(ErrorCode.AUTH_003);
        }
        revokedRefreshTokenStore.revoke(claims.jti(), claims.expiresAt());
    }

    public UserResponse me(String userId) {
        return UserResponse.from(getUserById(userId));
    }

    private UserAccount createUser(List<UserAccount> users, RegisterRequest request, String role) {
        UserAccount user = new UserAccount(
                UUID.randomUUID().toString(),
                request.email().toLowerCase(),
                passwordEncoder.encode(request.password()),
                request.name(),
                role,
                Instant.now().toString()
        );
        List<UserAccount> updated = new ArrayList<>(users);
        updated.add(user);
        userFileStore.saveUsers(updated);
        mirrorUserToDb(user);
        organizationService.createPersonalOrg(UUID.fromString(user.userId()), user.name());
        return user;
    }

    /** users.json 과 동일한 user 를 DB 에도 복제 — dual-write 단계용. 실패 시 회원가입 자체는 성공으로 본다. */
    private void mirrorUserToDb(UserAccount user) {
        try {
            UserEntity entity = new UserEntity();
            entity.setId(UUID.fromString(user.userId()));
            entity.setEmail(user.email());
            entity.setHashedPassword(user.hashedPassword());
            entity.setName(user.name());
            userRepository.save(entity);
        } catch (Exception e) {
            log.warn("user DB mirror 실패 (file 기록은 성공) userId={} error={}", user.userId(), e.getMessage());
        }
    }

    private void addMember(String serviceId, String userId, String role) {
        memberFileStore.addIfAbsent(new ServiceMember(
                UUID.randomUUID().toString(),
                serviceId,
                userId,
                role,
                Instant.now().toString()
        ));
    }

    private AuthResponse tokensFor(UserAccount user) {
        return new AuthResponse(
                tokenProvider.createAccessToken(user),
                tokenProvider.createRefreshToken(user),
                "bearer",
                UserResponse.from(user)
        );
    }

    private UserAccount getUserById(String userId) {
        return userFileStore.findById(userId)
                .orElseThrow(() -> new QapilotException(ErrorCode.AUTH_005));
    }

    private void ensureEmailAvailable(List<UserAccount> users, String email) {
        boolean exists = users.stream().anyMatch(user -> user.email().equalsIgnoreCase(email));
        if (exists) {
            throw new QapilotException(ErrorCode.AUTH_006);
        }
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new QapilotException(ErrorCode.COMMON_001, message);
        }
        return value;
    }
}
