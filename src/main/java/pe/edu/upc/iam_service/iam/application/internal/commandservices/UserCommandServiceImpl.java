package pe.edu.upc.iam_service.iam.application.internal.commandservices;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;
import pe.edu.upc.iam_service.iam.application.internal.outboundservices.hashing.HashingService;
import pe.edu.upc.iam_service.iam.application.internal.outboundservices.tokens.TokenService;
import pe.edu.upc.iam_service.iam.domain.model.aggregates.User;
import pe.edu.upc.iam_service.iam.domain.model.commands.SignInCommand;
import pe.edu.upc.iam_service.iam.domain.model.commands.SignUpCommand;
import pe.edu.upc.iam_service.iam.domain.model.valueobjects.Roles;
import pe.edu.upc.iam_service.iam.domain.services.UserCommandService;
import pe.edu.upc.iam_service.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import pe.edu.upc.iam_service.iam.infrastructure.persistence.jpa.repositories.UserRepository;

import java.util.ArrayList;
import java.util.Optional;

@Service
public class UserCommandServiceImpl implements UserCommandService {
    private final UserRepository userRepository;
    private final HashingService hashingService;
    private final TokenService tokenService;
    private final RoleRepository roleRepository;

    public UserCommandServiceImpl(
            UserRepository userRepository,
            HashingService hashingService,
            TokenService tokenService,
            RoleRepository roleRepository
    ) {
        this.userRepository = userRepository;
        this.hashingService = hashingService;
        this.tokenService = tokenService;
        this.roleRepository = roleRepository;
    }

    @Override
    public Optional<User> handle(SignUpCommand command) {
        if (userRepository.existsByUsername(command.username()))
            throw new RuntimeException("Username already exists");

        var roles = new ArrayList<>(command.roles());
        if (roles.isEmpty()) {
            var role = roleRepository.findByName(Roles.ROLE_CUSTOMER);
            if (role.isPresent()) roles.add(role.get());
        } else {
            var foundRoles = roles.stream()
                    .map(role -> roleRepository.findByName(role.getName())
                            .orElseThrow(() -> new RuntimeException("Role not found")))
                    .toList();
            roles = new ArrayList<>(foundRoles);
        }
        var user = new User(
                command.username(),
                hashingService.encode(command.password()),
                roles
        );
        userRepository.save(user);
        return userRepository.findByUsername(command.username());
    }

    @Override
    public Optional<ImmutablePair<User, String>> handle(SignInCommand command) {
        var user = userRepository.findByUsername(command.username()).
                orElseThrow(() -> new RuntimeException("User not found"));
        if (!hashingService.matches(command.password(), user.getPassword()))
            throw new RuntimeException("Invalid password");
        var token = tokenService.generateToken(user.getUsername());
        return Optional.of(new ImmutablePair<>(user, token));
    }
}
