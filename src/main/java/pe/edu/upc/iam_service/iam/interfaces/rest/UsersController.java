package pe.edu.upc.iam_service.iam.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.iam_service.iam.domain.model.queries.GetAllUsersQuery;
import pe.edu.upc.iam_service.iam.domain.model.queries.GetUserByIdQuery;
import pe.edu.upc.iam_service.iam.domain.services.UserQueryService;
import pe.edu.upc.iam_service.iam.interfaces.rest.resources.UserResource;
import pe.edu.upc.iam_service.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Users", description = "User Management Endpoints")
public class UsersController {

    private final UserQueryService userQueryService;

    public UsersController(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    @Operation(summary = "Get all users",
            description = "Retrieves a list of all registered users in the system.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of users retrieved successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(schema = @Schema(implementation = UserResource.class)))),
                    @ApiResponse(responseCode = "500", description = "Internal server error",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('read:users', 'ROLE_ADMIN', 'ROLE_SERVICE')")
    public ResponseEntity<List<UserResource>> getAllUsers() {
        var getAllUsersQuery = new GetAllUsersQuery();
        var users = userQueryService.handle(getAllUsersQuery);
        var userResources = users.stream()
                .map(UserResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(userResources);
    }

    @Operation(summary = "Get a user by ID",
            description = "Retrieves the details of a specific user using their unique identifier.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User found",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UserResource.class))),
                    @ApiResponse(responseCode = "404", description = "User not found",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
                    @ApiResponse(responseCode = "500", description = "Internal server error",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyAuthority('read:users', 'ROLE_ADMIN', 'ROLE_SERVICE')")
    public ResponseEntity<UserResource> getUserById(@PathVariable Long userId) {
        var getUserByIdQuery = new GetUserByIdQuery(userId);
        var user = userQueryService.handle(getUserByIdQuery);
        if (user.isEmpty()) return ResponseEntity.notFound().build();
        var userResource = UserResourceFromEntityAssembler.toResourceFromEntity(user.get());
        return ResponseEntity.ok(userResource);
    }
}
