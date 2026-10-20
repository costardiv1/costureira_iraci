package costureira.view;

import costureira.model.Cliente;
import costureira.model.ItemMaterial;
import costureira.model.ItemOrdemServico;
import costureira.model.Material;
import costureira.model.OrdemServico;
import costureira.model.RegraNegocioException;
import costureira.model.Servico;
import costureira.util.Copia;
import costureira.util.Formatos;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Movimentação de ordem de serviço / agendamento
 * (casos de uso "Cadastrar serviço" <<include>> "Adicionar itens do serviço",
 * e "Cadastrar materiais" <<extend>> "Adicionar itens do serviço").
 */
public class TelaOrdens extends TelaCadastro<OrdemServico> {

    private static final long serialVersionUID = 1L;

    private final JComboBox<Cliente> cliente = new JComboBox<>();
    private final JButton btNovoCliente = new JButton("Novo cliente...");
    private final JFormattedTextField dataAgendamento = Campos.data();
    private final JFormattedTextField horaAgendamento = Campos.hora();
    private final JFormattedTextField dataPrevista = Campos.data();
    private final JTextField status = Campos.texto(12);
    private final JTextField entrega = Campos.texto(25);
    private final JTextArea obs = new JTextArea(2, 30);

    // itens (peças + serviços)
    private final List<ItemOrdemServico> itens = new ArrayList<>();
    private final DefaultTableModel modeloItens = tabelaSomenteLeitura("Peça", "Serviço", "Qtd.", "Valor unit.", "Subtotal");
    private final JTable tabelaItens = new JTable(modeloItens);
    private final JComboBox<Servico> servico = new JComboBox<>();
    private final JTextField descricaoPeca = Campos.texto(18);
    private final JSpinner quantidade = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
    private final JButton btAdicionarItem = new JButton("Adicionar item");
    private final JButton btRemoverItem = new JButton("Remover item");

    // materiais
    private final List<ItemMaterial> materiais = new ArrayList<>();
    private final DefaultTableModel modeloMateriais = tabelaSomenteLeitura("Material", "Qtd.", "Unidade", "Valor unit.", "Subtotal");
    private final JTable tabelaMateriais = new JTable(modeloMateriais);
    private final JComboBox<Material> material = new JComboBox<>();
    private final JTextField qtdMaterial = Campos.texto(6);
    private final JButton btAdicionarMaterial = new JButton("Adicionar material");
    private final JButton btRemoverMaterial = new JButton("Remover material");
    private final JButton btNovoMaterial = new JButton("Novo material...");

    private final JLabel total = new JLabel();

    // botões de movimentação
    private final JButton btIniciar = new JButton("Iniciar execução");
    private final JButton btCancelarOs = new JButton("Cancelar OS");
    private final JButton btConsultar = new JButton("Consultar / imprimir");

    public TelaOrdens(Window dono) {
        super(dono, "Ordens de serviço / agendamentos");
        status.setEditable(false);
        entrega.setEditable(false);
        obs.setLineWrap(true);
        obs.setWrapStyleWord(true);
        total.setFont(total.getFont().deriveFont(Font.BOLD, 15f));

        btAdicionarItem.addActionListener(e -> adicionarItem());
        btRemoverItem.addActionListener(e -> removerSelecionado(tabelaItens, itens));
        btAdicionarMaterial.addActionListener(e -> adicionarMaterial());
        btRemoverMaterial.addActionListener(e -> removerSelecionado(tabelaMateriais, materiais));
        btNovoMaterial.addActionListener(e -> novoMaterial());
        btNovoCliente.addActionListener(e -> novoCliente());
        btIniciar.addActionListener(e -> iniciarExecucao());
        btCancelarOs.addActionListener(e -> cancelarOrdem());
        btConsultar.addActionListener(e -> consultar());

        montar();
        setSize(1100, 760);
        setLocationRelativeTo(dono);
    }

    private static DefaultTableModel tabelaSomenteLeitura(String... colunas) {
        return new DefaultTableModel(colunas, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };
    }

    @Override
    protected String[] colunas() {
        return new String[]{"Nº", "Cliente", "Agendamento", "Entrega prevista", "Status", "Valor total", "Entregue em"};
    }

    @Override
    protected int[] largurasColunas() {
        return new int[]{40, 230, 120, 110, 120, 100, 100};
    }

    @Override
    protected Object[] linha(OrdemServico os) {
        String situacao = os.getStatusDescricao() + (os.isAtrasada() ? " (ATRASADA)" : "");
        return new Object[]{os.getId(), os.getNomeCliente(),
            Formatos.data(os.getDataAgendamento()) + " " + Formatos.hora(os.getHoraAgendamento()),
            Formatos.data(os.getDataPrevistaEntrega()), situacao,
            Formatos.dinheiro(os.calcularValorTotal()), Formatos.data(os.getDataEntrega())};
    }

    @Override
    protected List<OrdemServico> listar() {
        return OrdemServico.listar();
    }

    @Override
    protected Color corLinha(OrdemServico os) {
        if (os.isAtrasada()) {
            return Cores.ATRASADO;
        }
        switch (os.getStatus()) {
            case 'e': return Cores.EXECUCAO;
            case 'f': return Cores.FINALIZADO;
            case 'c': return Cores.CANCELADO;
            default: return null;
        }
    }

    @Override
    protected String textoRodape(List<OrdemServico> registros) {
        int atrasadas = 0;
        for (OrdemServico os : registros) {
            if (os.isAtrasada()) {
                atrasadas++;
            }
        }
        return registros.size() + " ordem(ns)" + (atrasadas > 0 ? " — " + atrasadas + " ATRASADA(S) em vermelho" : "");
    }

    @Override
    protected JComponent criarFormulario() {
        JPanel linhaCliente = new JPanel(new BorderLayout(4, 0));
        linhaCliente.add(cliente, BorderLayout.CENTER);
        linhaCliente.add(btNovoCliente, BorderLayout.EAST);

        PainelForm cabecalho = new PainelForm()
                .linha("Cliente*:", linhaCliente)
                .linha("Data agendamento*:", dataAgendamento, "Hora*:", horaAgendamento)
                .linha("Entrega prevista*:", dataPrevista, "Status:", status)
                .linha("Entrega / retirada:", entrega)
                .linha("Observações:", new JScrollPane(obs));

        // aba de itens
        JPanel addItem = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addItem.add(new JLabel("Serviço:"));
        servico.setPreferredSize(new Dimension(240, servico.getPreferredSize().height));
        addItem.add(servico);
        addItem.add(new JLabel("Peça:"));
        addItem.add(descricaoPeca);
        addItem.add(new JLabel("Qtd.:"));
        addItem.add(quantidade);
        addItem.add(btAdicionarItem);
        addItem.add(btRemoverItem);
        JPanel abaItens = new JPanel(new BorderLayout());
        abaItens.add(addItem, BorderLayout.NORTH);
        abaItens.add(new JScrollPane(tabelaItens), BorderLayout.CENTER);

        // aba de materiais
        JPanel addMaterial = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addMaterial.add(new JLabel("Material:"));
        material.setPreferredSize(new Dimension(300, material.getPreferredSize().height));
        addMaterial.add(material);
        addMaterial.add(new JLabel("Qtd.:"));
        addMaterial.add(qtdMaterial);
        addMaterial.add(btAdicionarMaterial);
        addMaterial.add(btRemoverMaterial);
        addMaterial.add(btNovoMaterial);
        JPanel abaMateriais = new JPanel(new BorderLayout());
        abaMateriais.add(addMaterial, BorderLayout.NORTH);
        abaMateriais.add(new JScrollPane(tabelaMateriais), BorderLayout.CENTER);

        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Itens do serviço (peças)*", abaItens);
        abas.addTab("Materiais utilizados", abaMateriais);
        abas.setPreferredSize(new Dimension(600, 190));

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rodape.add(total);

        JPanel painel = new JPanel(new BorderLayout());
        painel.add(cabecalho, BorderLayout.NORTH);
        painel.add(abas, BorderLayout.CENTER);
        painel.add(rodape, BorderLayout.SOUTH);
        painel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        return painel;
    }

    @Override
    protected void atualizarListas(OrdemServico atual) {
        Cliente doRegistro = atual == null ? null : Cliente.buscar(atual.getIdCliente());
        Campos.preencher(cliente, Cliente.listarAtivos(), doRegistro);
        Campos.preencher(servico, Servico.listarAtivos(), null);
        Campos.preencher(material, Material.listarAtivos(), null);
        selecionarPrimeiro(servico);
        selecionarPrimeiro(material);
    }

    private static void selecionarPrimeiro(JComboBox<?> combo) {
        if (combo.getItemCount() > 0) {
            combo.setSelectedIndex(0);
        }
    }

    @Override
    protected void preencher(OrdemServico os) {
        Campos.selecionar(cliente, os.getIdCliente());
        Campos.setData(dataAgendamento, os.getDataAgendamento());
        Campos.setHora(horaAgendamento, os.getHoraAgendamento());
        Campos.setData(dataPrevista, os.getDataPrevistaEntrega());
        status.setText(os.getStatusDescricao() + (os.isAtrasada() ? " - ATRASADA" : ""));
        if (os.getDataEntrega() != null) {
            entrega.setText(Formatos.data(os.getDataEntrega())
                    + (Formatos.vazio(os.getRetiradaPor()) ? "" : " - retirada por " + os.getRetiradaPor()));
        } else {
            entrega.setText("");
        }
        obs.setText(os.getObs());
        itens.clear();
        for (ItemOrdemServico item : os.getItens()) {
            itens.add(Copia.de(item));
        }
        materiais.clear();
        for (ItemMaterial im : os.getMateriais()) {
            materiais.add(Copia.de(im));
        }
        atualizarTabelas();
    }

    @Override
    protected void limpar() {
        cliente.setSelectedIndex(-1);
        Campos.setData(dataAgendamento, null);
        Campos.setHora(horaAgendamento, null);
        Campos.setData(dataPrevista, null);
        status.setText("");
        entrega.setText("");
        obs.setText("");
        itens.clear();
        materiais.clear();
        descricaoPeca.setText("");
        quantidade.setValue(1);
        qtdMaterial.setText("");
        atualizarTabelas();
    }

    @Override
    protected void valoresIniciais() {
        Campos.setData(dataAgendamento, LocalDate.now());
        Campos.setHora(horaAgendamento, LocalTime.now().withSecond(0).withNano(0));
        Campos.setData(dataPrevista, LocalDate.now().plusDays(3));
        status.setText("Agendado");
    }

    @Override
    protected void habilitarCampos(boolean h) {
        for (JComponent c : Arrays.<JComponent>asList(cliente, btNovoCliente, dataAgendamento, horaAgendamento, dataPrevista, obs,
                servico, descricaoPeca, quantidade, btAdicionarItem, btRemoverItem,
                material, qtdMaterial, btAdicionarMaterial, btRemoverMaterial, btNovoMaterial)) {
            c.setEnabled(h);
        }
    }

    @Override
    protected void verificarEdicao(OrdemServico os) {
        if (os.getStatus() == 'f' || os.getStatus() == 'c') {
            throw new RegraNegocioException("Ordem finalizada ou cancelada não pode mais ser editada.\nUse \"Consultar / imprimir\" para ver os dados.");
        }
    }

    @Override
    protected OrdemServico lerFormulario() {
        OrdemServico os = new OrdemServico();
        Cliente c = (Cliente) cliente.getSelectedItem();
        os.setIdCliente(c == null ? 0 : c.getId());
        os.setDataAgendamento(Formatos.lerData(dataAgendamento.getText(), "a data do agendamento", true));
        os.setHoraAgendamento(Formatos.lerHora(horaAgendamento.getText(), "a hora do agendamento", true));
        os.setDataPrevistaEntrega(Formatos.lerData(dataPrevista.getText(), "a data prevista de entrega", true));
        os.setObs(obs.getText().trim());
        os.setItens(itens);
        os.setMateriais(materiais);
        return os;
    }

    @Override
    protected List<JButton> botoesExtras() {
        return Arrays.asList(btIniciar, btCancelarOs, btConsultar);
    }

    @Override
    protected void atualizarBotoesExtras(OrdemServico os, boolean editando) {
        btIniciar.setEnabled(os != null && os.getStatus() == 'a');
        btCancelarOs.setEnabled(os != null && os.isPendente());
        btConsultar.setEnabled(os != null);
    }

    // ---------- itens ----------

    /** <<include>> Adicionar itens do serviço. */
    private void adicionarItem() {
        Servico s = (Servico) servico.getSelectedItem();
        if (s == null) {
            Mensagens.aviso(this, "Selecione o serviço. (Cadastre os serviços no menu Cadastros > Serviços.)");
            return;
        }
        if (Formatos.vazio(descricaoPeca.getText())) {
            Mensagens.aviso(this, "Descreva a peça (ex.: Calça jeans azul).");
            descricaoPeca.requestFocus();
            return;
        }
        itens.add(new ItemOrdemServico(s, descricaoPeca.getText().trim(), (Integer) quantidade.getValue()));
        descricaoPeca.setText("");
        quantidade.setValue(1);
        atualizarTabelas();
    }

    /** <<extend>> materiais usados no serviço. */
    private void adicionarMaterial() {
        Material m = (Material) material.getSelectedItem();
        if (m == null) {
            Mensagens.aviso(this, "Selecione o material.");
            return;
        }
        double qtd;
        try {
            qtd = Formatos.lerDecimal(qtdMaterial.getText(), "a quantidade do material");
        } catch (RegraNegocioException ex) {
            Mensagens.aviso(this, ex.getMessage());
            return;
        }
        if (qtd <= 0) {
            Mensagens.aviso(this, "A quantidade deve ser maior que zero.");
            return;
        }
        materiais.add(new ItemMaterial(m, qtd));
        qtdMaterial.setText("");
        atualizarTabelas();
    }

    private void removerSelecionado(JTable tabela, List<?> lista) {
        int linha = tabela.getSelectedRow();
        if (linha < 0) {
            Mensagens.aviso(this, "Selecione a linha que deseja remover.");
            return;
        }
        lista.remove(linha);
        atualizarTabelas();
    }

    private void novoMaterial() {
        new TelaMateriais(this).setVisible(true);
        Campos.preencher(material, Material.listarAtivos(), null);
        selecionarPrimeiro(material);
    }

    private void novoCliente() {
        Cliente atual = (Cliente) cliente.getSelectedItem();
        new TelaClientes(this).setVisible(true);
        Campos.preencher(cliente, Cliente.listarAtivos(), atual);
    }

    private void atualizarTabelas() {
        modeloItens.setRowCount(0);
        double totalItens = 0;
        for (ItemOrdemServico item : itens) {
            modeloItens.addRow(new Object[]{item.getDescricaoPeca(), item.getNomeServico(), item.getQuantidade(),
                Formatos.dinheiro(item.getValorUnitario()), Formatos.dinheiro(item.getSubtotal())});
            totalItens += item.getSubtotal();
        }
        modeloMateriais.setRowCount(0);
        double totalMateriais = 0;
        for (ItemMaterial im : materiais) {
            modeloMateriais.addRow(new Object[]{im.getDescricaoMaterial(), Formatos.numero(im.getQuantidade()), im.getUnidade(),
                Formatos.dinheiro(im.getValorUnitario()), Formatos.dinheiro(im.getSubtotal())});
            totalMateriais += im.getSubtotal();
        }
        total.setText("Serviços: " + Formatos.dinheiro(totalItens) + "   Materiais: " + Formatos.dinheiro(totalMateriais)
                + "   VALOR TOTAL: " + Formatos.dinheiro(totalItens + totalMateriais));
    }

    // ---------- movimentação ----------

    private void iniciarExecucao() {
        OrdemServico os = OrdemServico.buscar(getSelecionado().getId());
        try {
            os.iniciarExecucao();
            recarregar(os.getId());
        } catch (RegraNegocioException ex) {
            Mensagens.aviso(this, ex.getMessage());
        }
    }

    /** cancelar() da ordem: muda o status para Cancelado e devolve os materiais ao estoque. */
    private void cancelarOrdem() {
        OrdemServico os = OrdemServico.buscar(getSelecionado().getId());
        if (!Mensagens.confirmar(this, "Cancelar a OS nº " + os.getId() + " de " + os.getNomeCliente() + "?\n"
                + "Os materiais voltam para o estoque e a ordem não poderá mais ser editada.")) {
            return;
        }
        try {
            os.cancelar();
            recarregar(os.getId());
        } catch (RegraNegocioException ex) {
            Mensagens.aviso(this, ex.getMessage());
        }
    }

    private void consultar() {
        TelaRecebimentos.mostrarOrdem(this, OrdemServico.buscar(getSelecionado().getId()));
    }
}
