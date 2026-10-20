package costureira.model;

import java.io.Serializable;

/**
 * Classe base dos cadastros e movimentações: todos têm um id automático (@id)
 * e os métodos salvar() e excluir() do diagrama de classes.
 */
public abstract class Entidade implements Serializable {

    private static final long serialVersionUID = 1L;

    protected int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /** Valida as regras de negócio e grava (inclui se id = 0, altera se não). */
    public abstract void salvar();

    /** Remove o registro, se as regras de negócio permitirem. */
    public abstract void excluir();
}
