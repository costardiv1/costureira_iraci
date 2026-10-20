package costureira.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Cópia profunda de objetos. As telas editam uma cópia, assim o registro
 * gravado só muda quando o usuário clica em Salvar.
 */
public final class Copia {

    private Copia() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T de(T objeto) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
                out.writeObject(objeto);
            }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                return (T) in.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Falha ao copiar objeto", e);
        }
    }
}
