package dev.maram.boot_network.user;

import dev.maram.boot_network.exception.UserAlreadyExistsException;
import dev.maram.boot_network.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Cacheable(cacheNames = "users", key = "#userId")
    public UserDto getUserById(Integer userId) {
        log.info("Fetching user by ID: {} (cache miss)", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new UserNotFoundException("Cannot find user with id " + userId);
                });

        UserDto userDto = userMapper.toDto(user);
        log.info("User fetched and mapped to DTO: {}", userDto.getEmail());
        return userDto;
    }

    @Caching(
            put = @CachePut(cacheNames = "users", key = "#userDto.id"),
            evict = {
                    @CacheEvict(cacheNames = "users", key = "'ALL'"),
                    @CacheEvict(cacheNames = "users", key = "'email:' + #result.email")
            }
    )
    public UserDto updateUser(UserDto userDto) {
        log.info("Updating user with ID: {}", userDto.getId());

        User existingUser = userRepository.findById(userDto.getId())
                .orElseThrow(() -> {
                    log.error("Cannot update user - user not found with ID: {}", userDto.getId());
                    return new UserNotFoundException("Cannot find user with id " + userDto.getId());
                });

        // Store old email for cache eviction
        String oldEmail = existingUser.getEmail();

        // Update entity from DTO
        userMapper.updateEntityFromDto(userDto, existingUser);

        User updatedUser = userRepository.save(existingUser);
        log.info("User updated successfully: {}", updatedUser.getId());

        UserDto updatedUserDto = userMapper.toDto(updatedUser);
        log.info("Updated user mapped to DTO");

        // If email changed, evict old email cache entry
        if (!oldEmail.equals(updatedUserDto.getEmail())) {
            evictUserByEmailCache(oldEmail);
        }

        return updatedUserDto;
    }

    @CacheEvict(cacheNames = "users", allEntries = true)
    public void deleteUser(Integer userId) {
        log.info("Deleting user with ID: {}", userId);

        // Get user first to evict email-based cache
        User userToDelete = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Cannot delete user - user not found with ID: {}", userId);
                    return new UserNotFoundException("Cannot find user with id " + userId);
                });

        userRepository.deleteById(userId);
        log.info("User deleted successfully: {}", userId);
    }

    public UserDto createUser(UserDto userDto) {
        log.info("Creating new user: {}", userDto.getEmail());

        // Check if user with email already exists
        if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
            log.error("User with email {} already exists", userDto.getEmail());
            throw new UserAlreadyExistsException("User with email " + userDto.getEmail() + " already exists");
        }

        User user = userMapper.toEntity(userDto);
        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());

        UserDto savedUserDto = userMapper.toDto(savedUser);
        log.info("New user mapped to DTO");
        return savedUserDto;
    }

    public List<UserDto> getAllUsers() {
        log.info("Fetching all users (cache miss)");

        List<User> users = userRepository.findAll();
        List<UserDto> userDtos = userMapper.toDtoList(users);

        log.info("Fetched {} users and mapped to DTOs", userDtos.size());
        return userDtos;
    }

    @Cacheable(cacheNames = "users", key = "'email:' + #email")
    public UserDto getUserByEmail(String email) {
        log.info("Fetching user by email: {} (cache miss)", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", email);
                    return new UserNotFoundException("Cannot find user with email " + email);
                });

        UserDto userDto = userMapper.toDto(user);
        log.info("User fetched by email and mapped to DTO");
        return userDto;
    }

    // Helper method for cache eviction
    @CacheEvict(cacheNames = "users", key = "'email:' + #email")
    private void evictUserByEmailCache(String email) {
        log.debug("Evicting cache for email: {}", email);
    }

}