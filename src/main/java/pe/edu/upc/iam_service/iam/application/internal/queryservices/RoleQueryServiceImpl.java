package pe.edu.upc.iam_service.iam.application.internal.queryservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.iam_service.iam.domain.model.entities.Role;
import pe.edu.upc.iam_service.iam.domain.model.queries.GetAllRolesQuery;
import pe.edu.upc.iam_service.iam.domain.model.queries.GetRoleByIdQuery;
import pe.edu.upc.iam_service.iam.domain.services.RoleQueryService;
import pe.edu.upc.iam_service.iam.infrastructure.persistence.jpa.repositories.RoleRepository;

import java.util.List;
import java.util.Optional;

@Service
public class RoleQueryServiceImpl implements RoleQueryService {
    private final RoleRepository roleRepository;

    public RoleQueryServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public List<Role> handle(GetAllRolesQuery query) {
        return roleRepository.findAll();
    }

    @Override
    public Optional<Role> handle(GetRoleByIdQuery query) {
        return roleRepository.findById(query.roleId());
    }
}
