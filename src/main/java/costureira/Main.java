package costureira;

import costureira.dao.BancoDados;
import costureira.view.Mensagens;
import costureira.view.TelaPrincipal;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.Locale;

/**
 * Sistema de Controle de Serviços - Costureira Iraci.
 * Ponto de entrada: abre a janela principal.
 */
public class Main {

    public static void main(String[] args) {
        Locale.setDefault(new Locale("pt", "BR"));
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // continua com o visual padrão do Java
            }
            BancoDados bd = BancoDados.get();
            TelaPrincipal tela = new TelaPrincipal();
            tela.setVisible(true);
            if (bd.isVazio() && Mensagens.confirmar(tela, "O sistema ainda não tem dados.\n"
                    + "Deseja carregar dados de exemplo (clientes, serviços, materiais e ordens) para testar?")) {
                DadosExemplo.carregar();
                tela.atualizarIndicadores();
            }
        });
    }
}
