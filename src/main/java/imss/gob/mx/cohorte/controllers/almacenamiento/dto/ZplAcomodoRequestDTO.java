package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Acomodo de las etiquetas sobre los carriles del rollo.
 *
 * {@code slots} describe la superficie del rollo tal como la ve el operador en
 * pantalla: una posición por carril, en orden de avance, con {@code null} donde
 * el carril se deja en blanco. Se manda la disposición completa y no solo la
 * lista de muestras porque la Zebra consume el papel por filas enteras, y elegir
 * en qué carril cae cada etiqueta es justo lo que permite aprovechar una tira de
 * rollo ya empezada.
 *
 * @param configuracionId  configuración de etiqueta a usar; nula toma la predeterminada
 * @param slots            muestras por carril, con huecos como {@code null}
 * @param marcoDepuracion  dibuja el contorno de cada etiqueta para calibrar
 */
public record ZplAcomodoRequestDTO(
        Long configuracionId,

        @NotEmpty(message = "El acomodo no puede estar vacío")
        @Size(max = 500, message = "El acomodo no puede exceder 500 posiciones")
        List<Long> slots,

        Boolean marcoDepuracion
) {
    public boolean marcoActivo() {
        return Boolean.TRUE.equals(marcoDepuracion);
    }
}
