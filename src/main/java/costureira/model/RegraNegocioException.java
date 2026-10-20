package costureira.model;

/**
 * Lançada quando uma operação fere uma regra de negócio. A mensagem é
 * mostrada para a usuária.
 */
public class RegraNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
