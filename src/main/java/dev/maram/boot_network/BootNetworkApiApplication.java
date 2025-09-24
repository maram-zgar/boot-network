package dev.maram.boot_network;

import dev.maram.boot_network.role.Role;
import dev.maram.boot_network.role.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.Optional;

@SpringBootApplication
//@EnableJpaAuditing
@EnableAsync
@EnableCaching
public class BootNetworkApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BootNetworkApiApplication.class, args);
	}

	@Bean
	public CommandLineRunner createUserRole(RoleRepository roleRepository) {
		return args -> {

			Optional<Role> role = roleRepository.findByName("USER");

			if (role.isEmpty()) {
				roleRepository.save(
						Role
								.builder()
								.name("USER")
								.build()
				);
			}
		};
	}

}
