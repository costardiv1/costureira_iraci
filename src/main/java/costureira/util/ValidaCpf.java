package costureira.util;

/**
 * Validação do CPF pelos dígitos verificadores.
 */
public final class ValidaCpf {

    private ValidaCpf() {
    }

    public static boolean valido(String cpf) {
        String d = Formatos.somenteDigitos(cpf);
        if (d.length() != 11 || d.matches("(\\d)\\1{10}")) {
            return false;
        }
        return digito(d, 9) == d.charAt(9) - '0' && digito(d, 10) == d.charAt(10) - '0';
    }

    /** Calcula o dígito verificador da posição informada (9 ou 10). */
    private static int digito(String d, int posicao) {
        int soma = 0;
        for (int i = 0; i < posicao; i++) {
            soma += (d.charAt(i) - '0') * (posicao + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    /** 12345678909 -> 123.456.789-09 */
    public static String formatar(String cpf) {
        String d = Formatos.somenteDigitos(cpf);
        if (d.length() != 11) {
            return cpf;
        }
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }
}
