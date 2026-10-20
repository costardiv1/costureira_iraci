package costureira.util;

import costureira.model.RegraNegocioException;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/**
 * Conversões entre texto e valores (datas, horas, dinheiro e números)
 * no padrão brasileiro.
 */
public final class Formatos {

    public static final Locale BRASIL = new Locale("pt", "BR");
    public static final DateTimeFormatter DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter HORA =
            DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private Formatos() {
    }

    public static boolean vazio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    public static String somenteDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }

    public static String data(LocalDate data) {
        return data == null ? "" : DATA.format(data);
    }

    public static String hora(LocalTime hora) {
        return hora == null ? "" : HORA.format(hora);
    }

    public static String dinheiro(double valor) {
        return NumberFormat.getCurrencyInstance(BRASIL).format(valor).replace(' ', ' ');
    }

    /** Valor com duas casas, para campos de digitação: 92,40 */
    public static String valor(double valor) {
        NumberFormat nf = NumberFormat.getNumberInstance(BRASIL);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(valor);
    }

    public static String numero(double valor) {
        NumberFormat nf = NumberFormat.getNumberInstance(BRASIL);
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(2);
        return nf.format(valor);
    }

    /** Converte "dd/mm/aaaa" em data. Texto sem dígitos é tratado como vazio (null). */
    public static LocalDate lerData(String texto, String campo, boolean obrigatorio) {
        if (somenteDigitos(texto).isEmpty()) {
            if (obrigatorio) {
                throw new RegraNegocioException("Informe " + campo + ".");
            }
            return null;
        }
        try {
            return LocalDate.parse(texto.trim(), DATA);
        } catch (DateTimeParseException e) {
            throw new RegraNegocioException(primeiraMaiuscula(campo) + " inválida. Use o formato dd/mm/aaaa.");
        }
    }

    public static LocalTime lerHora(String texto, String campo, boolean obrigatorio) {
        if (somenteDigitos(texto).isEmpty()) {
            if (obrigatorio) {
                throw new RegraNegocioException("Informe " + campo + ".");
            }
            return null;
        }
        try {
            return LocalTime.parse(texto.trim(), HORA);
        } catch (DateTimeParseException e) {
            throw new RegraNegocioException(primeiraMaiuscula(campo) + " inválida. Use o formato hh:mm.");
        }
    }

    /** Aceita "45", "45,90", "45.90", "1.234,56" e "R$ 1.234,56". */
    public static double lerDecimal(String texto, String campo) {
        String t = texto == null ? "" : texto.replace("R$", "").replace(" ", "").replace(" ", "").trim();
        if (t.isEmpty()) {
            throw new RegraNegocioException("Informe " + campo + ".");
        }
        if (t.contains(",")) {
            t = t.replace(".", "").replace(',', '.');
        }
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            throw new RegraNegocioException(primeiraMaiuscula(campo) + " deve ser um número. Ex.: 45,90");
        }
    }

    public static int lerInteiro(String texto, String campo) {
        String t = texto == null ? "" : texto.trim();
        if (t.isEmpty()) {
            throw new RegraNegocioException("Informe " + campo + ".");
        }
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            throw new RegraNegocioException(primeiraMaiuscula(campo) + " deve ser um número inteiro.");
        }
    }

    public static String primeiraMaiuscula(String texto) {
        if (vazio(texto)) {
            return "";
        }
        // ignora artigos do começo: "a data" -> "Data"
        String t = texto.replaceFirst("^(o|a|os|as) ", "");
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }
}
