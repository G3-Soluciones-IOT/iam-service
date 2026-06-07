package pe.edu.upc.iam_service.iam.interfaces.rest.resources;

public record AuthenticatedUserResource(Long id, String username, String token) {
}
