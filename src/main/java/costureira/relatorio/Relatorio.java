package costureira.relatorio;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * Um relatório: tem filtros próprios e gera uma tabela.
 * Todos usam apenas atributos que existem nos cadastros e movimentações.
 */
public abstract class Relatorio {

    private final String nome;

    protected Relatorio(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

    /** Painel com os filtros do relatório. */
    public abstract JComponent getFiltros();

    public abstract Resultado gerar();

    @Override
    public String toString() {
        return nome;
    }

    /** Tabela gerada pelo relatório. */
    public static class Resultado {
        public final String titulo;
        public final String[] colunas;
        public final List<Object[]> linhas = new ArrayList<>();
        public String rodape = "";

        public Resultado(String titulo, String... colunas) {
            this.titulo = titulo;
            this.colunas = colunas;
        }

        public void linha(Object... valores) {
            linhas.add(valores);
        }
    }
}
