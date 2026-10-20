package costureira.view;

import costureira.model.RegraNegocioException;
import costureira.relatorio.Relatorio;
import costureira.relatorio.Relatorios;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.print.PrinterException;
import java.text.MessageFormat;

/** Tela de relatórios: escolhe o relatório, ajusta os filtros e clica em Gerar. */
public class TelaRelatorios extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JComboBox<Relatorio> relatorio = new JComboBox<>(Relatorios.todos().toArray(new Relatorio[0]));
    private final JPanel areaFiltros = new JPanel(new BorderLayout());
    private final DefaultTableModel modelo = new DefaultTableModel() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return false;
        }
    };
    private final JTable tabela = new JTable(modelo);
    private final JLabel titulo = new JLabel(" ");
    private final JLabel rodape = new JLabel(" ");

    public TelaRelatorios(Window dono) {
        this(dono, 0);
    }

    public TelaRelatorios(Window dono, int indiceInicial) {
        super(dono, "Relatórios", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JButton gerar = new JButton("Gerar");
        JButton imprimir = new JButton("Imprimir");
        JButton fechar = new JButton("Fechar");
        gerar.addActionListener(e -> gerar());
        imprimir.addActionListener(e -> imprimir());
        fechar.addActionListener(e -> dispose());
        relatorio.addActionListener(e -> trocarRelatorio());

        JPanel escolha = new JPanel(new FlowLayout(FlowLayout.LEFT));
        escolha.add(new JLabel("Relatório:"));
        escolha.add(relatorio);
        escolha.add(gerar);

        areaFiltros.setBorder(BorderFactory.createTitledBorder("Filtros"));
        JPanel topo = new JPanel(new BorderLayout());
        topo.add(escolha, BorderLayout.NORTH);
        topo.add(areaFiltros, BorderLayout.CENTER);

        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 14f));
        rodape.setFont(rodape.getFont().deriveFont(Font.BOLD));
        tabela.setRowHeight(22);
        JPanel centro = new JPanel(new BorderLayout(0, 4));
        centro.add(titulo, BorderLayout.NORTH);
        centro.add(new JScrollPane(tabela), BorderLayout.CENTER);
        centro.add(rodape, BorderLayout.SOUTH);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botoes.add(imprimir);
        botoes.add(fechar);

        JPanel conteudo = new JPanel(new BorderLayout(4, 4));
        conteudo.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        conteudo.add(topo, BorderLayout.NORTH);
        conteudo.add(centro, BorderLayout.CENTER);
        conteudo.add(botoes, BorderLayout.SOUTH);
        setContentPane(conteudo);

        relatorio.setSelectedIndex(indiceInicial);
        trocarRelatorio();
        setSize(1050, 650);
        setLocationRelativeTo(dono);
    }

    private void trocarRelatorio() {
        Relatorio r = (Relatorio) relatorio.getSelectedItem();
        areaFiltros.removeAll();
        if (r != null) {
            areaFiltros.add(r.getFiltros(), BorderLayout.CENTER);
        }
        areaFiltros.revalidate();
        areaFiltros.repaint();
        gerar();
    }

    private void gerar() {
        Relatorio r = (Relatorio) relatorio.getSelectedItem();
        if (r == null) {
            return;
        }
        try {
            Relatorio.Resultado res = r.gerar();
            modelo.setDataVector(res.linhas.toArray(new Object[0][]), res.colunas);
            titulo.setText(res.titulo);
            rodape.setText(res.linhas.isEmpty() ? "Nenhum registro encontrado." : res.rodape);
        } catch (RegraNegocioException ex) {
            Mensagens.aviso(this, ex.getMessage());
        }
    }

    private void imprimir() {
        try {
            tabela.print(JTable.PrintMode.FIT_WIDTH, new MessageFormat(titulo.getText()),
                    new MessageFormat(rodape.getText().replace("'", "''") + " — página {0}"));
        } catch (PrinterException ex) {
            Mensagens.erro(this, "Não foi possível imprimir: " + ex.getMessage());
        }
    }
}
