package pe.edu.upc.iam_service.iam.domain.model.exceptions;

import pe.edu.upc.iam_service.iam.domain.model.valueobjects.Roles;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException(Roles role) {
        super("Role not found: " + role.name());
    }
}
