package pe.edu.upc.iam_service.iam.domain.services;

import pe.edu.upc.iam_service.iam.domain.model.commands.SeedRolesCommand;

public interface RoleCommandService {
    void handle(SeedRolesCommand command);
}
