package es.labjc.regalos3d.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ClienteDtos {

    private ClienteDtos() {
    }

    /** Alta y modificación. Nombre y móvil obligatorios; email y dirección opcionales. */
    public record ClienteRequest(
            @NotBlank @Size(max = 25) String telefono,
            @NotBlank @Size(max = 100) String nombre,
            @Email @Size(max = 150) String email,
            @Size(max = 300) String direccion,
            @Size(max = 500) String notas) {
    }

    public record ClienteResponse(Long id, String telefono, String nombre, String email,
                                  String direccion, String notas) {

        public static ClienteResponse de(Cliente c) {
            return new ClienteResponse(c.getId(), c.getTelefono(), c.getNombre(), c.getEmail(),
                    c.getDireccion(), c.getNotas());
        }
    }
}
