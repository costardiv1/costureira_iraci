package costureira.relatorio;

import costureira.model.Cliente;
import costureira.model.ItemOrdemServico;
import costureira.model.Material;
import costureira.model.OrdemServico;
import costureira.model.Recebimento;
import costureira.model.RegraNegocioException;
import costureira.util.Descricoes;
import costureira.util.Formatos;
import costureira.view.Campos;
import costureira.view.Campos.Opcao;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.FlowLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Relatórios de gestão da Iraci (seção 3 do documento de requisitos).
 */
public final class Relatorios {

    private Relatorios() {
    }

    public static List<Relatorio> todos() {
        return Arrays.asList(
                new OrdensPorPeriodo(),
                new ServicosMaisSolicitados(),
                new RecebimentosPorPeriodo(),
                new ClientesQueMaisUtilizaram(),
                new EntregasPendentes(),
                new ClientesCadastrados(),
                new EstoqueMateriais());
    }

    // ------------------------------------------------------------------ filtros comuns

    /** Filtro "de dd/mm/aaaa até dd/mm/aaaa" (padrão: do início do mês passado ao fim do mês atual). */
    static class Periodo {
        final JFormattedTextField inicio = Campos.data();
        final JFormattedTextField fim = Campos.data();

        Periodo() {
            LocalDate hoje = LocalDate.now();
            Campos.setData(inicio, hoje.minusMonths(1).withDayOfMonth(1));
            Campos.setData(fim, hoje.with(TemporalAdjusters.lastDayOfMonth()));
        }

        void adicionar(JPanel painel, String rotulo) {
            painel.add(new JLabel(rotulo + " de"));
            painel.add(inicio);
            painel.add(new JLabel("até"));
            painel.add(fim);
        }

        LocalDate getInicio() {
            return Formatos.lerData(inicio.getText(), "a data inicial", true);
        }

        LocalDate getFim() {
            LocalDate f = Formatos.lerData(fim.getText(), "a data final", true);
            if (f.isBefore(getInicio())) {
                throw new RegraNegocioException("A data final não pode ser anterior à data inicial.");
            }
            return f;
        }

        boolean contem(LocalDate data) {
            return data != null && !data.isBefore(getInicio()) && !data.isAfter(getFim());
        }

        String texto() {
            return Formatos.data(getInicio()) + " a " + Formatos.data(getFim());
        }
    }

    static JPanel painel() {
        return new JPanel(new FlowLayout(FlowLayout.LEFT));
    }

    // ------------------------------------------------------------------ 1

    /** Ordens de serviço por período e situação, agrupadas por dia, semana ou mês. */
    static class OrdensPorPeriodo extends Relatorio {
        private final Periodo periodo = new Periodo();
        private final JComboBox<Opcao> status = Campos.comboOpcoes(new Opcao("", "Todas"),
                new Opcao("a", "Agendado"), new Opcao("e", "Em execução"), new Opcao("f", "Finalizado"), new Opcao("c", "Cancelado"));
        private final JComboBox<String> agrupar = new JComboBox<>(new String[]{"Detalhado", "Por dia", "Por semana", "Por mês"});
        private final JPanel filtros = painel();

        OrdensPorPeriodo() {
            super("1. Ordens de serviço por período e situação");
            periodo.adicionar(filtros, "Agendamento");
            filtros.add(new JLabel("  Situação:"));
            filtros.add(status);
            filtros.add(new JLabel("  Agrupar:"));
            filtros.add(agrupar);
        }

        @Override
        public JComponent getFiltros() {
            return filtros;
        }

        @Override
        public Resultado gerar() {
            String filtroStatus = Campos.codigo(status);
            List<OrdemServico> ordens = new ArrayList<>();
            for (OrdemServico os : OrdemServico.listar()) {
                if (periodo.contem(os.getDataAgendamento())
                        && (filtroStatus.isEmpty() || os.getStatus() == filtroStatus.charAt(0))) {
                    ordens.add(os);
                }
            }
            ordens.sort(Comparator.comparing(OrdemServico::getDataAgendamento).thenComparing(OrdemServico::getHoraAgendamento));

            double soma = 0;
            for (OrdemServico os : ordens) {
                soma += os.calcularValorTotal();
            }
            String titulo = getNome() + " — " + periodo.texto() + " — situação: " + status.getSelectedItem();
            Resultado r;
            int modo = agrupar.getSelectedIndex();
            if (modo == 0) {
                r = new Resultado(titulo, "Nº", "Data", "Hora", "Cliente", "Situação", "Entrega prevista", "Valor total");
                for (OrdemServico os : ordens) {
                    r.linha(os.getId(), Formatos.data(os.getDataAgendamento()), Formatos.hora(os.getHoraAgendamento()),
                            os.getNomeCliente(), os.getStatusDescricao(), Formatos.data(os.getDataPrevistaEntrega()),
                            Formatos.dinheiro(os.calcularValorTotal()));
                }
            } else {
                r = new Resultado(titulo, "Período", "Agendadas", "Em execução", "Finalizadas", "Canceladas", "Total de ordens", "Valor total");
                Map<LocalDate, int[]> contagem = new TreeMap<>();
                Map<LocalDate, Double> valores = new TreeMap<>();
                for (OrdemServico os : ordens) {
                    LocalDate chave = chave(os.getDataAgendamento(), modo);
                    int[] c = contagem.computeIfAbsent(chave, k -> new int[4]);
                    c["aefc".indexOf(os.getStatus())]++;
                    valores.merge(chave, os.calcularValorTotal(), Double::sum);
                }
                for (Map.Entry<LocalDate, int[]> e : contagem.entrySet()) {
                    int[] c = e.getValue();
                    r.linha(rotulo(e.getKey(), modo), c[0], c[1], c[2], c[3], c[0] + c[1] + c[2] + c[3],
                            Formatos.dinheiro(valores.get(e.getKey())));
                }
            }
            r.rodape = ordens.size() + " ordem(ns) — valor total: " + Formatos.dinheiro(soma);
            return r;
        }

        private static LocalDate chave(LocalDate data, int modo) {
            switch (modo) {
                case 2: return data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                case 3: return data.withDayOfMonth(1);
                default: return data;
            }
        }

        private static String rotulo(LocalDate chave, int modo) {
            switch (modo) {
                case 2: return "Semana " + Formatos.data(chave) + " a " + Formatos.data(chave.plusDays(6));
                case 3: return String.format("%02d/%d", chave.getMonthValue(), chave.getYear());
                default: return Formatos.data(chave);
            }
        }
    }

    // ------------------------------------------------------------------ 2

    /** Ranking dos serviços mais solicitados (soma de quantidade e de subtotal). */
    static class ServicosMaisSolicitados extends Relatorio {
        private final Periodo periodo = new Periodo();
        private final JPanel filtros = painel();

        ServicosMaisSolicitados() {
            super("2. Serviços mais solicitados (ranking)");
            periodo.adicionar(filtros, "Agendamento");
            filtros.add(new JLabel("  (ordens canceladas não entram)"));
        }

        @Override
        public JComponent getFiltros() {
            return filtros;
        }

        @Override
        public Resultado gerar() {
            Map<String, double[]> porServico = new LinkedHashMap<>(); // [qtd, valor, ordens]
            for (OrdemServico os : OrdemServico.listar()) {
                if (os.getStatus() == 'c' || !periodo.contem(os.getDataAgendamento())) {
                    continue;
                }
                List<String> contados = new ArrayList<>();
                for (ItemOrdemServico item : os.getItens()) {
                    double[] v = porServico.computeIfAbsent(item.getNomeServico(), k -> new double[3]);
                    v[0] += item.getQuantidade();
                    v[1] += item.getSubtotal();
                    if (!contados.contains(item.getNomeServico())) {
                        v[2]++;
                        contados.add(item.getNomeServico());
                    }
                }
            }
            List<Map.Entry<String, double[]>> ranking = new ArrayList<>(porServico.entrySet());
            ranking.sort((a, b) -> a.getValue()[0] != b.getValue()[0]
                    ? Double.compare(b.getValue()[0], a.getValue()[0])
                    : Double.compare(b.getValue()[1], a.getValue()[1]));

            Resultado r = new Resultado(getNome() + " — " + periodo.texto(),
                    "Posição", "Serviço", "Quantidade de peças", "Nº de ordens", "Valor total");
            int pos = 1;
            double qtd = 0;
            double soma = 0;
            for (Map.Entry<String, double[]> e : ranking) {
                double[] v = e.getValue();
                r.linha(pos++ + "º", e.getKey(), (int) v[0], (int) v[2], Formatos.dinheiro(v[1]));
                qtd += v[0];
                soma += v[1];
            }
            r.rodape = ranking.size() + " serviço(s) — " + (int) qtd + " peça(s) — " + Formatos.dinheiro(soma);
            return r;
        }
    }

    // ------------------------------------------------------------------ 3

    /** Recebimentos por período e forma de pagamento, com total do período. */
    static class RecebimentosPorPeriodo extends Relatorio {
        private final Periodo periodo = new Periodo();
        private final JComboBox<Opcao> forma = Campos.comboOpcoes(new Opcao("", "Todas"),
                new Opcao("d", "Dinheiro"), new Opcao("p", "PIX"), new Opcao("db", "Cartão de débito"), new Opcao("cr", "Cartão de crédito"));
        private final JPanel filtros = painel();

        RecebimentosPorPeriodo() {
            super("3. Recebimentos por período e forma de pagamento");
            periodo.adicionar(filtros, "Recebimento");
            filtros.add(new JLabel("  Forma de pagamento:"));
            filtros.add(forma);
        }

        @Override
        public JComponent getFiltros() {
            return filtros;
        }

        @Override
        public Resultado gerar() {
            String filtro = Campos.codigo(forma);
            List<Recebimento> lista = new ArrayList<>();
            for (Recebimento rec : Recebimento.listar()) {
                if (rec.getStatus() == 'a' && periodo.contem(rec.getDataRecebimento())
                        && (filtro.isEmpty() || filtro.equals(rec.getFormaPagto()))) {
                    lista.add(rec);
                }
            }
            lista.sort(Comparator.comparing(Recebimento::getDataRecebimento).thenComparing(Recebimento::getId));

            Resultado r = new Resultado(getNome() + " — " + periodo.texto() + " — " + forma.getSelectedItem(),
                    "Data", "Recebimento nº", "OS nº", "Cliente", "Forma de pagamento", "Valor recebido");
            double total = 0;
            Map<String, Double> porForma = new LinkedHashMap<>();
            for (String f : Descricoes.FORMAS_PAGTO) {
                porForma.put(f, 0.0);
            }
            for (Recebimento rec : lista) {
                OrdemServico os = rec.getOrdem();
                r.linha(Formatos.data(rec.getDataRecebimento()), rec.getId(), rec.getIdOrdem(),
                        os == null ? "" : os.getNomeCliente(), rec.getFormaPagtoDescricao(), Formatos.dinheiro(rec.getValorRecebido()));
                total += rec.getValorRecebido();
                porForma.merge(rec.getFormaPagto(), rec.getValorRecebido(), Double::sum);
            }
            if (!lista.isEmpty() && filtro.isEmpty()) {
                r.linha("", "", "", "", "", "");
                for (Map.Entry<String, Double> e : porForma.entrySet()) {
                    r.linha("", "", "", "Subtotal", Descricoes.formaPagto(e.getKey()), Formatos.dinheiro(e.getValue()));
                }
            }
            r.rodape = lista.size() + " recebimento(s) — TOTAL DO PERÍODO: " + Formatos.dinheiro(total);
            return r;
        }
    }

    // ------------------------------------------------------------------ 4

    /** Clientes que mais utilizaram os serviços: quantidade de ordens e soma do valor total. */
    static class ClientesQueMaisUtilizaram extends Relatorio {
        private final Periodo periodo = new Periodo();
        private final JPanel filtros = painel();

        ClientesQueMaisUtilizaram() {
            super("4. Clientes que mais utilizaram os serviços");
            periodo.adicionar(filtros, "Agendamento");
            filtros.add(new JLabel("  (ordens canceladas não entram)"));
        }

        @Override
        public JComponent getFiltros() {
            return filtros;
        }

        @Override
        public Resultado gerar() {
            Map<Integer, double[]> porCliente = new LinkedHashMap<>(); // [ordens, valor]
            for (OrdemServico os : OrdemServico.listar()) {
                if (os.getStatus() == 'c' || !periodo.contem(os.getDataAgendamento())) {
                    continue;
                }
                double[] v = porCliente.computeIfAbsent(os.getIdCliente(), k -> new double[2]);
                v[0]++;
                v[1] += os.calcularValorTotal();
            }
            List<Map.Entry<Integer, double[]>> ranking = new ArrayList<>(porCliente.entrySet());
            ranking.sort((a, b) -> a.getValue()[0] != b.getValue()[0]
                    ? Double.compare(b.getValue()[0], a.getValue()[0])
                    : Double.compare(b.getValue()[1], a.getValue()[1]));

            Resultado r = new Resultado(getNome() + " — " + periodo.texto(),
                    "Posição", "Cliente", "Telefone", "Nº de ordens", "Valor total", "Ticket médio");
            int pos = 1;
            double soma = 0;
            for (Map.Entry<Integer, double[]> e : ranking) {
                Cliente c = Cliente.buscar(e.getKey());
                double[] v = e.getValue();
                r.linha(pos++ + "º", c == null ? "(removido)" : c.getNome(), c == null ? "" : c.getTelefone(),
                        (int) v[0], Formatos.dinheiro(v[1]), Formatos.dinheiro(v[1] / v[0]));
                soma += v[1];
            }
            r.rodape = ranking.size() + " cliente(s) — " + Formatos.dinheiro(soma);
            return r;
        }
    }

    // ------------------------------------------------------------------ 5

    /** Entregas pendentes e atrasadas: ordens não finalizadas com entrega vencida ou próxima. */
    static class EntregasPendentes extends Relatorio {
        private final JSpinner dias = new JSpinner(new SpinnerNumberModel(2, 0, 60, 1));
        private final JCheckBox somenteAtrasadas = new JCheckBox("Somente atrasadas e próximas");
        private final JPanel filtros = painel();

        EntregasPendentes() {
            super("5. Entregas pendentes e atrasadas");
            filtros.add(new JLabel("Considerar \"próxima\" a entrega que vence em até"));
            filtros.add(dias);
            filtros.add(new JLabel("dia(s)   "));
            filtros.add(somenteAtrasadas);
        }

        @Override
        public JComponent getFiltros() {
            return filtros;
        }

        @Override
        public Resultado gerar() {
            int limite = (Integer) dias.getValue();
            LocalDate hoje = LocalDate.now();
            List<OrdemServico> pendentes = new ArrayList<>();
            for (OrdemServico os : OrdemServico.listar()) {
                if (os.isPendente()) {
                    long faltam = ChronoUnit.DAYS.between(hoje, os.getDataPrevistaEntrega());
                    if (!somenteAtrasadas.isSelected() || faltam <= limite) {
                        pendentes.add(os);
                    }
                }
            }
            pendentes.sort(Comparator.comparing(OrdemServico::getDataPrevistaEntrega));

            Resultado r = new Resultado(getNome() + " — posição em " + Formatos.data(hoje),
                    "OS nº", "Cliente", "Telefone", "Situação da ordem", "Entrega prevista", "Prazo", "Valor total");
            int atrasadas = 0;
            int proximas = 0;
            double soma = 0;
            for (OrdemServico os : pendentes) {
                long faltam = ChronoUnit.DAYS.between(hoje, os.getDataPrevistaEntrega());
                String prazo;
                if (faltam < 0) {
                    prazo = "ATRASADA há " + (-faltam) + " dia(s)";
                    atrasadas++;
                } else if (faltam == 0) {
                    prazo = "Vence HOJE";
                    proximas++;
                } else if (faltam <= limite) {
                    prazo = "Vence em " + faltam + " dia(s)";
                    proximas++;
                } else {
                    prazo = "No prazo (" + faltam + " dias)";
                }
                Cliente c = os.getCliente();
                r.linha(os.getId(), os.getNomeCliente(), c == null ? "" : c.getTelefone(), os.getStatusDescricao(),
                        Formatos.data(os.getDataPrevistaEntrega()), prazo, Formatos.dinheiro(os.calcularValorTotal()));
                soma += os.calcularValorTotal();
            }
            r.rodape = pendentes.size() + " pendente(s) — " + atrasadas + " atrasada(s) — " + proximas
                    + " próxima(s) — a receber: " + Formatos.dinheiro(soma);
            return r;
        }
    }

    // ------------------------------------------------------------------ 6

    /** Clientes cadastrados, filtrados por status. */
    static class ClientesCadastrados extends Relatorio {
        private final JComboBox<Opcao> status = Campos.comboOpcoes(new Opcao("", "Todos"),
                new Opcao("a", "Ativos"), new Opcao("i", "Inativos"));
        private final JPanel filtros = painel();

        ClientesCadastrados() {
            super("6. Clientes cadastrados");
            filtros.add(new JLabel("Status:"));
            filtros.add(status);
        }

        @Override
        public JComponent getFiltros() {
            return filtros;
        }

        @Override
        public Resultado gerar() {
            String filtro = Campos.codigo(status);
            List<Cliente> lista = new ArrayList<>();
            for (Cliente c : Cliente.listar()) {
                if (filtro.isEmpty() || c.getStatus() == filtro.charAt(0)) {
                    lista.add(c);
                }
            }
            lista.sort(Comparator.comparing(c -> c.getNome().toLowerCase(Formatos.BRASIL)));
            Resultado r = new Resultado(getNome() + " — " + status.getSelectedItem(),
                    "Código", "Nome", "CPF", "Telefone", "E-mail", "Gênero", "Cadastrado em", "Status");
            for (Cliente c : lista) {
                r.linha(c.getId(), c.getNome(), c.getCpf(), c.getTelefone(), c.getEmail(),
                        Descricoes.genero(c.getGenero()), Formatos.data(c.getDtCadastro()), c.getStatusDescricao());
            }
            r.rodape = lista.size() + " cliente(s)";
            return r;
        }
    }

    // ------------------------------------------------------------------ 7

    /** Estoque de materiais (classe Materiais do diagrama). */
    static class EstoqueMateriais extends Relatorio {
        private final JCheckBox somenteBaixo = new JCheckBox("Somente estoque abaixo de");
        private final JSpinner minimo = new JSpinner(new SpinnerNumberModel(3, 0, 9999, 1));
        private final JPanel filtros = painel();

        EstoqueMateriais() {
            super("7. Estoque de materiais");
            filtros.add(somenteBaixo);
            filtros.add(minimo);
            filtros.add(new JLabel("(materiais ativos)"));
        }

        @Override
        public JComponent getFiltros() {
            return filtros;
        }

        @Override
        public Resultado gerar() {
            int min = (Integer) minimo.getValue();
            Resultado r = new Resultado(getNome() + (somenteBaixo.isSelected() ? " — abaixo de " + min : ""),
                    "Código", "Material", "Unidade", "Estoque", "Valor unitário", "Valor em estoque", "Situação");
            double soma = 0;
            int n = 0;
            List<Material> lista = Material.listarAtivos();
            lista.sort(Comparator.comparing(Material::getQtdEstoque));
            for (Material m : lista) {
                boolean baixo = m.getQtdEstoque() < min;
                if (somenteBaixo.isSelected() && !baixo) {
                    continue;
                }
                double valor = m.getQtdEstoque() * m.getValorProduto();
                r.linha(m.getId(), m.getDescricao(), m.getUnidade(), Formatos.numero(m.getQtdEstoque()),
                        Formatos.dinheiro(m.getValorProduto()), Formatos.dinheiro(valor),
                        m.getQtdEstoque() <= 0 ? "SEM ESTOQUE" : baixo ? "Comprar" : "OK");
                soma += valor;
                n++;
            }
            r.rodape = n + " material(is) — valor total em estoque: " + Formatos.dinheiro(soma);
            return r;
        }
    }
}
