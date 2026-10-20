package costureira.view;

import costureira.dao.BancoDados;
import costureira.model.OrdemServico;
import costureira.model.Recebimento;
import costureira.util.Formatos;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.time.LocalDate;
import java.util.function.Function;

/** Janela principal: menu, atalhos para os casos de uso e um resumo do dia. */
public class TelaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;

    private final JLabel agendadas = indicador();
    private final JLabel emExecucao = indicador();
    private final JLabel atrasadas = indicador();
    private final JLabel recebidoMes = indicador();

    public TelaPrincipal() {
        super("Controle de Serviços - Costureira Iraci");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setJMenuBar(criarMenu());

        JLabel titulo = new JLabel("Sistema de Controle de Serviços - Costureira Iraci", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 22f));
        titulo.setBorder(BorderFactory.createEmptyBorder(16, 0, 12, 0));

        JPanel indicadores = new JPanel(new GridLayout(1, 4, 10, 0));
        indicadores.add(caixa("Agendadas", agendadas, new Color(225, 235, 250)));
        indicadores.add(caixa("Em execução", emExecucao, Cores.EXECUCAO));
        indicadores.add(caixa("Entregas atrasadas", atrasadas, Cores.ATRASADO));
        indicadores.add(caixa("Recebido no mês", recebidoMes, Cores.FINALIZADO));

        JPanel atalhos = new JPanel(new GridLayout(2, 4, 10, 10));
        atalhos.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        atalhos.add(atalho("Cadastrar cliente", TelaClientes::new));
        atalhos.add(atalho("Cadastrar medidas", dono -> new TelaMedidas(dono, null)));
        atalhos.add(atalho("Cadastrar materiais", TelaMateriais::new));
        atalhos.add(atalho("Serviços oferecidos", TelaServicos::new));
        atalhos.add(atalho("<html><center>Cadastrar serviço<br>(ordem de serviço)</center></html>", TelaOrdens::new));
        atalhos.add(atalho("Registrar recebimento", TelaRecebimentos::new));
        atalhos.add(atalho("Entregas atrasadas", dono -> new TelaRelatorios(dono, 4)));
        atalhos.add(atalho("Relatórios", TelaRelatorios::new));

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        centro.add(indicadores, BorderLayout.NORTH);
        centro.add(atalhos, BorderLayout.CENTER);

        JLabel rodape = new JLabel("Dados gravados em: " + BancoDados.getArquivo().getAbsolutePath(), SwingConstants.CENTER);
        rodape.setForeground(Color.GRAY);
        rodape.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        getContentPane().add(titulo, BorderLayout.NORTH);
        getContentPane().add(centro, BorderLayout.CENTER);
        getContentPane().add(rodape, BorderLayout.SOUTH);

        atualizarIndicadores();
        setSize(new Dimension(980, 520));
        setLocationRelativeTo(null);
    }

    private JMenuBar criarMenu() {
        JMenuBar barra = new JMenuBar();

        JMenu cadastros = new JMenu("Cadastros");
        cadastros.add(item("Clientes", TelaClientes::new));
        cadastros.add(item("Medidas dos clientes", dono -> new TelaMedidas(dono, null)));
        cadastros.add(item("Serviços oferecidos", TelaServicos::new));
        cadastros.add(item("Materiais", TelaMateriais::new));
        cadastros.addSeparator();
        JMenuItem sair = new JMenuItem("Sair");
        sair.addActionListener(e -> dispose());
        cadastros.add(sair);

        JMenu movimentacoes = new JMenu("Movimentações");
        movimentacoes.add(item("Ordens de serviço / agendamentos", TelaOrdens::new));
        movimentacoes.add(item("Recebimentos (entrega)", TelaRecebimentos::new));

        JMenu relatorios = new JMenu("Relatórios");
        int i = 0;
        for (costureira.relatorio.Relatorio r : costureira.relatorio.Relatorios.todos()) {
            final int indice = i++;
            relatorios.add(item(r.getNome(), dono -> new TelaRelatorios(dono, indice)));
        }

        JMenu ajuda = new JMenu("Ajuda");
        JMenuItem sobre = new JMenuItem("Sobre");
        sobre.addActionListener(e -> Mensagens.info(this, "Sistema de Controle de Serviços - Costureira Iraci\n"
                + "Trabalho acadêmico - Vinícius Mendonça Costardi\n\n"
                + "Cadastros: clientes, medidas, serviços e materiais.\n"
                + "Movimentações: ordens de serviço/agendamentos e recebimentos (à vista, na entrega).\n"
                + "Relatórios: 7 relatórios de gestão."));
        ajuda.add(sobre);

        barra.add(cadastros);
        barra.add(movimentacoes);
        barra.add(relatorios);
        barra.add(ajuda);
        return barra;
    }

    private JMenuItem item(String texto, Function<Window, java.awt.Dialog> tela) {
        JMenuItem item = new JMenuItem(texto);
        item.addActionListener(e -> abrir(tela));
        return item;
    }

    private JButton atalho(String texto, Function<Window, java.awt.Dialog> tela) {
        JButton b = new JButton(texto);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
        b.setFocusPainted(false);
        b.addActionListener(e -> abrir(tela));
        return b;
    }

    private void abrir(Function<Window, java.awt.Dialog> tela) {
        tela.apply(this).setVisible(true); // modal: volta aqui quando a tela é fechada
        atualizarIndicadores();
    }

    private static JLabel indicador() {
        JLabel l = new JLabel("0", SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 26f));
        return l;
    }

    private static JPanel caixa(String rotulo, JLabel valor, Color fundo) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(fundo);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(fundo.darker()),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        JLabel r = new JLabel(rotulo, SwingConstants.CENTER);
        p.add(r, BorderLayout.NORTH);
        p.add(valor, BorderLayout.CENTER);
        return p;
    }

    public void atualizarIndicadores() {
        int a = 0;
        int e = 0;
        int atr = 0;
        for (OrdemServico os : OrdemServico.listar()) {
            if (os.getStatus() == 'a') {
                a++;
            } else if (os.getStatus() == 'e') {
                e++;
            }
            if (os.isAtrasada()) {
                atr++;
            }
        }
        double mes = 0;
        LocalDate hoje = LocalDate.now();
        for (Recebimento r : Recebimento.listar()) {
            if (r.getStatus() == 'a' && r.getDataRecebimento().getYear() == hoje.getYear()
                    && r.getDataRecebimento().getMonth() == hoje.getMonth()) {
                mes += r.getValorRecebido();
            }
        }
        agendadas.setText(String.valueOf(a));
        emExecucao.setText(String.valueOf(e));
        atrasadas.setText(String.valueOf(atr));
        atrasadas.setForeground(atr > 0 ? new Color(170, 0, 0) : Color.BLACK);
        recebidoMes.setText(Formatos.dinheiro(mes));
    }
}
