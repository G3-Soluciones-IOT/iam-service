package pe.edu.upc.iam_service.iam.domain.services;

import pe.edu.upc.iam_service.iam.domain.model.entities.Role;
import pe.edu.upc.iam_service.iam.domain.model.queries.GetAllRolesQuery;
import pe.edu.upc.iam_service.iam.domain.model.queries.GetRoleByIdQuery;

import java.util.List;
import java.util.Optional;

public interface RoleQueryService {
    List<Role> handle(GetAllRolesQuery query);

    Optional<Role> handle(GetRoleByIdQuery query);
}
