/*
 * Sistema de Controle de Serviços - Costureira Iraci (versão web)
 * Mesmas classes e regras de negócio da versão Java (src/main/java/costureira).
 * Os dados ficam gravados no próprio aparelho (localStorage do navegador).
 */
'use strict';
(function () {

  // =====================================================================
  // Armazenamento (equivale ao BancoDados.java)
  // =====================================================================
  const CHAVE = 'costureira-iraci-v1';
  const vazio = () => ({ clientes: [], medidas: [], materiais: [], servicos: [], ordens: [], recebimentos: [], seq: {} });
  let salvoNoAparelho = true;

  function carregar() {
    try {
      const texto = localStorage.getItem(CHAVE);
      if (texto) return Object.assign(vazio(), JSON.parse(texto));
    } catch (e) {
      salvoNoAparelho = false;
    }
    return vazio();
  }

  let db = carregar();

  function gravar() {
    try {
      localStorage.setItem(CHAVE, JSON.stringify(db));
      salvoNoAparelho = true;
    } catch (e) {
      salvoNoAparelho = false;
    }
  }

  function proximoId(entidade) {
    db.seq[entidade] = (db.seq[entidade] || 0) + 1;
    return db.seq[entidade];
  }

  const bdVazio = () => !db.clientes.length && !db.servicos.length && !db.materiais.length && !db.ordens.length;

  // =====================================================================
  // Formatos (equivale ao Formatos.java)
  // =====================================================================
  class RegraErro extends Error {}
  const falha = (msg) => { throw new RegraErro(msg); };

  const moedaFmt = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
  const numFmt = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 });
  const num2Fmt = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2, useGrouping: false });
  const dinheiro = (v) => moedaFmt.format(v || 0).replace(/ /g, ' ');
  const numero = (v) => numFmt.format(v || 0);
  const valor2 = (v) => num2Fmt.format(v || 0);
  const esc = (s) => String(s == null ? '' : s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const digitos = (s) => String(s || '').replace(/\D/g, '');
  const vazioTxt = (s) => !s || !String(s).trim();

  const isoDe = (d) => d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
  const hoje = () => isoDe(new Date());
  const partes = (s) => s.split('-').map(Number);
  function somarDias(s, n) { const [a, m, d] = partes(s); return isoDe(new Date(a, m - 1, d + n)); }
  function diasEntre(de, ate) {
    const [a1, m1, d1] = partes(de);
    const [a2, m2, d2] = partes(ate);
    return Math.round((Date.UTC(a2, m2 - 1, d2) - Date.UTC(a1, m1 - 1, d1)) / 86400000);
  }
  function data(s) { if (!s) return ''; const [a, m, d] = s.split('-'); return d + '/' + m + '/' + a; }
  function dataCurta(s) { if (!s) return ''; const [, m, d] = s.split('-'); return d + '/' + m; }
  function horaAgora() { const d = new Date(); return String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0'); }

  function cap(campo) { const t = campo.replace(/^(o|a|os|as) /, ''); return t.charAt(0).toUpperCase() + t.slice(1); }

  /** Aceita "45", "45,90", "45.90", "1.234,56" e "R$ 1.234,56". */
  function lerDecimal(texto, campo) {
    let t = String(texto || '').replace(/R\$/g, '').replace(/\s/g, '');
    if (!t) falha('Informe ' + campo + '.');
    if (t.includes(',')) t = t.replace(/\./g, '').replace(',', '.');
    const n = Number(t);
    if (!isFinite(n)) falha(cap(campo) + ' deve ser um número. Ex.: 45,90');
    return n;
  }

  function lerInteiro(texto, campo) {
    const t = String(texto || '').trim();
    if (!t) falha('Informe ' + campo + '.');
    if (!/^-?\d+$/.test(t)) falha(cap(campo) + ' deve ser um número inteiro.');
    return parseInt(t, 10);
  }

  // ValidaCpf.java
  function cpfValido(cpf) {
    const d = digitos(cpf);
    if (d.length !== 11 || /^(\d)\1{10}$/.test(d)) return false;
    for (const pos of [9, 10]) {
      let soma = 0;
      for (let i = 0; i < pos; i++) soma += Number(d[i]) * (pos + 1 - i);
      let resto = (soma * 10) % 11;
      if (resto === 10) resto = 0;
      if (resto !== Number(d[pos])) return false;
    }
    return true;
  }
  const fmtCpf = (c) => { const d = digitos(c); return d.length === 11 ? `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}` : c; };
  const fmtFone = (d) => d.length === 11 ? `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}` : `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;

  // Descricoes.java — "STATUS DAS CLASSES DO SISTEMA"
  const STATUS_CAD = { a: 'Ativo', i: 'Inativo' };
  const STATUS_OS = { a: 'Agendado', e: 'Em execução', f: 'Finalizado', c: 'Cancelado' };
  const STATUS_REC = { a: 'Recebido', c: 'Cancelado' };
  const GENERO = { n: 'Não informado', f: 'Feminino', m: 'Masculino' };
  const FORMAS = { d: 'Dinheiro', p: 'PIX', db: 'Cartão de débito', cr: 'Cartão de crédito' };
  const TIPOS_MEDIDA = ['Busto', 'Cintura', 'Quadril', 'Ombro', 'Comprimento da manga', 'Cava', 'Comprimento da blusa',
    'Comprimento da calça', 'Gancho', 'Coxa', 'Barra', 'Pescoço'];
  const UNIDADES = ['un', 'm', 'cm', 'par', 'rolo', 'pacote', 'carretel'];
  const ESTOQUE_MINIMO = 3;

  // =====================================================================
  // Modelo + regras de negócio (pacote model do Java)
  // =====================================================================
  const buscar = (lista, id) => db[lista].find((x) => x.id === id) || null;
  const copia = (o) => JSON.parse(JSON.stringify(o));

  function guardar(lista, obj, novo) {
    if (novo) {
      db[lista].push(obj);
    } else {
      const i = db[lista].findIndex((x) => x.id === obj.id);
      db[lista][i] = obj;
    }
  }

  const Clientes = {
    ativos: () => db.clientes.filter((c) => c.status === 'a'),
    salvar(c) {
      if (vazioTxt(c.nome)) falha('Informe o nome do cliente.');
      if (!cpfValido(c.cpf)) falha('CPF inválido. Confira os números digitados.');
      c.cpf = fmtCpf(c.cpf);
      const outro = db.clientes.find((x) => x.id !== c.id && digitos(x.cpf) === digitos(c.cpf));
      if (outro) falha('Já existe um cliente cadastrado com este CPF: ' + outro.nome + '.');
      const fone = digitos(c.telefone);
      if (fone.length < 10 || fone.length > 11) falha('Informe o telefone com DDD (10 ou 11 dígitos).');
      c.telefone = fmtFone(fone);
      if (!vazioTxt(c.email) && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(c.email.trim())) falha('E-mail inválido.');
      if (c.dtNasc && c.dtNasc > hoje()) falha('A data de nascimento não pode ser no futuro.');
      c.nome = c.nome.trim();
      const novo = !c.id;
      if (novo) {
        c.id = proximoId('cliente');
        c.dtCadastro = hoje();
      } else {
        c.dtCadastro = buscar('clientes', c.id).dtCadastro;
      }
      guardar('clientes', c, novo);
      gravar();
    },
    /** Regra: cliente com ordem vinculada não pode ser excluído, só inativado. */
    excluir(id) {
      if (db.ordens.some((o) => o.idCliente === id)) {
        falha('Este cliente possui ordens de serviço e não pode ser excluído. Edite o cadastro e altere o status para Inativo.');
      }
      db.clientes = db.clientes.filter((c) => c.id !== id);
      db.medidas = db.medidas.filter((m) => m.idCliente !== id);
      gravar();
    },
  };

  const Medidas = {
    salvar(m) {
      if (!buscar('clientes', m.idCliente)) falha('Selecione o cliente.');
      if (vazioTxt(m.tipoMedida)) falha('Informe o tipo de medida (ex.: Cintura).');
      if (!(m.valor > 0)) falha('O valor da medida deve ser maior que zero.');
      if (!m.dataAfericao) falha('Informe a data em que a medida foi tirada.');
      if (m.dataAfericao > hoje()) falha('A data da medida não pode ser no futuro.');
      m.tipoMedida = m.tipoMedida.trim();
      const novo = !m.id;
      if (novo) m.id = proximoId('medida');
      guardar('medidas', m, novo);
      gravar();
    },
    excluir(id) {
      db.medidas = db.medidas.filter((m) => m.id !== id);
      gravar();
    },
  };

  const Materiais = {
    ativos: () => db.materiais.filter((m) => m.status === 'a'),
    descricao: (m) => [m.tipo, m.marca].filter((x) => !vazioTxt(x)).join(' ') + (vazioTxt(m.cor) ? '' : ' - ' + m.cor),
    salvar(m) {
      if (vazioTxt(m.tipo)) falha('Informe o tipo do material (ex.: Zíper, Linha, Botão).');
      if (vazioTxt(m.unidade)) falha('Informe a unidade (ex.: un, m).');
      if (m.qtdEstoque < 0) falha('A quantidade em estoque não pode ser negativa.');
      if (m.valorProduto < 0) falha('O valor do produto não pode ser negativo.');
      m.tipo = m.tipo.trim();
      const novo = !m.id;
      if (novo) m.id = proximoId('material');
      guardar('materiais', m, novo);
      gravar();
    },
    /** Material já usado em alguma ordem não pode ser excluído, só inativado. */
    excluir(id) {
      const os = db.ordens.find((o) => o.materiais.some((im) => im.idMaterial === id));
      if (os) falha('Este material já foi usado na OS nº ' + os.id + ' e não pode ser excluído. Edite o cadastro e altere o status para Inativo.');
      db.materiais = db.materiais.filter((m) => m.id !== id);
      gravar();
    },
  };

  const Servicos = {
    ativos: () => db.servicos.filter((s) => s.status === 'a'),
    salvar(s) {
      if (vazioTxt(s.nome)) falha('Informe o nome do serviço.');
      const igual = db.servicos.find((x) => x.id !== s.id && x.nome.toLowerCase() === s.nome.trim().toLowerCase());
      if (igual) falha('Já existe um serviço com o nome "' + igual.nome + '".');
      if (!(s.valorPadrao > 0)) falha('O valor padrão deve ser maior que zero.');
      if (!(s.tempoEstimado > 0)) falha('O tempo estimado (em minutos) deve ser maior que zero.');
      s.nome = s.nome.trim();
      const novo = !s.id;
      if (novo) s.id = proximoId('servico');
      guardar('servicos', s, novo);
      gravar();
    },
    /** Regra: serviço com ordem vinculada não pode ser excluído, só inativado. */
    excluir(id) {
      const os = db.ordens.find((o) => o.itens.some((i) => i.idServico === id));
      if (os) falha('Este serviço já foi usado na OS nº ' + os.id + ' e não pode ser excluído. Edite o cadastro e altere o status para Inativo.');
      db.servicos = db.servicos.filter((s) => s.id !== id);
      gravar();
    },
  };

  /**
   * Ordem de serviço / agendamento.
   * Ciclo: Agendado (a) → Em execução (e) → Finalizado (f, no recebimento). Cancelado (c) enquanto não finalizada.
   */
  const Ordens = {
    totalServicos: (o) => o.itens.reduce((s, i) => s + i.quantidade * i.valorUnitario, 0),
    totalMateriais: (o) => o.materiais.reduce((s, m) => s + m.quantidade * m.valorUnitario, 0),
    total: (o) => Ordens.totalServicos(o) + Ordens.totalMateriais(o),
    pendente: (o) => o.status === 'a' || o.status === 'e',
    atrasada: (o) => Ordens.pendente(o) && !!o.dataPrevista && o.dataPrevista < hoje(),
    nomeCliente: (o) => { const c = buscar('clientes', o.idCliente); return c ? c.nome : '(cliente removido)'; },
    recebimento: (o) => db.recebimentos.find((r) => r.idOrdem === o.id && r.status === 'a') || null,

    salvar(o) {
      const antiga = o.id ? buscar('ordens', o.id) : null;
      if (antiga && (antiga.status === 'f' || antiga.status === 'c')) falha('Ordem finalizada ou cancelada não pode mais ser editada.');
      const cliente = buscar('clientes', o.idCliente);
      if (!cliente) falha('Selecione o cliente.');
      if ((!antiga || antiga.idCliente !== o.idCliente) && cliente.status !== 'a') {
        falha('O cliente ' + cliente.nome + ' está inativo. Só é possível registrar ordem para cliente ativo.');
      }
      if (!o.dataAgendamento) falha('Informe a data do agendamento.');
      if (!o.hora) falha('Informe a hora do agendamento.');
      if (!o.dataPrevista) falha('Informe a data prevista de entrega.');
      if (o.dataPrevista < o.dataAgendamento) falha('A data prevista de entrega não pode ser anterior à data de agendamento.');
      if (!o.itens.length) falha('A ordem deve ter pelo menos um item (peça e serviço).');
      for (const item of o.itens) {
        const s = buscar('servicos', item.idServico);
        if (!s) falha('Serviço do item "' + item.descricaoPeca + '" não encontrado.');
        if (!item.id && s.status !== 'a') falha('O serviço ' + s.nome + ' está inativo e não pode ser incluído.');
        if (vazioTxt(item.descricaoPeca)) falha('Informe a descrição da peça de todos os itens.');
        if (!(item.quantidade > 0)) falha('A quantidade de cada item deve ser maior que zero.');
      }
      for (const im of o.materiais) {
        const m = buscar('materiais', im.idMaterial);
        if (!m) falha('Material "' + im.descricaoMaterial + '" não encontrado.');
        if (!im.id && m.status !== 'a') falha('O material ' + Materiais.descricao(m) + ' está inativo e não pode ser incluído.');
        if (!(im.quantidade > 0)) falha('A quantidade de cada material deve ser maior que zero.');
      }

      // estoque: compara o que a ordem usava antes com o que usa agora
      const diferenca = {};
      for (const im of o.materiais) diferenca[im.idMaterial] = (diferenca[im.idMaterial] || 0) + im.quantidade;
      if (antiga) for (const im of antiga.materiais) diferenca[im.idMaterial] = (diferenca[im.idMaterial] || 0) - im.quantidade;
      for (const id of Object.keys(diferenca)) {
        const m = buscar('materiais', Number(id));
        const precisa = diferenca[id];
        if (precisa > 1e-9 && m.qtdEstoque + 1e-9 < precisa) {
          falha('Estoque insuficiente de ' + Materiais.descricao(m) + '. Disponível: ' + numero(m.qtdEstoque) + ' ' + m.unidade
            + ' | Necessário: ' + numero(precisa) + ' ' + m.unidade);
        }
      }

      // tudo validado: grava
      for (const id of Object.keys(diferenca)) {
        const m = buscar('materiais', Number(id));
        m.qtdEstoque = Math.round((m.qtdEstoque - diferenca[id]) * 1000) / 1000;
      }
      const novo = !antiga;
      if (novo) {
        o.id = proximoId('ordem');
        o.status = 'a';
        o.dataEntrega = null;
        o.retiradaPor = null;
      } else {
        o.status = antiga.status;
        o.dataEntrega = antiga.dataEntrega;
        o.retiradaPor = antiga.retiradaPor;
      }
      for (const item of o.itens) { if (!item.id) item.id = proximoId('itemOrdem'); item.idOrdem = o.id; }
      for (const im of o.materiais) { if (!im.id) im.id = proximoId('itemMaterial'); im.idOrdem = o.id; }
      o.valorTotal = Ordens.total(o);
      guardar('ordens', o, novo);
      gravar();
    },

    iniciar(id) {
      const o = buscar('ordens', id);
      if (o.status !== 'a') falha('Só é possível iniciar a execução de uma ordem agendada.');
      o.status = 'e';
      gravar();
    },

    devolverMateriais(o) {
      for (const im of o.materiais) {
        const m = buscar('materiais', im.idMaterial);
        if (m) m.qtdEstoque = Math.round((m.qtdEstoque + im.quantidade) * 1000) / 1000;
      }
    },

    /** Cancela a ordem e devolve os materiais ao estoque. */
    cancelar(id) {
      const o = buscar('ordens', id);
      if (o.status === 'f') falha('Ordem finalizada não pode ser cancelada.');
      if (o.status === 'c') falha('Esta ordem já está cancelada.');
      Ordens.devolverMateriais(o);
      o.status = 'c';
      gravar();
    },

    /** Só ordens agendadas ou canceladas podem ser excluídas. */
    excluir(id) {
      const o = buscar('ordens', id);
      if (o.status === 'e' || o.status === 'f') {
        falha('Só é possível excluir ordens agendadas ou canceladas. Ordem em execução pode ser cancelada; ordem finalizada faz parte do histórico.');
      }
      if (o.status === 'a') Ordens.devolverMateriais(o);
      db.ordens = db.ordens.filter((x) => x.id !== id);
      db.recebimentos = db.recebimentos.filter((r) => r.idOrdem !== id);
      gravar();
    },
  };

  /** Recebimento: sempre à vista, na entrega. Registrar finaliza a ordem. */
  const Recebimentos = {
    salvar(r) {
      const antigo = r.id ? buscar('recebimentos', r.id) : null;
      if (antigo && antigo.status === 'c') falha('Recebimento cancelado não pode ser editado.');
      if (antigo) r.idOrdem = antigo.idOrdem; // a ordem do recebimento não muda
      const os = buscar('ordens', r.idOrdem);
      if (!os) falha('Selecione a ordem de serviço.');
      if (!antigo) {
        if (os.status === 'c') falha('Não é possível registrar recebimento de uma ordem cancelada.');
        if (os.status === 'f' || Ordens.recebimento(os)) falha('Esta ordem já foi entregue e possui recebimento.');
        if (os.status === 'a') falha('A ordem ainda está agendada. Inicie a execução antes de registrar a entrega.');
      }
      if (!r.data) falha('Informe a data do recebimento.');
      if (r.data > hoje()) falha('A data do recebimento não pode ser no futuro.');
      if (r.data < os.dataAgendamento) falha('A data do recebimento não pode ser anterior ao agendamento (' + data(os.dataAgendamento) + ').');
      if (!FORMAS[r.forma]) falha('Selecione a forma de pagamento.');
      const total = Ordens.total(os);
      if (Math.abs(r.valor - total) > 0.005) {
        falha('O valor recebido deve ser igual ao valor total da ordem (' + dinheiro(total) + '). O pagamento é sempre à vista, na entrega: sem parcelamento nem adiantamento.');
      }
      r.retiradaPor = vazioTxt(r.retiradaPor) ? Ordens.nomeCliente(os) : r.retiradaPor.trim();
      r.status = 'a';
      const novo = !antigo;
      if (novo) r.id = proximoId('recebimento');
      guardar('recebimentos', r, novo);
      // entrega: a ordem é finalizada
      os.status = 'f';
      os.dataEntrega = r.data;
      os.retiradaPor = r.retiradaPor;
      gravar();
    },

    /** Estorno: cancela o recebimento e a ordem volta para "em execução". */
    cancelar(id) {
      const r = buscar('recebimentos', id);
      if (r.status === 'c') falha('Este recebimento já está cancelado.');
      r.status = 'c';
      const os = buscar('ordens', r.idOrdem);
      if (os) {
        os.status = 'e';
        os.dataEntrega = null;
        os.retiradaPor = null;
      }
      gravar();
    },
  };

  // =====================================================================
  // Dados de exemplo (DadosExemplo.java)
  // =====================================================================
  function carregarExemplo() {
    const h = hoje();
    const d = (n) => somarDias(h, n);
    const cli = (nome, cpf, telefone, email, genero) => {
      const c = { id: 0, status: 'a', nome, cpf, telefone, email, genero, dtNasc: null, endereco: 'Rua das Flores, ' + (100 + nome.length) };
      Clientes.salvar(c);
      return c;
    };
    const med = (c, tipoMedida, valor, dataAfericao) => Medidas.salvar({ id: 0, status: 'a', idCliente: c.id, tipoMedida, valor, dataAfericao });
    const serv = (nome, descricao, valorPadrao, tempoEstimado) => { const s = { id: 0, status: 'a', nome, descricao, valorPadrao, tempoEstimado }; Servicos.salvar(s); return s; };
    const mat = (tipo, marca, cor, unidade, qtdEstoque, valorProduto) => { const m = { id: 0, status: 'a', tipo, marca, cor, unidade, qtdEstoque, valorProduto }; Materiais.salvar(m); return m; };
    const item = (s, descricaoPeca, quantidade) => ({ id: 0, idServico: s.id, nomeServico: s.nome, descricaoPeca, quantidade, valorUnitario: s.valorPadrao });
    const usa = (m, quantidade) => ({ id: 0, idMaterial: m.id, descricaoMaterial: Materiais.descricao(m), unidade: m.unidade, quantidade, valorUnitario: m.valorProduto });
    const ordem = (c, dataAgendamento, dataPrevista, obs, itens, materiais) => {
      const o = { id: 0, idCliente: c.id, dataAgendamento, hora: String(9 + c.id).padStart(2, '0') + ':30', dataPrevista, obs: obs || '', itens, materiais: materiais || [] };
      Ordens.salvar(o);
      return o;
    };
    const receber = (o, dataRec, forma) => Recebimentos.salvar({ id: 0, idOrdem: o.id, data: dataRec, forma, valor: Ordens.total(o), retiradaPor: '', obs: '' });

    const maria = cli('Maria Aparecida Souza', '123.456.789-09', '(16) 99812-3456', 'maria.souza@email.com', 'f');
    const joao = cli('João Pedro Lima', '987.654.321-00', '(16) 3322-1144', '', 'm');
    const ana = cli('Ana Carolina Ribeiro', '456.123.789-55', '(16) 99701-2233', 'ana.ribeiro@email.com', 'f');
    const carlos = cli('Carlos Eduardo Martins', '321.654.987-91', '(16) 98123-7788', '', 'n');

    med(maria, 'Cintura', 74, d(-20));
    med(maria, 'Quadril', 102, d(-20));
    med(maria, 'Comprimento da calça', 98, d(-20));
    med(ana, 'Busto', 88, d(-5));

    const barra = serv('Barra de calça', 'Barra simples ou original', 25, 30);
    const ziper = serv('Troca de zíper', 'Calça, saia ou jaqueta', 35, 40);
    const ajuste = serv('Ajuste de cintura', 'Apertar ou soltar', 40, 60);
    const manga = serv('Encurtar manga', 'Camisa ou blazer', 30, 45);
    const vestido = serv('Ajuste de vestido', 'Laterais e alças', 80, 120);
    serv('Pregar botão', 'Por botão', 5, 5);

    const zip = mat('Zíper', 'YKK', 'Preto', 'un', 10, 6.5);
    const zipInv = mat('Zíper invisível', 'Corrente', 'Azul marinho', 'un', 2, 7.9);
    const linha = mat('Linha de costura', 'Coats', 'Branca', 'carretel', 15, 4.5);
    mat('Elástico', 'São José', 'Branco', 'm', 12, 2.2);
    mat('Botão', 'Genérico', 'Preto', 'un', 40, 0.5);

    const os1 = ordem(maria, d(-12), d(-9), 'Cliente pediu urgência', [item(barra, 'Calça jeans azul', 2), item(ziper, 'Calça social preta', 1)], [usa(zip, 1)]);
    Ordens.iniciar(os1.id);
    receber(os1, d(-9), 'p');
    const os2 = ordem(joao, d(-8), d(-6), '', [item(manga, 'Camisa social branca', 3)]);
    Ordens.iniciar(os2.id);
    receber(os2, d(-5), 'd');
    const os3 = ordem(ana, d(-6), d(-1), 'Prova marcada', [item(vestido, 'Vestido de festa verde', 1)], [usa(zipInv, 1), usa(linha, 1)]);
    Ordens.iniciar(os3.id);
    const os4 = ordem(maria, d(-2), d(1), '', [item(ajuste, 'Saia lápis cinza', 1), item(barra, 'Calça de linho bege', 1)]);
    Ordens.iniciar(os4.id);
    ordem(carlos, h, d(4), 'Vai trazer as peças à tarde', [item(barra, 'Calça de sarja', 2)]);
    const os6 = ordem(joao, d(-3), d(2), 'Cliente desistiu', [item(ziper, 'Jaqueta de couro', 1)]);
    Ordens.cancelar(os6.id);
  }

  // =====================================================================
  // Interface
  // =====================================================================
  const ui = {
    tela: 'inicio',
    cadastro: 'clientes',
    filtroOrdens: 'abertas',
    medidasCliente: 0,
    relatorio: 0,
    filtrosRel: {},
    confirmarApagar: false,
  };
  let folha = null;       // { tipo, id, modo: 'ver' | 'editar' | 'novo' }
  let rascunho = null;    // itens e materiais da ordem em edição
  let pendente = null;    // ação aguardando confirmação
  const podeImprimir = (() => { try { return window.self === window.top; } catch (e) { return false; } })();

  const $ = (id) => document.getElementById(id);
  const val = (nome) => { const el = $('f_' + nome); return el ? el.value : ''; };
  const opcoes = (obj) => Object.entries(obj);
  const chip = (texto, tipo) => `<span class="chip chip-${tipo}">${esc(texto)}</span>`;

  function chipOrdem(o) {
    if (Ordens.atrasada(o)) return chip('Atrasada', 'alerta');
    const tipos = { a: 'agendado', e: 'execucao', f: 'ok', c: 'neutro' };
    return chip(STATUS_OS[o.status], tipos[o.status]);
  }

  function toast(texto) {
    const t = $('toast');
    t.textContent = texto;
    t.classList.add('visivel');
    clearTimeout(toast.timer);
    toast.timer = setTimeout(() => t.classList.remove('visivel'), 2600);
  }

  // ---------- campos de formulário ----------
  function campoHTML(c, valor) {
    const id = 'f_' + c.nome;
    let entrada;
    if (c.tipo === 'select') {
      entrada = `<select id="${id}">${c.opcoes.map(([v, t]) => `<option value="${esc(v)}"${String(valor) === String(v) ? ' selected' : ''}>${esc(t)}</option>`).join('')}</select>`;
    } else if (c.tipo === 'textarea') {
      entrada = `<textarea id="${id}" rows="3">${esc(valor)}</textarea>`;
    } else {
      const lista = c.lista ? ` list="dl_${c.nome}"` : '';
      entrada = `<input id="${id}" type="${c.html || 'text'}"${c.modo ? ` inputmode="${c.modo}"` : ''}${lista}`
        + `${c.ph ? ` placeholder="${esc(c.ph)}"` : ''}${c.passo ? ` step="${c.passo}"` : ''} value="${esc(valor)}" autocomplete="off">`;
      if (c.lista) entrada += `<datalist id="dl_${c.nome}">${c.lista.map((o) => `<option value="${esc(o)}">`).join('')}</datalist>`;
    }
    const sufixo = c.sufixo ? `<span class="sufixo">${esc(c.sufixo)}</span>` : '';
    return `<label class="campo${c.largo ? ' largo' : ''}" for="${id}"><span class="rotulo">${esc(c.rotulo)}${c.obrig ? ' <b aria-hidden="true">*</b>' : ''}</span>`
      + `<span class="entrada">${entrada}${sufixo}</span></label>`;
  }

  function valorEdicao(c, obj) {
    const v = obj[c.nome];
    if (v == null) return '';
    if (c.formato === 'dinheiro') return valor2(v);
    if (c.formato === 'decimal') return numero(v).replace(/\./g, '');
    return v;
  }

  function valorLeitura(c, obj) {
    const v = obj[c.nome];
    if (v == null || v === '') return '—';
    if (c.tipo === 'select') { const o = c.opcoes.find(([k]) => String(k) === String(v)); return o ? o[1] : v; }
    if (c.html === 'date') return data(v);
    if (c.formato === 'dinheiro') return dinheiro(v);
    if (c.formato === 'decimal') return numero(v) + (c.sufixo ? ' ' + c.sufixo : '');
    return String(v) + (c.sufixo ? ' ' + c.sufixo : '');
  }

  const formGrade = (campos, obj) => `<div class="grade">${campos.map((c) => campoHTML(c, valorEdicao(c, obj))).join('')}</div>`;
  const leitura = (pares) => `<dl class="leitura">${pares.map(([k, v]) => `<div><dt>${esc(k)}</dt><dd>${v}</dd></div>`).join('')}</dl>`;

  // ---------- resumo da ordem ("Consultar serviço") ----------
  function resumoOrdem(o) {
    const itens = o.itens.map((i) => `<tr><td>${i.quantidade}×</td><td>${esc(i.descricaoPeca)}<small>${esc(i.nomeServico)} · ${dinheiro(i.valorUnitario)} cada</small></td><td class="num">${dinheiro(i.quantidade * i.valorUnitario)}</td></tr>`).join('');
    const mats = o.materiais.map((m) => `<tr><td>${numero(m.quantidade)} ${esc(m.unidade)}</td><td>${esc(m.descricaoMaterial)}</td><td class="num">${dinheiro(m.quantidade * m.valorUnitario)}</td></tr>`).join('');
    const c = buscar('clientes', o.idCliente);
    const entrega = o.dataEntrega ? `${data(o.dataEntrega)}${o.retiradaPor ? ' · retirada por ' + esc(o.retiradaPor) : ''}` : '—';
    return `<div class="resumo-os">
      <div class="resumo-topo"><span class="etiqueta">OS ${o.id}</span>${chipOrdem(o)}</div>
      ${leitura([
        ['Cliente', esc(Ordens.nomeCliente(o)) + (c ? `<small>${esc(c.telefone)}</small>` : '')],
        ['Agendamento', data(o.dataAgendamento) + ' às ' + esc(o.hora)],
        ['Entrega prevista', data(o.dataPrevista)],
        ['Entregue em', entrega],
      ])}
      <h4>Peças e serviços</h4>
      <table class="itens"><tbody>${itens}</tbody></table>
      ${mats ? `<h4>Materiais</h4><table class="itens"><tbody>${mats}</tbody></table>` : ''}
      ${vazioTxt(o.obs) ? '' : `<p class="obs">${esc(o.obs)}</p>`}
      <div class="total-linha"><span>Valor total</span><strong>${dinheiro(Ordens.total(o))}</strong></div>
    </div>`;
  }

  // =====================================================================
  // Cadastros e movimentações (uma configuração por classe do diagrama)
  // =====================================================================
  const CAD = {
    clientes: {
      lista: 'clientes', titulo: 'Cliente', plural: 'Clientes',
      novo: () => ({ id: 0, status: 'a', nome: '', cpf: '', telefone: '', email: '', dtNasc: '', endereco: '', genero: 'n' }),
      campos: () => [
        { nome: 'nome', rotulo: 'Nome', obrig: true, largo: true },
        { nome: 'cpf', rotulo: 'CPF', obrig: true, modo: 'numeric', ph: '000.000.000-00' },
        { nome: 'telefone', rotulo: 'Telefone', obrig: true, html: 'tel', ph: '(16) 99999-9999' },
        { nome: 'email', rotulo: 'E-mail', html: 'email' },
        { nome: 'dtNasc', rotulo: 'Data de nascimento', html: 'date' },
        { nome: 'endereco', rotulo: 'Endereço', largo: true },
        { nome: 'genero', rotulo: 'Gênero', tipo: 'select', opcoes: opcoes(GENERO) },
        { nome: 'status', rotulo: 'Status', obrig: true, tipo: 'select', opcoes: opcoes(STATUS_CAD) },
      ],
      ler: () => ({
        nome: val('nome'), cpf: val('cpf'), telefone: val('telefone'), email: val('email').trim(), dtNasc: val('dtNasc') || null,
        endereco: val('endereco').trim(), genero: val('genero'), status: val('status'),
      }),
      extraLeitura: (c) => [['Cadastrado em', data(c.dtCadastro)], ['Medidas', db.medidas.filter((m) => m.idCliente === c.id).length + ' registrada(s)']],
      salvar: Clientes.salvar, excluir: Clientes.excluir,
      extras: () => [{ rotulo: 'Ver medidas', acao: 'medidas-cliente' }],
      registros: () => [...db.clientes].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR')),
      cartao: (c) => ({ titulo: c.nome, sub: [c.cpf, c.telefone].join(' · '), lado: c.status === 'i' ? chip('Inativo', 'neutro') : '' }),
      busca: (c) => [c.nome, c.cpf, c.telefone, c.email].join(' '),
    },

    medidas: {
      lista: 'medidas', titulo: 'Medida', plural: 'Medidas',
      novo: () => ({ id: 0, status: 'a', idCliente: ui.medidasCliente || 0, tipoMedida: '', valor: null, dataAfericao: hoje() }),
      campos: () => [
        { nome: 'idCliente', rotulo: 'Cliente', obrig: true, largo: true, tipo: 'select', opcoes: [['0', 'Selecione...']].concat([...db.clientes].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR')).map((c) => [c.id, c.nome])) },
        { nome: 'tipoMedida', rotulo: 'Tipo de medida', obrig: true, lista: TIPOS_MEDIDA, ph: 'Ex.: Cintura' },
        { nome: 'valor', rotulo: 'Valor', obrig: true, modo: 'decimal', formato: 'decimal', sufixo: 'cm' },
        { nome: 'dataAfericao', rotulo: 'Data da medição', obrig: true, html: 'date' },
        { nome: 'status', rotulo: 'Status', obrig: true, tipo: 'select', opcoes: opcoes(STATUS_CAD) },
      ],
      ler: () => ({
        idCliente: Number(val('idCliente')), tipoMedida: val('tipoMedida'), valor: lerDecimal(val('valor'), 'o valor da medida'),
        dataAfericao: val('dataAfericao'), status: val('status'),
      }),
      salvar: Medidas.salvar, excluir: Medidas.excluir,
      registros: () => db.medidas.filter((m) => !ui.medidasCliente || m.idCliente === ui.medidasCliente),
      cartao: (m) => {
        const c = buscar('clientes', m.idCliente);
        return { titulo: m.tipoMedida, sub: (c ? c.nome : '') + ' · ' + data(m.dataAfericao), lado: `<span class="valor">${numero(m.valor)} cm</span>` };
      },
      busca: (m) => { const c = buscar('clientes', m.idCliente); return m.tipoMedida + ' ' + (c ? c.nome : ''); },
    },

    servicos: {
      lista: 'servicos', titulo: 'Serviço', plural: 'Serviços',
      novo: () => ({ id: 0, status: 'a', nome: '', descricao: '', valorPadrao: null, tempoEstimado: '' }),
      campos: () => [
        { nome: 'nome', rotulo: 'Nome', obrig: true, largo: true, ph: 'Ex.: Barra de calça' },
        { nome: 'descricao', rotulo: 'Descrição', tipo: 'textarea', largo: true },
        { nome: 'valorPadrao', rotulo: 'Valor padrão (R$)', obrig: true, modo: 'decimal', formato: 'dinheiro' },
        { nome: 'tempoEstimado', rotulo: 'Tempo estimado', obrig: true, modo: 'numeric', sufixo: 'min' },
        { nome: 'status', rotulo: 'Status', obrig: true, tipo: 'select', opcoes: opcoes(STATUS_CAD) },
      ],
      ler: () => ({
        nome: val('nome'), descricao: val('descricao').trim(), valorPadrao: lerDecimal(val('valorPadrao'), 'o valor padrão'),
        tempoEstimado: lerInteiro(val('tempoEstimado'), 'o tempo estimado'), status: val('status'),
      }),
      salvar: Servicos.salvar, excluir: Servicos.excluir,
      registros: () => [...db.servicos].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR')),
      cartao: (s) => ({ titulo: s.nome, sub: s.tempoEstimado + ' min' + (s.descricao ? ' · ' + s.descricao : ''), lado: (s.status === 'i' ? chip('Inativo', 'neutro') : '') + `<span class="valor">${dinheiro(s.valorPadrao)}</span>` }),
      busca: (s) => s.nome + ' ' + (s.descricao || ''),
    },

    materiais: {
      lista: 'materiais', titulo: 'Material', plural: 'Materiais',
      novo: () => ({ id: 0, status: 'a', tipo: '', marca: '', cor: '', unidade: 'un', qtdEstoque: null, valorProduto: null }),
      campos: () => [
        { nome: 'tipo', rotulo: 'Tipo', obrig: true, ph: 'Ex.: Zíper' },
        { nome: 'marca', rotulo: 'Marca' },
        { nome: 'cor', rotulo: 'Cor' },
        { nome: 'unidade', rotulo: 'Unidade', obrig: true, lista: UNIDADES },
        { nome: 'qtdEstoque', rotulo: 'Qtd. em estoque', obrig: true, modo: 'decimal', formato: 'decimal' },
        { nome: 'valorProduto', rotulo: 'Valor do produto (R$)', obrig: true, modo: 'decimal', formato: 'dinheiro' },
        { nome: 'status', rotulo: 'Status', obrig: true, tipo: 'select', opcoes: opcoes(STATUS_CAD) },
      ],
      ler: () => ({
        tipo: val('tipo'), marca: val('marca').trim(), cor: val('cor').trim(), unidade: val('unidade').trim(),
        qtdEstoque: lerDecimal(val('qtdEstoque'), 'a quantidade em estoque'), valorProduto: lerDecimal(val('valorProduto'), 'o valor do produto'), status: val('status'),
      }),
      salvar: Materiais.salvar, excluir: Materiais.excluir,
      registros: () => [...db.materiais].sort((a, b) => a.tipo.localeCompare(b.tipo, 'pt-BR')),
      cartao: (m) => ({
        titulo: Materiais.descricao(m),
        sub: dinheiro(m.valorProduto) + ' / ' + m.unidade,
        lado: (m.status === 'i' ? chip('Inativo', 'neutro') : m.qtdEstoque < ESTOQUE_MINIMO ? chip('Estoque baixo', 'execucao') : '') + `<span class="valor">${numero(m.qtdEstoque)} ${esc(m.unidade)}</span>`,
      }),
      busca: (m) => Materiais.descricao(m),
    },

    // ---------- movimentação: ordem de serviço ----------
    ordens: {
      lista: 'ordens', titulo: 'Ordem de serviço', plural: 'Ordens de serviço',
      novo: () => ({ id: 0, status: 'a', idCliente: 0, dataAgendamento: hoje(), hora: horaAgora(), dataPrevista: somarDias(hoje(), 3), obs: '', itens: [], materiais: [] }),
      tituloFolha: (o, modo) => modo === 'novo' ? 'Nova ordem de serviço' : 'Ordem de serviço nº ' + o.id,
      corpo(o, modo) {
        if (modo === 'ver') return resumoOrdem(o);
        rascunho = rascunho || { itens: copia(o.itens), materiais: copia(o.materiais) };
        const clientes = Clientes.ativos();
        const atual = buscar('clientes', o.idCliente);
        if (atual && !clientes.includes(atual)) clientes.push(atual);
        clientes.sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'));
        const servicos = Servicos.ativos();
        const materiais = Materiais.ativos();
        return `${formGrade([
          { nome: 'idCliente', rotulo: 'Cliente', obrig: true, largo: true, tipo: 'select', opcoes: [['0', 'Selecione o cliente...']].concat(clientes.map((c) => [c.id, c.nome + (c.status === 'i' ? ' (inativo)' : '')])) },
          { nome: 'dataAgendamento', rotulo: 'Data do agendamento', obrig: true, html: 'date' },
          { nome: 'hora', rotulo: 'Hora', obrig: true, html: 'time' },
          { nome: 'dataPrevista', rotulo: 'Entrega prevista', obrig: true, html: 'date' },
          { nome: 'obs', rotulo: 'Observações', tipo: 'textarea', largo: true },
        ], o)}
        <section class="bloco">
          <h3>Itens do serviço <b aria-hidden="true">*</b></h3>
          <div id="lista-itens"></div>
          <div class="adicionar">
            <label class="campo largo" for="n_servico"><span class="rotulo">Serviço</span><span class="entrada"><select id="n_servico">${servicos.length ? servicos.map((s) => `<option value="${s.id}">${esc(s.nome)} — ${dinheiro(s.valorPadrao)}</option>`).join('') : '<option value="">Cadastre os serviços primeiro</option>'}</select></span></label>
            <label class="campo" for="n_peca"><span class="rotulo">Peça</span><span class="entrada"><input id="n_peca" placeholder="Ex.: Calça jeans azul" autocomplete="off"></span></label>
            <label class="campo curto" for="n_qtd"><span class="rotulo">Qtd.</span><span class="entrada"><input id="n_qtd" type="number" min="1" step="1" value="1" inputmode="numeric"></span></label>
            <button type="button" class="botao secundario" data-acao="add-item">Adicionar item</button>
          </div>
        </section>
        <section class="bloco">
          <h3>Materiais utilizados</h3>
          <div id="lista-materiais"></div>
          <div class="adicionar">
            <label class="campo largo" for="n_material"><span class="rotulo">Material</span><span class="entrada"><select id="n_material">${materiais.length ? materiais.map((m) => `<option value="${m.id}">${esc(Materiais.descricao(m))} (${numero(m.qtdEstoque)} ${esc(m.unidade)})</option>`).join('') : '<option value="">Nenhum material cadastrado</option>'}</select></span></label>
            <label class="campo curto" for="n_qtdmat"><span class="rotulo">Qtd.</span><span class="entrada"><input id="n_qtdmat" inputmode="decimal" placeholder="1" autocomplete="off"></span></label>
            <button type="button" class="botao secundario" data-acao="add-mat">Adicionar material</button>
          </div>
          <details class="novo-material">
            <summary>Cadastrar material novo</summary>
            <div class="grade">
              ${campoHTML({ nome: 'nm_tipo', rotulo: 'Tipo', obrig: true, ph: 'Ex.: Botão' }, '')}
              ${campoHTML({ nome: 'nm_cor', rotulo: 'Cor' }, '')}
              ${campoHTML({ nome: 'nm_unidade', rotulo: 'Unidade', obrig: true, lista: UNIDADES }, 'un')}
              ${campoHTML({ nome: 'nm_qtd', rotulo: 'Qtd. em estoque', obrig: true, modo: 'decimal' }, '')}
              ${campoHTML({ nome: 'nm_valor', rotulo: 'Valor (R$)', obrig: true, modo: 'decimal' }, '')}
            </div>
            <button type="button" class="botao secundario" data-acao="novo-material">Salvar material</button>
          </details>
        </section>
        <div class="total-linha" id="total-ordem"></div>`;
      },
      depois(o, modo) { if (modo !== 'ver') desenharItens(); },
      ler: () => ({
        idCliente: Number(val('idCliente')), dataAgendamento: val('dataAgendamento'), hora: val('hora'), dataPrevista: val('dataPrevista'),
        obs: val('obs').trim(), itens: copia(rascunho.itens), materiais: copia(rascunho.materiais),
      }),
      salvar: Ordens.salvar, excluir: Ordens.excluir,
      podeEditar: (o) => o.status === 'a' || o.status === 'e',
      extras: (o) => {
        const b = [];
        if (o.status === 'a') b.push({ rotulo: 'Iniciar execução', acao: 'iniciar', tipo: 'principal' });
        if (o.status === 'e') b.push({ rotulo: 'Registrar recebimento', acao: 'receber-os', tipo: 'principal' });
        if (Ordens.pendente(o)) b.push({ rotulo: 'Cancelar OS', acao: 'cancelar-os', tipo: 'perigo' });
        if (podeImprimir) b.push({ rotulo: 'Imprimir', acao: 'imprimir' });
        return b;
      },
      podeExcluir: (o) => o.status === 'a' || o.status === 'c',
      registros: () => {
        const peso = (o) => Ordens.atrasada(o) ? 0 : o.status === 'e' ? 1 : o.status === 'a' ? 2 : 3;
        return db.ordens.filter((o) => {
          switch (ui.filtroOrdens) {
            case 'abertas': return Ordens.pendente(o);
            case 'atrasadas': return Ordens.atrasada(o);
            case 'finalizadas': return o.status === 'f';
            case 'canceladas': return o.status === 'c';
            default: return true;
          }
        }).sort((a, b) => peso(a) - peso(b) || (peso(a) < 3 ? a.dataPrevista.localeCompare(b.dataPrevista) : b.id - a.id));
      },
      cartao: (o) => ({
        etiqueta: 'OS ' + o.id,
        titulo: Ordens.nomeCliente(o),
        sub: (o.status === 'f' ? 'Entregue ' + dataCurta(o.dataEntrega) : 'Entrega ' + dataCurta(o.dataPrevista)) + ' · ' + o.itens.reduce((s, i) => s + i.quantidade, 0) + ' peça(s)',
        lado: chipOrdem(o) + `<span class="valor">${dinheiro(Ordens.total(o))}</span>`,
        alerta: Ordens.atrasada(o),
      }),
      busca: (o) => ['OS ' + o.id, Ordens.nomeCliente(o), STATUS_OS[o.status]].concat(o.itens.map((i) => i.descricaoPeca + ' ' + i.nomeServico)).join(' '),
    },

    // ---------- movimentação: recebimento ----------
    recebimentos: {
      lista: 'recebimentos', titulo: 'Recebimento', plural: 'Recebimentos',
      novo: () => {
        const prontas = db.ordens.filter((o) => o.status === 'e');
        const preferida = ui.ordemParaReceber && prontas.find((o) => o.id === ui.ordemParaReceber);
        const os = preferida || prontas[0];
        return { id: 0, status: 'a', idOrdem: os ? os.id : 0, data: hoje(), forma: 'p', valor: os ? Ordens.total(os) : null, retiradaPor: os ? Ordens.nomeCliente(os) : '', obs: '' };
      },
      tituloFolha: (r, modo) => modo === 'novo' ? 'Registrar recebimento' : 'Recebimento nº ' + r.id,
      corpo(r, modo) {
        const os = buscar('ordens', r.idOrdem);
        if (modo === 'ver') {
          return leitura([
            ['Situação', r.status === 'a' ? chip('Recebido', 'ok') : chip('Cancelado', 'neutro')],
            ['Data', data(r.data)],
            ['Forma de pagamento', esc(FORMAS[r.forma])],
            ['Valor recebido', `<strong>${dinheiro(r.valor)}</strong>`],
            ['Retirada por', esc(r.retiradaPor)],
            ['Observações', esc(r.obs) || '—'],
          ]) + (os ? `<h3 class="sub">Consultar serviço</h3>${resumoOrdem(os)}` : '');
        }
        let escolha;
        if (modo === 'novo') {
          const prontas = db.ordens.filter((o) => o.status === 'e');
          if (!prontas.length) {
            return `<div class="vazio"><p><strong>Não há ordens em execução para entregar.</strong></p><p>Na ordem de serviço, toque em <em>Iniciar execução</em> antes de registrar a entrega e o recebimento.</p></div>`;
          }
          escolha = campoHTML({ nome: 'idOrdem', rotulo: 'Ordem de serviço', obrig: true, largo: true, tipo: 'select', opcoes: prontas.map((o) => [o.id, `OS ${o.id} — ${Ordens.nomeCliente(o)} — ${dinheiro(Ordens.total(o))}`]) }, r.idOrdem);
        } else {
          escolha = `<input type="hidden" id="f_idOrdem" value="${r.idOrdem}">`;
        }
        return `<div class="grade">${escolha}</div>
          <h3 class="sub">Consultar serviço</h3>
          <div id="consulta-os">${os ? resumoOrdem(os) : ''}</div>
          ${formGrade([
            { nome: 'data', rotulo: 'Data do recebimento', obrig: true, html: 'date' },
            { nome: 'forma', rotulo: 'Forma de pagamento', obrig: true, tipo: 'select', opcoes: opcoes(FORMAS) },
            { nome: 'valor', rotulo: 'Valor recebido (R$)', obrig: true, modo: 'decimal', formato: 'dinheiro' },
            { nome: 'retiradaPor', rotulo: 'Retirada por' },
            { nome: 'obs', rotulo: 'Observações', largo: true },
          ], r)}
          <p class="dica">O pagamento é sempre à vista, na entrega: o valor tem que ser igual ao total da ordem.</p>`;
      },
      depois(r, modo) {
        const sel = $('f_idOrdem');
        if (modo === 'novo' && sel && sel.tagName === 'SELECT') {
          sel.addEventListener('change', () => {
            const os = buscar('ordens', Number(sel.value));
            $('consulta-os').innerHTML = os ? resumoOrdem(os) : '';
            if (os) {
              $('f_valor').value = valor2(Ordens.total(os));
              $('f_retiradaPor').value = Ordens.nomeCliente(os);
            }
          });
        }
      },
      semFormulario: (r, modo) => modo === 'novo' && !db.ordens.some((o) => o.status === 'e'),
      ler: () => ({
        idOrdem: Number(val('idOrdem')), data: val('data'), forma: val('forma'), valor: lerDecimal(val('valor'), 'o valor recebido'),
        retiradaPor: val('retiradaPor'), obs: val('obs').trim(),
      }),
      salvar: Recebimentos.salvar,
      podeEditar: (r) => r.status === 'a',
      podeExcluir: () => false,
      extras: (r) => r.status === 'a' ? [{ rotulo: 'Recibo', acao: 'recibo' }, { rotulo: 'Cancelar recebimento', acao: 'estornar', tipo: 'perigo' }] : [],
      registros: () => [...db.recebimentos].sort((a, b) => b.data.localeCompare(a.data) || b.id - a.id),
      cartao: (r) => {
        const os = buscar('ordens', r.idOrdem);
        return {
          etiqueta: 'OS ' + r.idOrdem,
          titulo: os ? Ordens.nomeCliente(os) : '',
          sub: data(r.data) + ' · ' + FORMAS[r.forma],
          lado: (r.status === 'c' ? chip('Cancelado', 'neutro') : '') + `<span class="valor">${dinheiro(r.valor)}</span>`,
        };
      },
      busca: (r) => { const os = buscar('ordens', r.idOrdem); return 'OS ' + r.idOrdem + ' ' + (os ? Ordens.nomeCliente(os) : '') + ' ' + FORMAS[r.forma]; },
    },
  };

  // itens e materiais da ordem em edição
  function desenharItens() {
    if (!rascunho || !$('lista-itens')) return;
    $('lista-itens').innerHTML = rascunho.itens.length
      ? `<ul class="linhas">${rascunho.itens.map((i, n) => `<li><span><strong>${i.quantidade}× ${esc(i.descricaoPeca)}</strong><small>${esc(i.nomeServico)} · ${dinheiro(i.valorUnitario)} cada</small></span><span class="valor">${dinheiro(i.quantidade * i.valorUnitario)}</span><button type="button" class="remover" data-acao="rem-item" data-n="${n}" aria-label="Remover ${esc(i.descricaoPeca)}">×</button></li>`).join('')}</ul>`
      : '<p class="dica">Nenhuma peça ainda. Escolha o serviço, descreva a peça e toque em Adicionar item.</p>';
    $('lista-materiais').innerHTML = rascunho.materiais.length
      ? `<ul class="linhas">${rascunho.materiais.map((m, n) => `<li><span><strong>${numero(m.quantidade)} ${esc(m.unidade)} · ${esc(m.descricaoMaterial)}</strong><small>${dinheiro(m.valorUnitario)} / ${esc(m.unidade)}</small></span><span class="valor">${dinheiro(m.quantidade * m.valorUnitario)}</span><button type="button" class="remover" data-acao="rem-mat" data-n="${n}" aria-label="Remover ${esc(m.descricaoMaterial)}">×</button></li>`).join('')}</ul>`
      : '';
    const o = { itens: rascunho.itens, materiais: rascunho.materiais };
    $('total-ordem').innerHTML = `<span>Serviços ${dinheiro(Ordens.totalServicos(o))} · Materiais ${dinheiro(Ordens.totalMateriais(o))}</span><strong>${dinheiro(Ordens.total(o))}</strong>`;
  }

  // =====================================================================
  // Folha (painel de cadastro) — botões Cadastrar, Editar, Salvar, Cancelar, Excluir
  // =====================================================================
  function abrir(tipo, id, modo) {
    folha = { tipo, id: id || 0, modo };
    rascunho = null;
    pendente = null;
    desenharFolha();
    $('folha').hidden = false;
    document.body.classList.add('travado');
    const primeiro = $('folha-corpo').querySelector('input:not([type=hidden]), select, textarea');
    if (modo !== 'ver' && primeiro && window.matchMedia('(min-width: 720px)').matches) primeiro.focus();
  }

  function fecharFolha() {
    folha = null;
    rascunho = null;
    pendente = null;
    $('folha').hidden = true;
    document.body.classList.remove('travado');
  }

  function registroAtual() {
    const cfg = CAD[folha.tipo];
    if (folha.modo === 'novo') return cfg.novo();
    const obj = buscar(cfg.lista, folha.id);
    return obj ? copia(obj) : null;
  }

  function desenharFolha() {
    const cfg = CAD[folha.tipo];
    const obj = registroAtual();
    if (!obj) { fecharFolha(); return; }
    const modo = folha.modo;
    $('folha-titulo').textContent = cfg.tituloFolha ? cfg.tituloFolha(obj, modo) : (modo === 'novo' ? 'Cadastrar ' + cfg.titulo.toLowerCase() : cfg.titulo);
    let corpo;
    if (cfg.corpo) {
      corpo = cfg.corpo(obj, modo);
    } else if (modo === 'ver') {
      const campos = cfg.campos();
      corpo = leitura(campos.map((c) => [c.rotulo, esc(valorLeitura(c, obj))]).concat(cfg.extraLeitura ? cfg.extraLeitura(obj) : []));
    } else {
      corpo = formGrade(cfg.campos(), obj);
    }
    $('folha-corpo').innerHTML = corpo;
    $('folha-corpo').scrollTop = 0;
    esconderErro();
    desenharBotoes(cfg, obj);
    if (cfg.depois) cfg.depois(obj, modo);
  }

  function botaoHTML(b) {
    return `<button type="button" class="botao ${b.tipo || 'secundario'}" data-acao="${b.acao}">${esc(b.rotulo)}</button>`;
  }

  function desenharBotoes(cfg, obj) {
    const area = $('folha-botoes');
    if (pendente) {
      area.innerHTML = `<p class="pergunta">${esc(pendente.texto)}</p>` + botaoHTML({ rotulo: pendente.sim, acao: 'sim', tipo: 'perigo' }) + botaoHTML({ rotulo: 'Voltar', acao: 'nao' });
      return;
    }
    let botoes = [];
    if (folha.modo === 'ver') {
      botoes = botoes.concat(cfg.extras ? cfg.extras(obj) : []);
      if (!cfg.podeEditar || cfg.podeEditar(obj)) botoes.push({ rotulo: 'Editar', acao: 'editar' });
      if (cfg.excluir && (!cfg.podeExcluir || cfg.podeExcluir(obj))) botoes.push({ rotulo: 'Excluir', acao: 'excluir', tipo: 'perigo-leve' });
    } else if (cfg.semFormulario && cfg.semFormulario(obj, folha.modo)) {
      botoes.push({ rotulo: 'Fechar', acao: 'fechar-folha' });
    } else {
      botoes.push({ rotulo: 'Salvar', acao: 'salvar', tipo: 'principal' }, { rotulo: 'Cancelar', acao: 'cancelar-edicao' });
    }
    area.innerHTML = botoes.map(botaoHTML).join('');
  }

  function mostrarErro(texto) {
    const e = $('folha-erro');
    e.textContent = texto;
    e.hidden = false;
    e.scrollIntoView({ block: 'nearest' });
  }
  function esconderErro() { $('folha-erro').hidden = true; }

  function confirmar(texto, sim, acao) {
    pendente = { texto, sim, acao };
    desenharBotoes(CAD[folha.tipo], registroAtual());
  }

  /** Executa uma regra de negócio; se ela recusar, mostra a mensagem na folha. */
  function tentar(fn) {
    try {
      fn();
      return true;
    } catch (e) {
      if (!(e instanceof RegraErro)) throw e;
      pendente = null;
      if (folha) {
        desenharBotoes(CAD[folha.tipo], registroAtual());
        mostrarErro(e.message);
      } else {
        toast(e.message);
      }
      return false;
    }
  }

  // =====================================================================
  // Telas
  // =====================================================================
  const ICONES = {
    inicio: '<path d="M4 11 12 4l8 7v9h-5v-6H9v6H4z"/>',
    ordens: '<circle cx="6.5" cy="6.5" r="2.5"/><circle cx="6.5" cy="17.5" r="2.5"/><path d="M8.6 8 20 18M8.6 16 20 6"/>',
    recebimentos: '<rect x="3" y="6" width="18" height="12" rx="2"/><circle cx="12" cy="12" r="2.6"/>',
    cadastros: '<path d="M4 5h16v4H4zM4 11h16v4H4zM4 17h10v3H4z"/>',
    relatorios: '<path d="M5 20V10M12 20V4M19 20v-7"/>',
  };

  function listaHTML(tipo) {
    const cfg = CAD[tipo];
    const regs = cfg.registros();
    const id = 'busca_' + tipo;
    const itens = regs.map((r) => {
      const c = cfg.cartao(r);
      return `<li><button type="button" class="cartao${c.alerta ? ' cartao-alerta' : ''}" data-acao="abrir" data-tipo="${tipo}" data-id="${r.id}" data-busca="${esc(cfg.busca(r).toLowerCase())}">
        ${c.etiqueta ? `<span class="etiqueta">${esc(c.etiqueta)}</span>` : ''}
        <span class="cartao-txt"><strong>${esc(c.titulo)}</strong><small>${esc(c.sub)}</small></span>
        <span class="cartao-lado">${c.lado || ''}</span></button></li>`;
    }).join('');
    return `<div class="barra-lista">
        <input type="search" id="${id}" class="busca" placeholder="Pesquisar ${esc(cfg.plural.toLowerCase())}" data-busca-lista="${tipo}" autocomplete="off" aria-label="Pesquisar">
        <button type="button" class="botao principal" data-acao="novo" data-tipo="${tipo}">${tipo === 'recebimentos' ? 'Registrar' : 'Cadastrar'}</button>
      </div>
      ${regs.length ? `<ul class="lista" id="lista_${tipo}">${itens}</ul><p class="contagem">${regs.length} registro(s)</p>`
        : `<div class="vazio"><p>Nenhum registro aqui ainda.</p><p>Toque em <strong>${tipo === 'recebimentos' ? 'Registrar' : 'Cadastrar'}</strong> para incluir o primeiro.</p></div>`}`;
  }

  function telaInicio() {
    const h = hoje();
    const mes = h.slice(0, 7);
    const ags = db.ordens.filter((o) => o.status === 'a').length;
    const exec = db.ordens.filter((o) => o.status === 'e').length;
    const atr = db.ordens.filter(Ordens.atrasada).length;
    const recebido = db.recebimentos.filter((r) => r.status === 'a' && r.data.slice(0, 7) === mes).reduce((s, r) => s + r.valor, 0);
    const proximas = db.ordens.filter(Ordens.pendente).sort((a, b) => a.dataPrevista.localeCompare(b.dataPrevista)).slice(0, 6);

    const prazo = (o) => {
      const n = diasEntre(h, o.dataPrevista);
      if (n < 0) return chip('Atrasada ' + (-n) + 'd', 'alerta');
      if (n === 0) return chip('Hoje', 'execucao');
      if (n === 1) return chip('Amanhã', 'agendado');
      return chip('Em ' + n + ' dias', 'neutro');
    };

    const boasVindas = bdVazio() ? `<section class="painel boas-vindas">
        <h2>Comece por aqui</h2>
        <p>O sistema está vazio. Você pode cadastrar os serviços e clientes da Iraci, ou carregar dados de exemplo para conhecer as telas.</p>
        <div class="acoes"><button type="button" class="botao principal" data-acao="exemplo">Carregar dados de exemplo</button>
        <button type="button" class="botao secundario" data-acao="ir-cadastro" data-cad="servicos">Cadastrar serviços</button></div>
      </section>` : '';

    return `${boasVindas}
      <section class="indicadores" aria-label="Resumo">
        <button type="button" class="indicador" data-acao="ir-ordens" data-filtro="abertas"><span>Agendadas</span><strong>${ags}</strong></button>
        <button type="button" class="indicador" data-acao="ir-ordens" data-filtro="abertas"><span>Em execução</span><strong>${exec}</strong></button>
        <button type="button" class="indicador${atr ? ' indicador-alerta' : ''}" data-acao="ir-ordens" data-filtro="atrasadas"><span>Atrasadas</span><strong>${atr}</strong></button>
        <button type="button" class="indicador" data-acao="ir-relatorio" data-rel="2"><span>Recebido no mês</span><strong class="dinheiro">${dinheiro(recebido)}</strong></button>
      </section>

      <section class="atalhos">
        <button type="button" class="botao principal grande" data-acao="novo" data-tipo="ordens">Nova ordem de serviço</button>
        <button type="button" class="botao secundario grande" data-acao="novo" data-tipo="recebimentos">Registrar recebimento</button>
        <button type="button" class="botao secundario grande" data-acao="novo" data-tipo="clientes">Cadastrar cliente</button>
      </section>

      <section class="painel">
        <div class="painel-topo"><h2>Próximas entregas</h2><button type="button" class="link" data-acao="ir-ordens" data-filtro="abertas">Ver todas</button></div>
        ${proximas.length ? `<ul class="lista">${proximas.map((o) => `<li><button type="button" class="cartao${Ordens.atrasada(o) ? ' cartao-alerta' : ''}" data-acao="abrir" data-tipo="ordens" data-id="${o.id}">
            <span class="etiqueta">OS ${o.id}</span>
            <span class="cartao-txt"><strong>${esc(Ordens.nomeCliente(o))}</strong><small>${data(o.dataPrevista)} · ${esc(STATUS_OS[o.status])}</small></span>
            <span class="cartao-lado">${prazo(o)}</span></button></li>`).join('')}</ul>`
          : '<p class="dica">Nenhuma entrega pendente.</p>'}
      </section>

      <section class="painel dados">
        <h2>Seus dados</h2>
        <p>${salvoNoAparelho ? 'Tudo o que você cadastra fica salvo neste aparelho, neste navegador.' : 'Este navegador não está deixando salvar os dados: eles serão perdidos ao fechar a página.'}</p>
        ${bdVazio() ? '' : ui.confirmarApagar
          ? `<p class="pergunta">Apagar todos os clientes, serviços, materiais, ordens e recebimentos deste aparelho?</p>
             <div class="acoes"><button type="button" class="botao perigo" data-acao="apagar-sim">Sim, apagar tudo</button><button type="button" class="botao secundario" data-acao="apagar-nao">Voltar</button></div>`
          : '<button type="button" class="link perigo-txt" data-acao="apagar">Apagar todos os dados</button>'}
      </section>`;
  }

  function telaOrdens() {
    const filtros = [['abertas', 'Em aberto'], ['atrasadas', 'Atrasadas'], ['finalizadas', 'Finalizadas'], ['canceladas', 'Canceladas'], ['todas', 'Todas']];
    return `<h1 class="titulo-tela">Ordens de serviço</h1>
      <div class="filtros" role="group" aria-label="Filtrar ordens">${filtros.map(([k, t]) => `<button type="button" class="filtro${ui.filtroOrdens === k ? ' ativo' : ''}" data-acao="filtro-ordens" data-filtro="${k}" aria-pressed="${ui.filtroOrdens === k}">${t}</button>`).join('')}</div>
      ${listaHTML('ordens')}`;
  }

  function telaRecebimentos() {
    const total = db.recebimentos.filter((r) => r.status === 'a').reduce((s, r) => s + r.valor, 0);
    return `<h1 class="titulo-tela">Recebimentos</h1>
      <p class="dica">Sempre à vista, na entrega das roupas. Total recebido: <strong>${dinheiro(total)}</strong></p>
      ${listaHTML('recebimentos')}`;
  }

  function telaCadastros() {
    const abas = [['clientes', 'Clientes'], ['medidas', 'Medidas'], ['servicos', 'Serviços'], ['materiais', 'Materiais']];
    let extra = '';
    if (ui.cadastro === 'medidas') {
      const clientes = [...db.clientes].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'));
      extra = `<label class="campo largo filtro-cliente" for="filtro_medidas"><span class="rotulo">Cliente</span><span class="entrada"><select id="filtro_medidas">
        <option value="0">Todos os clientes</option>${clientes.map((c) => `<option value="${c.id}"${ui.medidasCliente === c.id ? ' selected' : ''}>${esc(c.nome)}</option>`).join('')}</select></span></label>`;
    }
    return `<h1 class="titulo-tela">Cadastros</h1>
      <div class="segmentos" role="tablist">${abas.map(([k, t]) => `<button type="button" role="tab" class="segmento${ui.cadastro === k ? ' ativo' : ''}" aria-selected="${ui.cadastro === k}" data-acao="ir-cadastro" data-cad="${k}">${t}</button>`).join('')}</div>
      ${extra}${listaHTML(ui.cadastro)}`;
  }

  // =====================================================================
  // Relatórios (pacote relatorio do Java)
  // =====================================================================
  const inicioPadrao = () => { const [a, m] = partes(hoje()); return isoDe(new Date(a, m - 2, 1)); };
  const fimPadrao = () => { const [a, m] = partes(hoje()); return isoDe(new Date(a, m, 0)); };
  const filtroPeriodo = (rotulo) => [
    { nome: 'ini', rotulo: rotulo + ' de', html: 'date', padrao: inicioPadrao },
    { nome: 'fim', rotulo: 'até', html: 'date', padrao: fimPadrao },
  ];
  const noPeriodo = (d, f) => {
    if (!f.ini || !f.fim) falha('Informe as datas do período.');
    if (f.fim < f.ini) falha('A data final não pode ser anterior à data inicial.');
    return !!d && d >= f.ini && d <= f.fim;
  };
  const periodoTxt = (f) => data(f.ini) + ' a ' + data(f.fim);
  const ranking = (a, b) => b[1] - a[1] || b[2] - a[2];

  const RELATORIOS = [
    {
      nome: 'Ordens de serviço por período e situação',
      filtros: filtroPeriodo('Agendamento').concat([
        { nome: 'status', rotulo: 'Situação', tipo: 'select', opcoes: [['', 'Todas']].concat(opcoes(STATUS_OS)), padrao: () => '' },
        { nome: 'agrupar', rotulo: 'Agrupar', tipo: 'select', opcoes: [['nao', 'Detalhado'], ['dia', 'Por dia'], ['semana', 'Por semana'], ['mes', 'Por mês']], padrao: () => 'nao' },
      ]),
      gerar(f) {
        const ordens = db.ordens.filter((o) => noPeriodo(o.dataAgendamento, f) && (!f.status || o.status === f.status))
          .sort((a, b) => a.dataAgendamento.localeCompare(b.dataAgendamento) || a.hora.localeCompare(b.hora));
        const soma = ordens.reduce((s, o) => s + Ordens.total(o), 0);
        const rodape = ordens.length + ' ordem(ns) · valor total ' + dinheiro(soma);
        if (f.agrupar === 'nao') {
          return {
            titulo: periodoTxt(f), rodape,
            colunas: ['Nº', 'Data', 'Hora', 'Cliente', 'Situação', 'Entrega prevista', 'Valor total'], num: [6],
            linhas: ordens.map((o) => [o.id, data(o.dataAgendamento), o.hora, Ordens.nomeCliente(o), STATUS_OS[o.status], data(o.dataPrevista), dinheiro(Ordens.total(o))]),
          };
        }
        const grupos = new Map();
        for (const o of ordens) {
          let chave = o.dataAgendamento;
          if (f.agrupar === 'semana') { const [a, m, d] = partes(chave); const dt = new Date(a, m - 1, d); dt.setDate(dt.getDate() - ((dt.getDay() + 6) % 7)); chave = isoDe(dt); }
          if (f.agrupar === 'mes') chave = chave.slice(0, 7) + '-01';
          const g = grupos.get(chave) || { a: 0, e: 0, f: 0, c: 0, valor: 0 };
          g[o.status]++;
          g.valor += Ordens.total(o);
          grupos.set(chave, g);
        }
        const rotulo = (k) => f.agrupar === 'semana' ? 'Semana ' + dataCurta(k) + ' a ' + dataCurta(somarDias(k, 6)) : f.agrupar === 'mes' ? k.slice(5, 7) + '/' + k.slice(0, 4) : data(k);
        return {
          titulo: periodoTxt(f), rodape,
          colunas: ['Período', 'Agendadas', 'Em execução', 'Finalizadas', 'Canceladas', 'Total', 'Valor total'], num: [1, 2, 3, 4, 5, 6],
          linhas: [...grupos.keys()].sort().map((k) => { const g = grupos.get(k); return [rotulo(k), g.a, g.e, g.f, g.c, g.a + g.e + g.f + g.c, dinheiro(g.valor)]; }),
        };
      },
    },
    {
      nome: 'Serviços mais solicitados',
      filtros: filtroPeriodo('Agendamento'),
      gerar(f) {
        const mapa = new Map();
        for (const o of db.ordens) {
          if (o.status === 'c' || !noPeriodo(o.dataAgendamento, f)) continue;
          const vistos = new Set();
          for (const i of o.itens) {
            const g = mapa.get(i.nomeServico) || [i.nomeServico, 0, 0, 0];
            g[1] += i.quantidade;
            g[2] += i.quantidade * i.valorUnitario;
            if (!vistos.has(i.nomeServico)) { g[3]++; vistos.add(i.nomeServico); }
            mapa.set(i.nomeServico, g);
          }
        }
        const lista = [...mapa.values()].sort(ranking);
        return {
          titulo: periodoTxt(f) + ' · ordens canceladas não entram',
          colunas: ['Posição', 'Serviço', 'Peças', 'Nº de ordens', 'Valor total'], num: [2, 3, 4],
          linhas: lista.map((g, n) => [(n + 1) + 'º', g[0], g[1], g[3], dinheiro(g[2])]),
          rodape: lista.length + ' serviço(s) · ' + lista.reduce((s, g) => s + g[1], 0) + ' peça(s) · ' + dinheiro(lista.reduce((s, g) => s + g[2], 0)),
        };
      },
    },
    {
      nome: 'Recebimentos por período e forma de pagamento',
      filtros: filtroPeriodo('Recebimento').concat([{ nome: 'forma', rotulo: 'Forma de pagamento', tipo: 'select', opcoes: [['', 'Todas']].concat(opcoes(FORMAS)), padrao: () => '' }]),
      gerar(f) {
        const lista = db.recebimentos.filter((r) => r.status === 'a' && noPeriodo(r.data, f) && (!f.forma || r.forma === f.forma))
          .sort((a, b) => a.data.localeCompare(b.data) || a.id - b.id);
        const total = lista.reduce((s, r) => s + r.valor, 0);
        const linhas = lista.map((r) => { const os = buscar('ordens', r.idOrdem); return [data(r.data), r.id, r.idOrdem, os ? Ordens.nomeCliente(os) : '', FORMAS[r.forma], dinheiro(r.valor)]; });
        const subtotais = lista.length && !f.forma
          ? Object.keys(FORMAS).map((k) => ['', '', '', 'Subtotal', FORMAS[k], dinheiro(lista.filter((r) => r.forma === k).reduce((s, r) => s + r.valor, 0))])
          : [];
        return {
          titulo: periodoTxt(f), colunas: ['Data', 'Receb. nº', 'OS nº', 'Cliente', 'Forma', 'Valor'], num: [5],
          linhas, subtotais, rodape: lista.length + ' recebimento(s) · total do período ' + dinheiro(total),
        };
      },
    },
    {
      nome: 'Clientes que mais utilizaram os serviços',
      filtros: filtroPeriodo('Agendamento'),
      gerar(f) {
        const mapa = new Map();
        for (const o of db.ordens) {
          if (o.status === 'c' || !noPeriodo(o.dataAgendamento, f)) continue;
          const g = mapa.get(o.idCliente) || [o.idCliente, 0, 0];
          g[1]++;
          g[2] += Ordens.total(o);
          mapa.set(o.idCliente, g);
        }
        const lista = [...mapa.values()].sort(ranking);
        return {
          titulo: periodoTxt(f) + ' · ordens canceladas não entram',
          colunas: ['Posição', 'Cliente', 'Telefone', 'Ordens', 'Valor total', 'Ticket médio'], num: [3, 4, 5],
          linhas: lista.map((g, n) => { const c = buscar('clientes', g[0]); return [(n + 1) + 'º', c ? c.nome : '(removido)', c ? c.telefone : '', g[1], dinheiro(g[2]), dinheiro(g[2] / g[1])]; }),
          rodape: lista.length + ' cliente(s) · ' + dinheiro(lista.reduce((s, g) => s + g[2], 0)),
        };
      },
    },
    {
      nome: 'Entregas pendentes e atrasadas',
      filtros: [
        { nome: 'dias', rotulo: '"Próxima" = vence em até (dias)', html: 'number', padrao: () => '2' },
        { nome: 'somente', rotulo: 'Mostrar', tipo: 'select', opcoes: [['todas', 'Todas as pendentes'], ['proximas', 'Só atrasadas e próximas']], padrao: () => 'todas' },
      ],
      gerar(f) {
        const limite = Math.max(0, parseInt(f.dias, 10) || 0);
        const h = hoje();
        const lista = db.ordens.filter((o) => Ordens.pendente(o) && (f.somente === 'todas' || diasEntre(h, o.dataPrevista) <= limite))
          .sort((a, b) => a.dataPrevista.localeCompare(b.dataPrevista));
        let atrasadas = 0;
        let proximas = 0;
        const linhas = lista.map((o) => {
          const n = diasEntre(h, o.dataPrevista);
          let prazo;
          if (n < 0) { prazo = 'ATRASADA há ' + (-n) + ' dia(s)'; atrasadas++; }
          else if (n === 0) { prazo = 'Vence HOJE'; proximas++; }
          else if (n <= limite) { prazo = 'Vence em ' + n + ' dia(s)'; proximas++; }
          else prazo = 'No prazo (' + n + ' dias)';
          const c = buscar('clientes', o.idCliente);
          return [o.id, Ordens.nomeCliente(o), c ? c.telefone : '', STATUS_OS[o.status], data(o.dataPrevista), prazo, dinheiro(Ordens.total(o))];
        });
        return {
          titulo: 'Posição em ' + data(h), colunas: ['OS', 'Cliente', 'Telefone', 'Situação', 'Entrega prevista', 'Prazo', 'Valor'], num: [6], destaque: 5,
          linhas, rodape: lista.length + ' pendente(s) · ' + atrasadas + ' atrasada(s) · ' + proximas + ' próxima(s) · a receber ' + dinheiro(lista.reduce((s, o) => s + Ordens.total(o), 0)),
        };
      },
    },
    {
      nome: 'Clientes cadastrados',
      filtros: [{ nome: 'status', rotulo: 'Status', tipo: 'select', opcoes: [['', 'Todos'], ['a', 'Ativos'], ['i', 'Inativos']], padrao: () => '' }],
      gerar(f) {
        const lista = db.clientes.filter((c) => !f.status || c.status === f.status).sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'));
        return {
          titulo: f.status ? (f.status === 'a' ? 'Ativos' : 'Inativos') : 'Todos',
          colunas: ['Código', 'Nome', 'CPF', 'Telefone', 'E-mail', 'Gênero', 'Cadastro', 'Status'],
          linhas: lista.map((c) => [c.id, c.nome, c.cpf, c.telefone, c.email, GENERO[c.genero], data(c.dtCadastro), STATUS_CAD[c.status]]),
          rodape: lista.length + ' cliente(s)',
        };
      },
    },
    {
      nome: 'Estoque de materiais',
      filtros: [
        { nome: 'minimo', rotulo: 'Estoque mínimo', html: 'number', padrao: () => String(ESTOQUE_MINIMO) },
        { nome: 'somente', rotulo: 'Mostrar', tipo: 'select', opcoes: [['todos', 'Todos os ativos'], ['baixo', 'Só abaixo do mínimo']], padrao: () => 'todos' },
      ],
      gerar(f) {
        const min = Number(f.minimo) || 0;
        const lista = Materiais.ativos().filter((m) => f.somente === 'todos' || m.qtdEstoque < min).sort((a, b) => a.qtdEstoque - b.qtdEstoque);
        return {
          titulo: 'Materiais ativos', colunas: ['Código', 'Material', 'Unidade', 'Estoque', 'Valor unit.', 'Valor em estoque', 'Situação'], num: [3, 4, 5], destaque: 6,
          linhas: lista.map((m) => [m.id, Materiais.descricao(m), m.unidade, numero(m.qtdEstoque), dinheiro(m.valorProduto), dinheiro(m.qtdEstoque * m.valorProduto),
            m.qtdEstoque <= 0 ? 'SEM ESTOQUE' : m.qtdEstoque < min ? 'Comprar' : 'OK']),
          rodape: lista.length + ' material(is) · valor em estoque ' + dinheiro(lista.reduce((s, m) => s + m.qtdEstoque * m.valorProduto, 0)),
        };
      },
    },
  ];

  function filtrosDoRelatorio(n) {
    const rel = RELATORIOS[n];
    if (!ui.filtrosRel[n]) {
      ui.filtrosRel[n] = {};
      for (const f of rel.filtros) ui.filtrosRel[n][f.nome] = f.padrao();
    }
    return ui.filtrosRel[n];
  }

  function telaRelatorios() {
    const n = ui.relatorio;
    const rel = RELATORIOS[n];
    const f = filtrosDoRelatorio(n);
    let resultado;
    let erro = '';
    try {
      resultado = rel.gerar(f);
    } catch (e) {
      if (!(e instanceof RegraErro)) throw e;
      erro = e.message;
    }
    const filtros = rel.filtros.map((c) => {
      const id = 'rf_' + c.nome;
      const entrada = c.tipo === 'select'
        ? `<select id="${id}" data-filtro-rel="${c.nome}">${c.opcoes.map(([v, t]) => `<option value="${esc(v)}"${f[c.nome] === v ? ' selected' : ''}>${esc(t)}</option>`).join('')}</select>`
        : `<input id="${id}" type="${c.html}" data-filtro-rel="${c.nome}" value="${esc(f[c.nome])}"${c.html === 'number' ? ' min="0" inputmode="numeric"' : ''}>`;
      return `<label class="campo" for="${id}"><span class="rotulo">${esc(c.rotulo)}</span><span class="entrada">${entrada}</span></label>`;
    }).join('');

    let tabela = '';
    if (resultado) {
      const num = new Set(resultado.num || []);
      const celula = (v, i) => `<td class="${num.has(i) ? 'num' : ''}${i === resultado.destaque && /ATRASADA|SEM ESTOQUE|Comprar/.test(v) ? ' destaque' : ''}">${esc(v)}</td>`;
      tabela = resultado.linhas.length
        ? `<div class="tabela-rolagem"><table class="relatorio">
            <thead><tr>${resultado.colunas.map((c, i) => `<th class="${num.has(i) ? 'num' : ''}">${esc(c)}</th>`).join('')}</tr></thead>
            <tbody>${resultado.linhas.map((l) => `<tr>${l.map(celula).join('')}</tr>`).join('')}</tbody>
            ${resultado.subtotais && resultado.subtotais.length ? `<tbody class="subtotais">${resultado.subtotais.map((l) => `<tr>${l.map(celula).join('')}</tr>`).join('')}</tbody>` : ''}
          </table></div><p class="rodape-rel">${esc(resultado.rodape)}</p>`
        : '<div class="vazio"><p>Nenhum registro encontrado com esses filtros.</p></div>';
    }

    return `<h1 class="titulo-tela">Relatórios</h1>
      <label class="campo largo escolha-rel" for="rel_escolha"><span class="rotulo">Relatório</span><span class="entrada"><select id="rel_escolha">
        ${RELATORIOS.map((r, i) => `<option value="${i}"${i === n ? ' selected' : ''}>${i + 1}. ${esc(r.nome)}</option>`).join('')}</select></span></label>
      <div class="painel">
        <div class="grade filtros-rel">${filtros}</div>
      </div>
      <div class="cabecalho-rel"><h2>${n + 1}. ${esc(rel.nome)}</h2>${resultado ? `<p>${esc(resultado.titulo)}</p>` : ''}
        ${podeImprimir && resultado && resultado.linhas.length ? '<button type="button" class="botao secundario" data-acao="imprimir">Imprimir</button>' : ''}</div>
      ${erro ? `<p class="erro-inline">${esc(erro)}</p>` : tabela}`;
  }

  const TELAS = { inicio: telaInicio, ordens: telaOrdens, recebimentos: telaRecebimentos, cadastros: telaCadastros, relatorios: telaRelatorios };

  function render() {
    for (const b of document.querySelectorAll('[data-tela]')) {
      const ativo = b.dataset.tela === ui.tela;
      b.classList.toggle('ativo', ativo);
      if (ativo) b.setAttribute('aria-current', 'page'); else b.removeAttribute('aria-current');
    }
    $('tela').innerHTML = (TELAS[ui.tela] || telaInicio)();
  }

  function irPara(tela) {
    ui.tela = tela;
    ui.confirmarApagar = false;
    render();
    window.scrollTo(0, 0);
    try { history.replaceState(null, '', '#' + tela); } catch (e) { /* sem histórico */ }
  }

  // =====================================================================
  // Ações (cliques)
  // =====================================================================
  const ACOES = {
    abrir: (el) => abrir(el.dataset.tipo, Number(el.dataset.id), 'ver'),
    novo: (el) => { ui.ordemParaReceber = 0; abrir(el.dataset.tipo, 0, 'novo'); },
    'fechar-folha': () => {
      if (folha && folha.modo !== 'ver' && !(CAD[folha.tipo].semFormulario && CAD[folha.tipo].semFormulario(registroAtual(), folha.modo))) {
        confirmar('Sair sem salvar as alterações?', 'Sair sem salvar', fecharFolha);
        return;
      }
      fecharFolha();
    },
    editar: () => {
      const cfg = CAD[folha.tipo];
      const obj = registroAtual();
      if (cfg.podeEditar && !cfg.podeEditar(obj)) { mostrarErro('Ordem finalizada ou cancelada não pode mais ser editada.'); return; }
      folha.modo = 'editar';
      rascunho = null;
      desenharFolha();
    },
    'cancelar-edicao': () => {
      if (folha.modo === 'novo') { fecharFolha(); return; }
      folha.modo = 'ver';
      rascunho = null;
      desenharFolha();
    },
    salvar: () => {
      const cfg = CAD[folha.tipo];
      const eraNovo = folha.modo === 'novo';
      tentar(() => {
        const obj = cfg.ler();
        obj.id = eraNovo ? 0 : folha.id;
        cfg.salvar(obj);
        folha.id = obj.id;
        folha.modo = 'ver';
        rascunho = null;
        desenharFolha();
        render();
        toast(folha.tipo === 'recebimentos' && eraNovo ? 'Recebimento registrado. OS ' + obj.idOrdem + ' finalizada.' : 'Registro salvo.');
      });
    },
    excluir: () => confirmar('Excluir este registro?', 'Sim, excluir', () => {
      const cfg = CAD[folha.tipo];
      if (tentar(() => cfg.excluir(folha.id))) { fecharFolha(); render(); toast('Registro excluído.'); }
    }),
    sim: () => { const p = pendente; pendente = null; p.acao(); },
    nao: () => { pendente = null; desenharBotoes(CAD[folha.tipo], registroAtual()); },

    // ordem de serviço
    iniciar: () => { if (tentar(() => Ordens.iniciar(folha.id))) { desenharFolha(); render(); toast('Ordem em execução.'); } },
    'cancelar-os': () => confirmar('Cancelar esta ordem? Os materiais voltam para o estoque e ela não poderá mais ser editada.', 'Sim, cancelar OS', () => {
      if (tentar(() => Ordens.cancelar(folha.id))) { desenharFolha(); render(); toast('Ordem cancelada.'); }
    }),
    'receber-os': () => { ui.ordemParaReceber = folha.id; abrir('recebimentos', 0, 'novo'); ui.ordemParaReceber = 0; },
    'add-item': () => {
      esconderErro();
      const s = buscar('servicos', Number($('n_servico').value));
      const peca = $('n_peca').value.trim();
      const qtd = parseInt($('n_qtd').value, 10);
      if (!s) { mostrarErro('Cadastre os serviços em Cadastros › Serviços antes de montar a ordem.'); return; }
      if (!peca) { mostrarErro('Descreva a peça (ex.: Calça jeans azul).'); $('n_peca').focus(); return; }
      if (!(qtd > 0)) { mostrarErro('A quantidade deve ser maior que zero.'); return; }
      rascunho.itens.push({ id: 0, idServico: s.id, nomeServico: s.nome, descricaoPeca: peca, quantidade: qtd, valorUnitario: s.valorPadrao });
      $('n_peca').value = '';
      $('n_qtd').value = '1';
      desenharItens();
    },
    'rem-item': (el) => { rascunho.itens.splice(Number(el.dataset.n), 1); desenharItens(); },
    'add-mat': () => {
      esconderErro();
      const m = buscar('materiais', Number($('n_material').value));
      if (!m) { mostrarErro('Selecione o material.'); return; }
      tentar(() => {
        const qtd = lerDecimal($('n_qtdmat').value || '1', 'a quantidade do material');
        if (!(qtd > 0)) falha('A quantidade deve ser maior que zero.');
        rascunho.materiais.push({ id: 0, idMaterial: m.id, descricaoMaterial: Materiais.descricao(m), unidade: m.unidade, quantidade: qtd, valorUnitario: m.valorProduto });
        $('n_qtdmat').value = '';
        desenharItens();
      });
    },
    'rem-mat': (el) => { rascunho.materiais.splice(Number(el.dataset.n), 1); desenharItens(); },
    'novo-material': () => {
      esconderErro();
      tentar(() => {
        const m = {
          id: 0, status: 'a', tipo: val('nm_tipo'), marca: '', cor: val('nm_cor').trim(), unidade: val('nm_unidade').trim(),
          qtdEstoque: lerDecimal(val('nm_qtd'), 'a quantidade em estoque'), valorProduto: lerDecimal(val('nm_valor'), 'o valor do material'),
        };
        Materiais.salvar(m);
        const sel = $('n_material');
        if (sel.options.length === 1 && !sel.options[0].value) sel.innerHTML = '';
        sel.insertAdjacentHTML('beforeend', `<option value="${m.id}">${esc(Materiais.descricao(m))} (${numero(m.qtdEstoque)} ${esc(m.unidade)})</option>`);
        sel.value = String(m.id);
        for (const k of ['nm_tipo', 'nm_cor', 'nm_qtd', 'nm_valor']) $('f_' + k).value = '';
        document.querySelector('.novo-material').open = false;
        toast('Material cadastrado. Agora toque em Adicionar material.');
      });
    },

    // recebimento
    estornar: () => confirmar('Cancelar este recebimento? A ordem volta para "Em execução".', 'Sim, cancelar recebimento', () => {
      if (tentar(() => Recebimentos.cancelar(folha.id))) { desenharFolha(); render(); toast('Recebimento cancelado.'); }
    }),
    recibo: () => {
      const r = buscar('recebimentos', folha.id);
      const os = buscar('ordens', r.idOrdem);
      $('folha-titulo').textContent = 'Recibo';
      $('folha-corpo').innerHTML = `<div class="recibo">
          <p class="recibo-marca">Costureira Iraci</p>
          <p>Recebi de <strong>${esc(Ordens.nomeCliente(os))}</strong> a importância de <strong>${dinheiro(r.valor)}</strong>,
          referente à OS nº ${os.id}, paga em ${esc(FORMAS[r.forma].toLowerCase())} em ${data(r.data)}.</p>
          <p>Retirada por: ${esc(r.retiradaPor)}</p>
          ${resumoOrdem(os)}
          <p class="assinatura">Iraci — Costureira</p>
        </div>`;
      $('folha-botoes').innerHTML = (podeImprimir ? botaoHTML({ rotulo: 'Imprimir', acao: 'imprimir', tipo: 'principal' }) : '') + botaoHTML({ rotulo: 'Voltar', acao: 'voltar-ver' });
    },
    'voltar-ver': () => desenharFolha(),
    imprimir: () => window.print(),

    // cliente → medidas (<<extend>>)
    'medidas-cliente': () => { ui.medidasCliente = folha.id; ui.cadastro = 'medidas'; fecharFolha(); irPara('cadastros'); },

    // navegação
    'ir-cadastro': (el) => { ui.cadastro = el.dataset.cad; if (ui.cadastro !== 'medidas') ui.medidasCliente = 0; irPara('cadastros'); },
    'ir-ordens': (el) => { ui.filtroOrdens = el.dataset.filtro; irPara('ordens'); },
    'filtro-ordens': (el) => { ui.filtroOrdens = el.dataset.filtro; render(); },
    'ir-relatorio': (el) => { ui.relatorio = Number(el.dataset.rel); irPara('relatorios'); },

    // dados
    exemplo: () => {
      if (!bdVazio()) return;
      carregarExemplo();
      render();
      toast('Dados de exemplo carregados.');
    },
    apagar: () => { ui.confirmarApagar = true; render(); },
    'apagar-nao': () => { ui.confirmarApagar = false; render(); },
    'apagar-sim': () => { db = vazio(); gravar(); ui.confirmarApagar = false; ui.medidasCliente = 0; render(); toast('Todos os dados foram apagados.'); },
  };

  document.addEventListener('click', (e) => {
    const nav = e.target.closest('[data-tela]');
    if (nav) { irPara(nav.dataset.tela); return; }
    const el = e.target.closest('[data-acao]');
    if (el && ACOES[el.dataset.acao]) ACOES[el.dataset.acao](el);
  });

  // pesquisa nas listas (sem redesenhar, para não perder o foco)
  document.addEventListener('input', (e) => {
    const t = e.target;
    if (t.dataset && t.dataset.buscaLista) {
      const termo = t.value.trim().toLowerCase();
      const lista = $('lista_' + t.dataset.buscaLista);
      if (!lista) return;
      for (const li of lista.children) {
        const b = li.firstElementChild;
        li.hidden = !!termo && !b.dataset.busca.includes(termo);
      }
    }
  });

  document.addEventListener('change', (e) => {
    const t = e.target;
    if (t.id === 'rel_escolha') { ui.relatorio = Number(t.value); render(); return; }
    if (t.id === 'filtro_medidas') { ui.medidasCliente = Number(t.value); render(); return; }
    if (t.dataset && t.dataset.filtroRel) {
      filtrosDoRelatorio(ui.relatorio)[t.dataset.filtroRel] = t.value;
      const foco = t.id;
      render();
      const el = $(foco);
      if (el) el.focus();
    }
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && folha) ACOES['fechar-folha']();
  });

  // =====================================================================
  // Início
  // =====================================================================
  const dataHoje = $('hoje-data');
  if (dataHoje) dataHoje.textContent = new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date());
  const inicial = (location.hash || '').replace('#', '');
  if (TELAS[inicial]) ui.tela = inicial;
  render();
})();
