package costureira.view;

import costureira.model.OrdemServico;
import costureira.model.Recebimento;
import costureira.model.RegraNegocioException;
import costureira.util.Formatos;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.print.PrinterException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Caso de uso "Registrar recebimento" (<<include>> "Consultar serviço").
 * Recebimento sempre à vista, na entrega: registrar finaliza a ordem.
 */
public class TelaRecebimentos extends TelaCadastro<Recebimento> {

    private static final long serialVersionUID = 1L;

    private final JComboBox<OrdemServico> ordem = new JComboBox<>();
    private final JTextArea consulta = new JTextArea();
    private final JFormattedTextField dataRecebimento = Campos.data();
    private final JComboBox<Campos.Opcao> formaPagto = Campos.comboOpcoes(
            new Campos.Opcao("d", "Dinheiro"), new Campos.Opcao("p", "PIX"),
            new Campos.Opcao("db", "Cartão de débito"), new Campos.Opcao("cr", "Cartão de crédito"));
    private final JTextField valorRecebido = Campos.texto(10);
    private final JTextField retiradaPor = Campos.texto(25);
    private final JTextField obs = Campos.texto(20);
    private final JTextField status = Campos.texto(10);

    private final JButton btEstornar = new JButton("Cancelar recebimento");
    private final JButton btRecibo = new JButton("Imprimir recibo");

    public TelaRecebimentos(Window dono) {
        super(dono, "Recebimentos (entrega das roupas)");
        consulta.setEditable(false);
        consulta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        status.setEditable(false);
        btCadastrar.setText("Registrar recebimento");
        ordem.addActionListener(e -> ordemSelecionada());
        btEstornar.addActionListener(e -> estornar());
        btRecibo.addActionListener(e -> imprimirRecibo());
        montar();
        setSize(1000, 720);
        setLocationRelativeTo(dono);
    }

    @Override
    protected String[] colunas() {
        return new String[]{"Nº", "OS", "Cliente", "Data", "Forma de pagamento", "Valor recebido", "Status"};
    }

    @Override
    protected int[] largurasColunas() {
        return new int[]{40, 40, 230, 90, 140, 110, 80};
    }

    @Override
    protected Object[] linha(Recebimento r) {
        OrdemServico os = r.getOrdem();
        return new Object[]{r.getId(), r.getIdOrdem(), os == null ? "" : os.getNomeCliente(),
            Formatos.data(r.getDataRecebimento()), r.getFormaPagtoDescricao(),
            Formatos.dinheiro(r.getValorRecebido()), r.getStatusDescricao()};
    }

    @Override
    protected List<Recebimento> listar() {
        return Recebimento.listar();
    }

    @Override
    protected boolean permiteExcluir() {
        return false;
    }

    @Override
    protected Color corLinha(Recebimento r) {
        return r.getStatus() == 'c' ? Cores.CANCELADO : null;
    }

    @Override
    protected String textoRodape(List<Recebimento> registros) {
        double soma = 0;
        for (Recebimento r : registros) {
            if (r.getStatus() == 'a') {
                soma += r.getValorRecebido();
            }
        }
        return registros.size() + " recebimento(s) — total recebido: " + Formatos.dinheiro(soma);
    }

    @Override
    protected JComponent criarFormulario() {
        JScrollPane rolagemConsulta = new JScrollPane(consulta);
        rolagemConsulta.setPreferredSize(new Dimension(400, 170)); // não deixa o texto alargar a tela
        PainelForm form = new PainelForm()
                .linha("Ordem de serviço*:", ordem)
                .linha("Consultar serviço:", rolagemConsulta)
                .linha("Data do recebimento*:", dataRecebimento, "Forma de pagamento*:", formaPagto)
                .linha("Valor recebido (R$)*:", valorRecebido, "Retirada por:", retiradaPor)
                .linha("Observações:", obs, "Status:", status);
        return form;
    }

    @Override
    protected void atualizarListas(Recebimento atual) {
        if (atual == null) {
            // só ordens em execução podem ser entregues
            Campos.preencher(ordem, OrdemServico.listarPorStatus('e'), null);
        } else {
            OrdemServico os = atual.getOrdem();
            Campos.preencher(ordem, os == null ? Collections.<OrdemServico>emptyList() : Collections.singletonList(os), os);
        }
    }

    @Override
    protected void preencher(Recebimento r) {
        Campos.selecionar(ordem, r.getIdOrdem());
        Campos.setData(dataRecebimento, r.getDataRecebimento());
        Campos.selecionarCodigo(formaPagto, r.getFormaPagto());
        valorRecebido.setText(Formatos.valor(r.getValorRecebido()));
        retiradaPor.setText(r.getRetiradaPor());
        obs.setText(r.getObs());
        status.setText(r.getStatusDescricao());
        mostrarConsulta();
    }

    @Override
    protected void limpar() {
        ordem.setSelectedIndex(-1);
        consulta.setText("");
        Campos.setData(dataRecebimento, null);
        formaPagto.setSelectedIndex(-1);
        valorRecebido.setText("");
        retiradaPor.setText("");
        obs.setText("");
        status.setText("");
    }

    @Override
    protected void cadastrar() {
        super.cadastrar();
        if (ordem.getItemCount() == 0) {
            Mensagens.info(this, "Não há ordens em execução para entregar.\n"
                    + "Na tela de Ordens de serviço, use \"Iniciar execução\" antes de registrar a entrega.");
            cancelar();
        }
    }

    @Override
    protected void valoresIniciais() {
        Campos.setData(dataRecebimento, LocalDate.now());
        formaPagto.setSelectedIndex(0);
        if (ordem.getItemCount() > 0) {
            ordem.setSelectedIndex(0);
        }
        status.setText("Novo");
    }

    @Override
    protected void habilitarCampos(boolean h) {
        ordem.setEnabled(h && isNovo());
        dataRecebimento.setEnabled(h);
        formaPagto.setEnabled(h);
        valorRecebido.setEnabled(h);
        retiradaPor.setEnabled(h);
        obs.setEnabled(h);
    }

    @Override
    protected void verificarEdicao(Recebimento r) {
        if (r.getStatus() == 'c') {
            throw new RegraNegocioException("Recebimento cancelado não pode ser editado.");
        }
    }

    @Override
    protected Recebimento lerFormulario() {
        Recebimento r = new Recebimento();
        OrdemServico os = (OrdemServico) ordem.getSelectedItem();
        r.setIdOrdem(os == null ? 0 : os.getId());
        r.setDataRecebimento(Formatos.lerData(dataRecebimento.getText(), "a data do recebimento", true));
        r.setFormaPagto(Campos.codigo(formaPagto));
        r.setValorRecebido(Formatos.lerDecimal(valorRecebido.getText(), "o valor recebido"));
        r.setRetiradaPor(retiradaPor.getText());
        r.setObs(obs.getText().trim());
        return r;
    }

    @Override
    protected void depoisDeSalvar(Recebimento r, boolean eraNovo) {
        if (eraNovo) {
            Mensagens.info(this, "Recebimento registrado. OS nº " + r.getIdOrdem() + " finalizada (entregue).");
        }
    }

    @Override
    protected List<JButton> botoesExtras() {
        return Arrays.asList(btEstornar, btRecibo);
    }

    @Override
    protected void atualizarBotoesExtras(Recebimento r, boolean editando) {
        btEstornar.setEnabled(r != null && r.getStatus() == 'a');
        btRecibo.setEnabled(r != null && r.getStatus() == 'a');
    }

    /** Ao escolher a ordem: mostra a consulta e sugere o valor total (pagamento à vista). */
    private void ordemSelecionada() {
        mostrarConsulta();
        OrdemServico os = (OrdemServico) ordem.getSelectedItem();
        if (isNovo() && os != null) {
            valorRecebido.setText(Formatos.valor(os.calcularValorTotal()));
            retiradaPor.setText(os.getNomeCliente());
        }
    }

    /** <<include>> Consultar serviço. */
    private void mostrarConsulta() {
        OrdemServico os = (OrdemServico) ordem.getSelectedItem();
        consulta.setText(os == null ? "" : os.getResumo());
        consulta.setCaretPosition(0);
    }

    private void estornar() {
        Recebimento r = Recebimento.buscar(getSelecionado().getId());
        if (!Mensagens.confirmar(this, "Cancelar este recebimento?\nA OS nº " + r.getIdOrdem() + " volta para \"Em execução\".")) {
            return;
        }
        try {
            r.cancelar();
            recarregar(r.getId());
        } catch (RegraNegocioException ex) {
            Mensagens.aviso(this, ex.getMessage());
        }
    }

    private void imprimirRecibo() {
        Recebimento r = getSelecionado();
        OrdemServico os = r.getOrdem();
        String texto = "RECIBO - COSTUREIRA IRACI\n"
                + "==============================================\n\n"
                + "Recebi de " + os.getNomeCliente() + " a importância de " + Formatos.dinheiro(r.getValorRecebido())
                + "\nreferente à OS nº " + os.getId() + ", paga em " + r.getFormaPagtoDescricao().toLowerCase(Formatos.BRASIL)
                + " em " + Formatos.data(r.getDataRecebimento()) + ".\n"
                + "Retirada por: " + r.getRetiradaPor() + "\n\n"
                + "----------------------------------------------\n"
                + os.getResumo()
                + "\n\n\n______________________________\nIraci - Costureira";
        mostrarTexto(this, "Recibo do recebimento nº " + r.getId(), texto);
    }

    /** Janela de consulta da ordem, com opção de imprimir. */
    public static void mostrarOrdem(Component pai, OrdemServico os) {
        mostrarTexto(pai, "Consultar serviço - OS nº " + os.getId(), "COSTUREIRA IRACI - ORDEM DE SERVIÇO\n"
                + "==============================================\n" + os.getResumo());
    }

    static void mostrarTexto(Component pai, String titulo, String texto) {
        Window dono = pai instanceof Window ? (Window) pai : javax.swing.SwingUtilities.getWindowAncestor(pai);
        JDialog d = new JDialog(dono, titulo, JDialog.ModalityType.APPLICATION_MODAL);
        JTextArea area = new JTextArea(texto);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setCaretPosition(0);
        JButton imprimir = new JButton("Imprimir");
        JButton fechar = new JButton("Fechar");
        imprimir.addActionListener(e -> {
            try {
                area.print();
            } catch (PrinterException ex) {
                Mensagens.erro(d, "Não foi possível imprimir: " + ex.getMessage());
            }
        });
        fechar.addActionListener(e -> d.dispose());
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botoes.add(imprimir);
        botoes.add(fechar);
        JScrollPane rolagem = new JScrollPane(area);
        rolagem.setPreferredSize(new Dimension(640, 480));
        d.getContentPane().add(rolagem, BorderLayout.CENTER);
        d.getContentPane().add(botoes, BorderLayout.SOUTH);
        d.pack();
        d.setLocationRelativeTo(pai);
        d.setVisible(true);
    }
}
