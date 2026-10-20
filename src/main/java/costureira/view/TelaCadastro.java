package costureira.view;

import costureira.model.Entidade;
import costureira.model.RegraNegocioException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.Scrollable;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Tela padrão de cadastro: lista (tabela) em cima, formulário embaixo e os botões
 * Cadastrar, Editar, Salvar, Cancelar e Excluir (os métodos do diagrama de classes).
 *
 * As subclasses montam o formulário e dizem como ler/preencher os campos.
 * O registro gravado só é alterado ao clicar em Salvar: lerFormulario() cria um
 * objeto novo e salvar() dele substitui o antigo.
 */
public abstract class TelaCadastro<T extends Entidade> extends JDialog {

    private static final long serialVersionUID = 1L;

    protected final JButton btCadastrar = new JButton("Cadastrar");
    protected final JButton btEditar = new JButton("Editar");
    protected final JButton btSalvar = new JButton("Salvar");
    protected final JButton btCancelar = new JButton("Cancelar");
    protected final JButton btExcluir = new JButton("Excluir");
    protected final JButton btFechar = new JButton("Fechar");

    private List<T> registros = new ArrayList<>(); // precisa vir antes do modelo da tabela
    private final JTextField campoBusca = new JTextField(25);
    private final Modelo modelo = new Modelo();
    protected final JTable tabela = new JTable(modelo);
    private final TableRowSorter<Modelo> ordenador = new TableRowSorter<>(modelo);
    private final JLabel rotuloTotal = new JLabel();

    private T selecionado;
    private boolean editando;
    private boolean novo;

    protected TelaCadastro(Window dono, String titulo) {
        super(dono, titulo, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    // ---------- o que cada tela precisa implementar ----------

    protected abstract String[] colunas();

    protected abstract Object[] linha(T registro);

    protected abstract List<T> listar();

    protected abstract JComponent criarFormulario();

    /** Mostra o registro nos campos. */
    protected abstract void preencher(T registro);

    protected abstract void limpar();

    protected abstract void habilitarCampos(boolean habilitar);

    /** Cria um objeto NOVO a partir dos campos (o id é colocado pela tela). */
    protected abstract T lerFormulario();

    // ---------- ganchos opcionais ----------

    /** Atualiza combos antes de cadastrar/editar/mostrar (atual = registro mostrado ou null). */
    protected void atualizarListas(T atual) {
    }

    /** Valores padrão de um registro novo (ex.: data de hoje). */
    protected void valoresIniciais() {
    }

    /** Diz se o registro pode ser editado; lança RegraNegocioException se não puder. */
    protected void verificarEdicao(T registro) {
    }

    protected List<JButton> botoesExtras() {
        return Collections.emptyList();
    }

    /** Chamado quando a seleção ou o modo (edição/visualização) muda. */
    protected void atualizarBotoesExtras(T selecionado, boolean editando) {
    }

    protected boolean permiteExcluir() {
        return true;
    }

    /** Cor de fundo da linha (null = padrão). */
    protected Color corLinha(T registro) {
        return null;
    }

    protected String textoRodape(List<T> registros) {
        return registros.size() + " registro(s)";
    }

    protected int[] largurasColunas() {
        return null;
    }

    // ---------- montagem ----------

    /** Deve ser chamado no fim do construtor da subclasse. */
    protected void montar() {
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("Pesquisar:"));
        topo.add(campoBusca);
        topo.add(rotuloTotal);

        tabela.setRowSorter(ordenador);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setAutoCreateRowSorter(false);
        tabela.setRowHeight(22);
        tabela.setDefaultRenderer(Object.class, new Renderizador());
        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !editando) {
                mostrarSelecionado();
            }
        });
        tabela.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && !editando && selecionado != null) {
                    editar();
                }
            }
        });
        campoBusca.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filtrar(); }
            public void removeUpdate(DocumentEvent e) { filtrar(); }
            public void changedUpdate(DocumentEvent e) { filtrar(); }
        });

        JComponent formulario = criarFormulario();
        JPanel painelForm = new PainelLarguraDaJanela();
        painelForm.setBorder(BorderFactory.createTitledBorder("Dados"));
        painelForm.add(formulario, BorderLayout.CENTER);

        JSplitPane divisor = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(tabela), new JScrollPane(painelForm));
        divisor.setResizeWeight(0.45);
        divisor.setBorder(null);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        for (JButton b : botoesExtras()) {
            botoes.add(b);
        }
        botoes.add(btCadastrar);
        botoes.add(btEditar);
        botoes.add(btSalvar);
        botoes.add(btCancelar);
        if (permiteExcluir()) {
            botoes.add(btExcluir);
        }
        botoes.add(btFechar);

        btCadastrar.addActionListener(e -> cadastrar());
        btEditar.addActionListener(e -> editar());
        btSalvar.addActionListener(e -> salvar());
        btCancelar.addActionListener(e -> cancelar());
        btExcluir.addActionListener(e -> excluir());
        btFechar.addActionListener(e -> fecharTela());
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                fecharTela();
            }
        });
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        JPanel conteudo = new JPanel(new BorderLayout(4, 4));
        conteudo.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        conteudo.add(topo, BorderLayout.NORTH);
        conteudo.add(divisor, BorderLayout.CENTER);
        conteudo.add(botoes, BorderLayout.SOUTH);
        setContentPane(conteudo);

        int[] larguras = largurasColunas();
        if (larguras != null) {
            for (int i = 0; i < larguras.length && i < tabela.getColumnCount(); i++) {
                tabela.getColumnModel().getColumn(i).setPreferredWidth(larguras[i]);
            }
        }

        recarregar(0);
        modoVisualizacao();
        setSize(1000, 680);
        setLocationRelativeTo(getOwner());
    }

    // ---------- ações (métodos do diagrama) ----------

    /** cadastrar(): limpa o formulário para um registro novo. */
    protected void cadastrar() {
        novo = true;
        atualizarListas(null);
        limpar();
        valoresIniciais();
        modoEdicao();
    }

    /** editar(): libera os campos do registro selecionado. */
    protected void editar() {
        if (selecionado == null) {
            Mensagens.aviso(this, "Selecione um registro na lista.");
            return;
        }
        try {
            verificarEdicao(selecionado);
        } catch (RegraNegocioException ex) {
            Mensagens.aviso(this, ex.getMessage());
            return;
        }
        novo = false;
        atualizarListas(selecionado);
        preencher(selecionado);
        modoEdicao();
    }

    /** salvar(): valida e grava. */
    protected void salvar() {
        try {
            T registro = lerFormulario();
            registro.setId(novo ? 0 : selecionado.getId());
            registro.salvar();
            modoVisualizacaoSemMostrar();
            recarregar(registro.getId());
            depoisDeSalvar(registro, novo);
        } catch (RegraNegocioException ex) {
            Mensagens.aviso(this, ex.getMessage());
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            Mensagens.erro(this, "Erro ao salvar: " + ex.getMessage());
        }
    }

    protected void depoisDeSalvar(T registro, boolean eraNovo) {
    }

    /** cancelar(): desfaz a edição. */
    protected void cancelar() {
        modoVisualizacao();
    }

    /** excluir(): remove o registro selecionado, se as regras permitirem. */
    protected void excluir() {
        if (selecionado == null) {
            Mensagens.aviso(this, "Selecione um registro na lista.");
            return;
        }
        if (!Mensagens.confirmar(this, "Confirma a exclusão do registro selecionado?")) {
            return;
        }
        try {
            selecionado.excluir();
            recarregar(0);
        } catch (RegraNegocioException ex) {
            Mensagens.aviso(this, ex.getMessage());
        }
    }

    private void fecharTela() {
        if (editando && !Mensagens.confirmar(this, "Existem alterações não salvas. Deseja sair mesmo assim?")) {
            return;
        }
        dispose();
    }

    // ---------- controle de estado ----------

    private void modoEdicao() {
        editando = true;
        tabela.setEnabled(false);
        campoBusca.setEnabled(false);
        habilitarCampos(true);
        atualizarBotoes();
    }

    private void modoVisualizacao() {
        modoVisualizacaoSemMostrar();
        mostrarSelecionado();
    }

    private void modoVisualizacaoSemMostrar() {
        editando = false;
        novo = false;
        tabela.setEnabled(true);
        campoBusca.setEnabled(true);
        habilitarCampos(false);
        atualizarBotoes();
    }

    private void atualizarBotoes() {
        btCadastrar.setEnabled(!editando);
        btEditar.setEnabled(!editando && selecionado != null);
        btExcluir.setEnabled(!editando && selecionado != null);
        btSalvar.setEnabled(editando);
        btCancelar.setEnabled(editando);
        atualizarBotoesExtras(editando ? null : selecionado, editando);
    }

    private void mostrarSelecionado() {
        int linhaVista = tabela.getSelectedRow();
        selecionado = linhaVista < 0 ? null : registros.get(tabela.convertRowIndexToModel(linhaVista));
        if (selecionado == null) {
            limpar();
        } else {
            atualizarListas(selecionado);
            preencher(selecionado);
        }
        atualizarBotoes();
    }

    /** Recarrega a lista e seleciona o registro de id informado (0 = nenhum). */
    protected void recarregar(int idSelecionar) {
        registros = listar();
        modelo.fireTableDataChanged();
        rotuloTotal.setText("   " + textoRodape(registros));
        tabela.clearSelection();
        for (int i = 0; i < registros.size(); i++) {
            if (registros.get(i).getId() == idSelecionar) {
                int vista = tabela.convertRowIndexToView(i);
                if (vista >= 0) {
                    tabela.setRowSelectionInterval(vista, vista);
                    tabela.scrollRectToVisible(tabela.getCellRect(vista, 0, true));
                }
                break;
            }
        }
        if (!editando) {
            mostrarSelecionado();
        }
    }

    protected T getSelecionado() {
        return selecionado;
    }

    protected boolean isEditando() {
        return editando;
    }

    protected boolean isNovo() {
        return novo;
    }

    private void filtrar() {
        String texto = campoBusca.getText().trim();
        ordenador.setRowFilter(texto.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(texto)));
    }

    /** Painel que acompanha a largura da janela (só rola na vertical). */
    private static class PainelLarguraDaJanela extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;

        PainelLarguraDaJanela() {
            super(new BorderLayout());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle area, int orientacao, int direcao) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle area, int orientacao, int direcao) {
            return area.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getParent().getHeight() > getPreferredSize().height;
        }
    }

    private class Modelo extends AbstractTableModel {
        private static final long serialVersionUID = 1L;

        @Override
        public int getRowCount() {
            return registros.size();
        }

        @Override
        public int getColumnCount() {
            return colunas().length;
        }

        @Override
        public String getColumnName(int coluna) {
            return colunas()[coluna];
        }

        @Override
        public Object getValueAt(int linha, int coluna) {
            Object valor = linha(registros.get(linha))[coluna];
            return valor == null ? "" : valor;
        }
    }

    private class Renderizador extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean sel, boolean foco, int lin, int col) {
            Component c = super.getTableCellRendererComponent(t, valor, sel, foco, lin, col);
            if (!sel) {
                Color cor = corLinha(registros.get(t.convertRowIndexToModel(lin)));
                c.setBackground(cor != null ? cor : t.getBackground());
            }
            return c;
        }
    }
}
