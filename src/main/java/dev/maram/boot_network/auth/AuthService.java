package dev.maram.boot_network.auth;

import dev.maram.boot_network.email.EmailService;
import dev.maram.boot_network.email.EmailTemplateName;
import dev.maram.boot_network.role.Role;
import dev.maram.boot_network.role.RoleRepository;
import dev.maram.boot_network.security.JwtService;
import dev.maram.boot_network.user.Token;
import dev.maram.boot_network.user.TokenRepository;
import dev.maram.boot_network.user.User;
import dev.maram.boot_network.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    //1. assign a default role (user) to any registring user
    //2. create a user object + save it
    //3. send a validation email (implement an email sender service)

    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${application.mailing.frontend.activation-url}")
    private String activationUrl;

    public void register(RegistrationRequest request) throws MessagingException {
        log.info("Beginning user registration for email: {}", request.getEmail());
        Role userRole = roleRepository
                .findByName("USER")
                .orElseThrow(() -> {
                    log.error("USER role not found in database - initialization issue");
                    return new IllegalArgumentException("role USER was not intialized");
                });

        log.info("Found role: {}", userRole.getName());

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .accountLocked(false)
                .enabled(false)
                .roles(List.of(userRole))
                .build();

        User savedUser = userRepository.save(user);
        log.info("User saved successfully with ID: {}", savedUser.getId());

        sendValidationEmail(user);  // 1. generate and activate the token   -   2. send the email itself
        log.info("Registration process completed for user ID: {}", savedUser.getId());
    }

    private void sendValidationEmail(User user) throws MessagingException {
        log.info("Generating validation email for user ID: {}", user.getId());

        String newToken = generateAndSaveActivationToken(user);
        log.info("Generated activation token for user ID: {}", user.getId());

        emailService.sendEmail(
                user.getEmail(),
                user.fullName(),
                EmailTemplateName.ACTIVATE_ACCOUNT,
                activationUrl,
                newToken,
                "Account activation"
        );

        log.info("Validation email sent to: {}", user.getEmail());
    }


    private String generateAndSaveActivationToken(User user) {
        log.info("Generating activation token for user ID: {}", user.getId());

        String generatedToken = generateActivationCode(6);
        log.info("Activation code generated for user ID: {}", user.getId());

        Token token = Token.builder()
                .token(generatedToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(user)
                .build();

        Token savedToken = tokenRepository.save(token);
        log.info("Token saved with ID: {} for user ID: {}", savedToken.getId(), user.getId());

        return generatedToken;
    }

    private String generateActivationCode(int length) {
        log.info("Generating activation code of length: {}", length);

        String characters = "0123456789";
        StringBuilder codeBuilder = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();

        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }

        String code = codeBuilder.toString();
        log.info("Activation code generated successfully");
        return code;
    }

    // this method will take care of the whole authentication process, if the userName and password are correct it will return the authentication
    // 1.takes the user's email and password from the AuthenticationRequest and hands them to the AuthenticationManager, The AuthenticationManager then verifies these credentials against the database
    // 2.extracts the user object from the successful Authentication and creates a Map of extra information (claims) to be included in the JWT(the user's full name).
    // 3.creates a new token using JwtService, that contains the user's full name and details
    public AuthenticationResponse authenticate(@Valid AuthenticationRequest request) {

        log.info("Authentication attempt for email: {}", request.getEmail());

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        log.info("Authentication successful for email: {}", request.getEmail());

        Claims claims = Jwts.claims();
        User user = (User) auth.getPrincipal();
        claims.put("fullName", user.fullName());

        String jwtToken = jwtService.generateToken(claims, (UserDetails) user);
        log.info("JWT token generated for user ID: {}", user.getId());

        return AuthenticationResponse
                .builder()
                .token(jwtToken)
                .build();
    }

    @Transactional // used to manage database transactions
    public void activateAccount(String token) throws MessagingException {
        log.info("Account activation attempt with token: {}", token);

        Token savedToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.error("Invalid activation token attempted: {}", token);
                    return new RuntimeException("Invalid token!");
                });

        log.info("Token found for user ID: {}", savedToken.getUser().getId());

        if(LocalDateTime.now().isAfter(savedToken.getExpiresAt())) {
            log.error("Token expired for user ID: {}, sending new validation email", savedToken.getUser().getId());
            sendValidationEmail(savedToken.getUser());
            throw new RuntimeException("Activation token has expired, A new token has been sent to the same email address");
        }

        User user = userRepository.findById(savedToken.getUser().getId())
                .orElseThrow(() -> {
                    log.error("User not found for token: {}", token);
                    return new UsernameNotFoundException("User not found");
                });

        user.setEnabled(true);
        userRepository.save(user);
        log.info("Account activated for user ID: {}", user.getId());

        savedToken.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(savedToken);
        log.info("Token validated at: {}", savedToken.getValidatedAt());
    }
}