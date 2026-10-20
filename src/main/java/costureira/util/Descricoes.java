package costureira.util;

/**
 * Significado dos códigos de status usados no documento de requisitos
 * ("STATUS DAS CLASSES DO SISTEMA").
 */
public final class Descricoes {

    /** Formas de pagamento (d/p/db/cr). */
    public static final String[] FORMAS_PAGTO = {"d", "p", "db", "cr"};

    private Descricoes() {
    }

    /** STATUS (a/i) = ativo, inativo. */
    public static String statusCadastro(char status) {
        return status == 'a' ? "Ativo" : status == 'i' ? "Inativo" : "?";
    }

    /** STATUS (a/c/e/f) = agendado, cancelado, execução, finalizado. */
    public static String statusOrdem(char status) {
        switch (status) {
            case 'a': return "Agendado";
            case 'c': return "Cancelado";
            case 'e': return "Em execução";
            case 'f': return "Finalizado";
            default: return "?";
        }
    }

    /** STATUS do recebimento: ativo ou cancelado (estornado). */
    public static String statusRecebimento(char status) {
        return status == 'a' ? "Recebido" : status == 'c' ? "Cancelado" : "?";
    }

    /** GÊNERO (f/m/n) = feminino, masculino, não informado. */
    public static String genero(char genero) {
        switch (genero) {
            case 'f': return "Feminino";
            case 'm': return "Masculino";
            default: return "Não informado";
        }
    }

    /** FORMA DE PAGAMENTO (d/p/db/cr) = dinheiro, pix, cartão de débito, cartão de crédito. */
    public static String formaPagto(String forma) {
        if (forma == null) {
            return "";
        }
        switch (forma) {
            case "d": return "Dinheiro";
            case "p": return "PIX";
            case "db": return "Cartão de débito";
            case "cr": return "Cartão de crédito";
            default: return forma;
        }
    }
}
