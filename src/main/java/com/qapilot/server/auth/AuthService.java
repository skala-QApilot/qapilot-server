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
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.organization.OrganizationService;
import com.qapilot.server.service.ServiceDomainService;
import com.qapilot.server.service.domain.QapilotService;
import com.qapilot.server.service.persistence.ServiceMemberEntity;
import com.qapilot.server.service.persistence.ServiceMemberRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 인증/회원가입 유스케이스. PR-15h — JPA only.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18, rewritten 2026-06-02
 */
@Service
public class AuthService {

    private final ServiceDomainService serviceDomainService;
    private final RevokedRefreshTokenStore revokedRefreshTokenStore;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final ServiceMemberRepository serviceMemberRepository;
    private final OrganizationService organizationService;

    public AuthService(
            ServiceDomainService serviceDomainService,
            RevokedRefreshTokenStore revokedRefreshTokenStore,
            JwtTokenProvider tokenProvider,
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            ServiceMemberRepository serviceMemberRepository,
            OrganizationService organizationService
    ) {
        this.serviceDomainService = serviceDomainService;
        this.revokedRefreshTokenStore = revokedRefreshTokenStore;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.serviceMemberRepository = serviceMemberRepository;
        this.organizationService = organizationService;
    }

    public AuthResponse register(RegisterRequest request) {
        ensureEmailAvailable(request.email());
        boolean firstUser = userRepository.count() == 0;
        if (firstUser) {
            return tokensFor(createUser(request, "admin"));
        }
        QapilotService service = serviceDomainService.serviceByProjectSlugAndToken(
                required(request.projectSlug(), "project_slug 필드가 필요합니다."),
                required(request.serverAuthToken(), "server_auth_token 필드가 필요합니다.")
        );
        UserAccount user = createUser(request, "member");
        addServiceMember(service.serviceId(), user.userId(), "member");
        return tokensFor(user);
    }

    public AuthResponse login(LoginRequest request) {
        UserAccount user = userRepository.findByEmail(request.email().toLowerCase())
                .map(this::toUserAccount)
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

    private UserAccount createUser(RegisterRequest request, String role) {
        UUID userId = UUID.randomUUID();
        UserEntity entity = new UserEntity();
        entity.setId(userId);
        entity.setEmail(request.email().toLowerCase());
        entity.setHashedPassword(passwordEncoder.encode(request.password()));
        entity.setName(request.name());
        entity.setRole(role);
        userRepository.save(entity);
        organizationService.createPersonalOrg(userId, request.name());
        return toUserAccount(entity);
    }

    private void addServiceMember(String serviceId, String userId, String role) {
        UUID svc = UUID.fromString(serviceId);
        UUID uid = UUID.fromString(userId);
        if (serviceMemberRepository.existsByServiceIdAndUserId(svc, uid)) {
            return;
        }
        ServiceMemberEntity member = new ServiceMemberEntity();
        member.setServiceId(svc);
        member.setUserId(uid);
        member.setRole(role);
        serviceMemberRepository.save(member);
    }

    private UserAccount getUserById(String userId) {
        try {
            return userRepository.findById(UUID.fromString(userId))
                    .map(this::toUserAccount)
                    .orElseThrow(() -> new QapilotException(ErrorCode.AUTH_005));
        } catch (IllegalArgumentException e) {
            throw new QapilotException(ErrorCode.AUTH_005);
        }
    }

    private UserAccount toUserAccount(UserEntity e) {
        return new UserAccount(
                e.getId().toString(),
                e.getEmail(),
                e.getHashedPassword(),
                e.getName(),
                e.getRole(),
                toIso(e.getCreatedAt())
        );
    }

    private void ensureEmailAvailable(String email) {
        if (userRepository.findByEmail(email.toLowerCase()).isPresent()) {
            throw new QapilotException(ErrorCode.AUTH_006);
        }
    }

    private AuthResponse tokensFor(UserAccount user) {
        return new AuthResponse(
                tokenProvider.createAccessToken(user),
                tokenProvider.createRefreshToken(user),
                "bearer",
                UserResponse.from(user)
        );
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new QapilotException(ErrorCode.COMMON_001, message);
        }
        return value;
    }

    private String toIso(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
