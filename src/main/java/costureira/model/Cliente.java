package costureira.model;

import costureira.dao.BancoDados;
import costureira.util.Descricoes;
import costureira.util.Formatos;
import costureira.util.ValidaCpf;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Cadastro de cliente.
 * Atributos: @id, *status (a/i), *nome, *#cpf, *telefone, e-mail, gênero (f/m/n),
 * data de nascimento, endereço, @dt_cadastro.
 */
public class Cliente extends Entidade {

    private static final long serialVersionUID = 1L;

    private char status = 'a';
    private String nome;
    private LocalDate dtNasc;
    private String endereco;
    private String telefone;
    private String cpf;
    private String email;
    private char genero = 'n';
    private LocalDate dtCadastro;

    @Override
    public void salvar() {
        BancoDados bd = BancoDados.get();
        if (Formatos.vazio(nome)) {
            throw new RegraNegocioException("Informe o nome do cliente.");
        }
        if (!ValidaCpf.valido(cpf)) {
            throw new RegraNegocioException("CPF inválido. Confira os números digitados.");
        }
        cpf = ValidaCpf.formatar(cpf);
        for (Cliente c : bd.getClientes()) {
            if (c.id != id && Formatos.somenteDigitos(c.cpf).equals(Formatos.somenteDigitos(cpf))) {
                throw new RegraNegocioException("Já existe um cliente cadastrado com este CPF: " + c.nome + ".");
            }
        }
        String fone = Formatos.somenteDigitos(telefone);
        if (fone.length() < 10 || fone.length() > 11) {
            throw new RegraNegocioException("Informe o telefone com DDD (10 ou 11 dígitos).");
        }
        telefone = formatarTelefone(fone);
        if (!Formatos.vazio(email) && !email.trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new RegraNegocioException("E-mail inválido.");
        }
        if (dtNasc != null && dtNasc.isAfter(LocalDate.now())) {
            throw new RegraNegocioException("A data de nascimento não pode ser no futuro.");
        }
        if (status != 'a' && status != 'i') {
            throw new RegraNegocioException("Status inválido.");
        }
        if (genero != 'f' && genero != 'm' && genero != 'n') {
            throw new RegraNegocioException("Gênero inválido.");
        }
        nome = nome.trim();

        if (id == 0) {
            id = bd.proximoId("cliente");
            dtCadastro = LocalDate.now();
            bd.getClientes().add(this);
        } else {
            dtCadastro = buscar(id).dtCadastro;
            BancoDados.substituir(bd.getClientes(), this);
        }
        bd.gravar();
    }

    /** Regra: cliente com ordem vinculada não pode ser excluído, só inativado. */
    @Override
    public void excluir() {
        BancoDados bd = BancoDados.get();
        for (OrdemServico os : bd.getOrdens()) {
            if (os.getIdCliente() == id) {
                throw new RegraNegocioException("Este cliente possui ordens de serviço e não pode ser excluído.\n"
                        + "Edite o cadastro e altere o status para Inativo.");
            }
        }
        bd.getClientes().removeIf(c -> c.id == id);
        bd.getMedidas().removeIf(m -> m.getIdCliente() == id);
        bd.gravar();
    }

    public static List<Cliente> listar() {
        return new ArrayList<>(BancoDados.get().getClientes());
    }

    public static List<Cliente> listarAtivos() {
        List<Cliente> ativos = new ArrayList<>();
        for (Cliente c : BancoDados.get().getClientes()) {
            if (c.status == 'a') {
                ativos.add(c);
            }
        }
        return ativos;
    }

    public static Cliente buscar(int id) {
        return BancoDados.buscar(BancoDados.get().getClientes(), id);
    }

    private static String formatarTelefone(String d) {
        if (d.length() == 11) {
            return "(" + d.substring(0, 2) + ") " + d.substring(2, 7) + "-" + d.substring(7);
        }
        return "(" + d.substring(0, 2) + ") " + d.substring(2, 6) + "-" + d.substring(6);
    }

    public String getStatusDescricao() {
        return Descricoes.statusCadastro(status);
    }

    @Override
    public String toString() {
        return nome + (status == 'i' ? " (inativo)" : "");
    }

    public char getStatus() {
        return status;
    }

    public void setStatus(char status) {
        this.status = status;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public LocalDate getDtNasc() {
        return dtNasc;
    }

    public void setDtNasc(LocalDate dtNasc) {
        this.dtNasc = dtNasc;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public char getGenero() {
        return genero;
    }

    public void setGenero(char genero) {
        this.genero = genero;
    }

    public LocalDate getDtCadastro() {
        return dtCadastro;
    }
}
