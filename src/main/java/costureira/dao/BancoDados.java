package costureira.dao;

import costureira.model.Cliente;
import costureira.model.Entidade;
import costureira.model.Material;
import costureira.model.Medida;
import costureira.model.OrdemServico;
import costureira.model.Recebimento;
import costureira.model.Servico;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "Banco de dados" do sistema: guarda todas as listas em memória e grava
 * tudo no arquivo dados/costureira.dat a cada alteração.
 *
 * O local do arquivo pode ser trocado com -Dcostureira.dados=caminho/arquivo.dat
 */
public class BancoDados implements Serializable {

    private static final long serialVersionUID = 1L;

    private static BancoDados instancia;
    private static File arquivo;

    private final List<Cliente> clientes = new ArrayList<>();
    private final List<Medida> medidas = new ArrayList<>();
    private final List<Material> materiais = new ArrayList<>();
    private final List<Servico> servicos = new ArrayList<>();
    private final List<OrdemServico> ordens = new ArrayList<>();
    private final List<Recebimento> recebimentos = new ArrayList<>();
    private final Map<String, Integer> sequencias = new HashMap<>();

    public static synchronized BancoDados get() {
        if (instancia == null) {
            instancia = carregar();
        }
        return instancia;
    }

    public static File getArquivo() {
        if (arquivo == null) {
            arquivo = new File(System.getProperty("costureira.dados", "dados" + File.separator + "costureira.dat"));
        }
        return arquivo;
    }

    private static BancoDados carregar() {
        File f = getArquivo();
        if (!f.exists()) {
            return new BancoDados();
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
            return (BancoDados) in.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            // arquivo corrompido ou de versão antiga: guarda uma cópia e começa vazio
            File copia = new File(f.getPath() + ".erro");
            f.renameTo(copia);
            System.err.println("Não foi possível ler " + f + " (" + e + "). Cópia salva em " + copia);
            return new BancoDados();
        }
    }

    /** Grava o arquivo (primeiro num temporário, para não corromper em caso de falha). */
    public synchronized void gravar() {
        File f = getArquivo();
        File pasta = f.getAbsoluteFile().getParentFile();
        if (pasta != null) {
            pasta.mkdirs();
        }
        File temp = new File(f.getPath() + ".tmp");
        try {
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(temp))) {
                out.writeObject(this);
            }
            Files.move(temp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível gravar os dados em " + f.getAbsolutePath(), e);
        }
    }

    /** Gera o próximo id (@id automático) de uma entidade. */
    public synchronized int proximoId(String entidade) {
        int proximo = sequencias.getOrDefault(entidade, 0) + 1;
        sequencias.put(entidade, proximo);
        return proximo;
    }

    public boolean isVazio() {
        return clientes.isEmpty() && servicos.isEmpty() && materiais.isEmpty() && ordens.isEmpty();
    }

    public static <T extends Entidade> T buscar(List<T> lista, int id) {
        for (T t : lista) {
            if (t.getId() == id) {
                return t;
            }
        }
        return null;
    }

    /** Troca o registro de mesmo id pelo objeto informado. */
    public static <T extends Entidade> void substituir(List<T> lista, T objeto) {
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId() == objeto.getId()) {
                lista.set(i, objeto);
                return;
            }
        }
        throw new IllegalStateException("Registro " + objeto.getId() + " não encontrado");
    }

    public List<Cliente> getClientes() {
        return clientes;
    }

    public List<Medida> getMedidas() {
        return medidas;
    }

    public List<Material> getMateriais() {
        return materiais;
    }

    public List<Servico> getServicos() {
        return servicos;
    }

    public List<OrdemServico> getOrdens() {
        return ordens;
    }

    public List<Recebimento> getRecebimentos() {
        return recebimentos;
    }
}
