package pe.edu.upc.iam_service.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignInResource(
        @NotBlank
        @Size(max = 254)
        String username,

        @NotBlank
        @Size(min = 8, max = 128)
        String password) {
}
