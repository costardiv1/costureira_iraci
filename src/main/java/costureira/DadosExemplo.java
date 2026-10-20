package costureira;

import costureira.model.Cliente;
import costureira.model.ItemMaterial;
import costureira.model.ItemOrdemServico;
import costureira.model.Material;
import costureira.model.Medida;
import costureira.model.OrdemServico;
import costureira.model.Recebimento;
import costureira.model.Servico;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Dados fictícios para demonstração (oferecidos na primeira vez que o sistema abre).
 * Tudo é gravado pelos métodos salvar(), passando pelas mesmas regras de negócio.
 */
public final class DadosExemplo {

    private DadosExemplo() {
    }

    public static void carregar() {
        LocalDate hoje = LocalDate.now();

        Cliente maria = cliente("Maria Aparecida Souza", "123.456.789-09", "(16) 99812-3456", "maria.souza@email.com", 'f');
        Cliente joao = cliente("João Pedro Lima", "987.654.321-00", "(16) 3322-1144", "", 'm');
        Cliente ana = cliente("Ana Carolina Ribeiro", "456.123.789-55", "(16) 99701-2233", "ana.ribeiro@email.com", 'f');
        Cliente carlos = cliente("Carlos Eduardo Martins", "321.654.987-91", "(16) 98123-7788", "", 'n');

        medida(maria, "Cintura", 74, hoje.minusDays(20));
        medida(maria, "Quadril", 102, hoje.minusDays(20));
        medida(maria, "Comprimento da calça", 98, hoje.minusDays(20));
        medida(ana, "Busto", 88, hoje.minusDays(5));

        Servico barra = servico("Barra de calça", "Barra simples ou original", 25, 30);
        Servico ziper = servico("Troca de zíper", "Calça, saia ou jaqueta", 35, 40);
        Servico ajuste = servico("Ajuste de cintura", "Apertar ou soltar", 40, 60);
        Servico manga = servico("Encurtar manga", "Camisa ou blazer", 30, 45);
        Servico vestido = servico("Ajuste de vestido", "Laterais e alças", 80, 120);
        servico("Pregar botão", "Por botão", 5, 5);

        Material zip = material("Zíper", "YKK", "Preto", "un", 10, 6.5);
        Material zipInv = material("Zíper invisível", "Corrente", "Azul marinho", "un", 2, 7.9);
        Material linha = material("Linha de costura", "Coats", "Branca", "carretel", 15, 4.5);
        material("Elástico", "São José", "Branco", "m", 12, 2.2);
        material("Botão", "Genérico", "Preto", "un", 40, 0.5);

        // finalizada e recebida
        OrdemServico os1 = ordem(maria, hoje.minusDays(12), hoje.minusDays(9), "Cliente pediu urgência");
        os1.getItens().add(new ItemOrdemServico(barra, "Calça jeans azul", 2));
        os1.getItens().add(new ItemOrdemServico(ziper, "Calça social preta", 1));
        os1.getMateriais().add(new ItemMaterial(zip, 1));
        os1.salvar();
        os1.iniciarExecucao();
        receber(os1, hoje.minusDays(9), "p");

        OrdemServico os2 = ordem(joao, hoje.minusDays(8), hoje.minusDays(6), null);
        os2.getItens().add(new ItemOrdemServico(manga, "Camisa social branca", 3));
        os2.salvar();
        os2.iniciarExecucao();
        receber(os2, hoje.minusDays(5), "d");

        // em execução e atrasada
        OrdemServico os3 = ordem(ana, hoje.minusDays(6), hoje.minusDays(1), "Prova marcada");
        os3.getItens().add(new ItemOrdemServico(vestido, "Vestido de festa verde", 1));
        os3.getMateriais().add(new ItemMaterial(zipInv, 1));
        os3.getMateriais().add(new ItemMaterial(linha, 1));
        os3.salvar();
        os3.iniciarExecucao();

        // em execução no prazo
        OrdemServico os4 = ordem(maria, hoje.minusDays(2), hoje.plusDays(1), null);
        os4.getItens().add(new ItemOrdemServico(ajuste, "Saia lápis cinza", 1));
        os4.getItens().add(new ItemOrdemServico(barra, "Calça de linho bege", 1));
        os4.salvar();
        os4.iniciarExecucao();

        // agendada
        OrdemServico os5 = ordem(carlos, hoje, hoje.plusDays(4), "Vai trazer as peças à tarde");
        os5.getItens().add(new ItemOrdemServico(barra, "Calça de sarja", 2));
        os5.salvar();

        // cancelada
        OrdemServico os6 = ordem(joao, hoje.minusDays(3), hoje.plusDays(2), "Cliente desistiu");
        os6.getItens().add(new ItemOrdemServico(ziper, "Jaqueta de couro", 1));
        os6.salvar();
        os6.cancelar();
    }

    private static Cliente cliente(String nome, String cpf, String fone, String email, char genero) {
        Cliente c = new Cliente();
        c.setNome(nome);
        c.setCpf(cpf);
        c.setTelefone(fone);
        c.setEmail(email);
        c.setGenero(genero);
        c.setEndereco("Rua das Flores, " + (100 + nome.length()));
        c.salvar();
        return c;
    }

    private static void medida(Cliente c, String tipo, double valor, LocalDate data) {
        Medida m = new Medida();
        m.setIdCliente(c.getId());
        m.setTipoMedida(tipo);
        m.setValor(valor);
        m.setDataAfericao(data);
        m.salvar();
    }

    private static Servico servico(String nome, String descricao, double valor, int minutos) {
        Servico s = new Servico();
        s.setNome(nome);
        s.setDescricao(descricao);
        s.setValorPadrao(valor);
        s.setTempoEstimado(minutos);
        s.salvar();
        return s;
    }

    private static Material material(String tipo, String marca, String cor, String unidade, double qtd, double valor) {
        Material m = new Material();
        m.setTipo(tipo);
        m.setMarca(marca);
        m.setCor(cor);
        m.setUnidade(unidade);
        m.setQtdEstoque(qtd);
        m.setValorProduto(valor);
        m.salvar();
        return m;
    }

    private static OrdemServico ordem(Cliente c, LocalDate data, LocalDate prevista, String obs) {
        OrdemServico os = new OrdemServico();
        os.setIdCliente(c.getId());
        os.setDataAgendamento(data);
        os.setHoraAgendamento(LocalTime.of(9 + c.getId(), 30));
        os.setDataPrevistaEntrega(prevista);
        os.setObs(obs);
        return os;
    }

    private static void receber(OrdemServico os, LocalDate data, String forma) {
        Recebimento r = new Recebimento();
        r.setIdOrdem(os.getId());
        r.setDataRecebimento(data);
        r.setFormaPagto(forma);
        r.setValorRecebido(os.calcularValorTotal());
        r.salvar();
    }
}
