package costureira.model;

import costureira.dao.BancoDados;
import costureira.util.Descricoes;
import costureira.util.Formatos;

import java.util.ArrayList;
import java.util.List;

/**
 * Cadastro de materiais (linhas, zíperes, botões, elástico...).
 * Atributos: @id, *status (a/i), *tipo, marca, cor, *unidade, *qtd_estoque, *valor_produto.
 */
public class Material extends Entidade {

    private static final long serialVersionUID = 1L;

    public static final String[] UNIDADES = {"un", "m", "cm", "par", "rolo", "pacote", "carretel"};

    private char status = 'a';
    private String tipo;
    private String marca;
    private String cor;
    private String unidade = "un";
    private double qtdEstoque;
    private double valorProduto;

    @Override
    public void salvar() {
        BancoDados bd = BancoDados.get();
        if (Formatos.vazio(tipo)) {
            throw new RegraNegocioException("Informe o tipo do material (ex.: Zíper, Linha, Botão).");
        }
        if (Formatos.vazio(unidade)) {
            throw new RegraNegocioException("Informe a unidade (ex.: un, m).");
        }
        if (qtdEstoque < 0) {
            throw new RegraNegocioException("A quantidade em estoque não pode ser negativa.");
        }
        if (valorProduto < 0) {
            throw new RegraNegocioException("O valor do produto não pode ser negativo.");
        }
        if (status != 'a' && status != 'i') {
            throw new RegraNegocioException("Status inválido.");
        }
        tipo = tipo.trim();
        if (id == 0) {
            id = bd.proximoId("material");
            bd.getMateriais().add(this);
        } else {
            BancoDados.substituir(bd.getMateriais(), this);
        }
        bd.gravar();
    }

    /** Material já usado em alguma ordem não pode ser excluído, só inativado. */
    @Override
    public void excluir() {
        BancoDados bd = BancoDados.get();
        for (OrdemServico os : bd.getOrdens()) {
            for (ItemMaterial im : os.getMateriais()) {
                if (im.getIdMaterial() == id) {
                    throw new RegraNegocioException("Este material já foi usado na OS nº " + os.getId()
                            + " e não pode ser excluído.\nEdite o cadastro e altere o status para Inativo.");
                }
            }
        }
        bd.getMateriais().removeIf(m -> m.id == id);
        bd.gravar();
    }

    public static List<Material> listar() {
        return new ArrayList<>(BancoDados.get().getMateriais());
    }

    public static List<Material> listarAtivos() {
        List<Material> ativos = new ArrayList<>();
        for (Material m : BancoDados.get().getMateriais()) {
            if (m.status == 'a') {
                ativos.add(m);
            }
        }
        return ativos;
    }

    public static Material buscar(int id) {
        return BancoDados.buscar(BancoDados.get().getMateriais(), id);
    }

    /** Ex.: "Zíper invisível Corrente - Preto". */
    public String getDescricao() {
        StringBuilder sb = new StringBuilder(tipo == null ? "" : tipo);
        if (!Formatos.vazio(marca)) {
            sb.append(' ').append(marca.trim());
        }
        if (!Formatos.vazio(cor)) {
            sb.append(" - ").append(cor.trim());
        }
        return sb.toString();
    }

    public String getStatusDescricao() {
        return Descricoes.statusCadastro(status);
    }

    @Override
    public String toString() {
        return getDescricao() + " (" + Formatos.numero(qtdEstoque) + " " + unidade + " em estoque)";
    }

    public char getStatus() {
        return status;
    }

    public void setStatus(char status) {
        this.status = status;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    public String getUnidade() {
        return unidade;
    }

    public void setUnidade(String unidade) {
        this.unidade = unidade;
    }

    public double getQtdEstoque() {
        return qtdEstoque;
    }

    public void setQtdEstoque(double qtdEstoque) {
        this.qtdEstoque = qtdEstoque;
    }

    public double getValorProduto() {
        return valorProduto;
    }

    public void setValorProduto(double valorProduto) {
        this.valorProduto = valorProduto;
    }
}
