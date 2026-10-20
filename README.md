# Sistema de Controle de Serviços – Costureira Iraci

Trabalho acadêmico de **Vinícius Mendonça Costardi**. É um software desktop em **Java (Swing)**, feito a partir do
diagrama de classes, do diagrama de caso de uso e do documento de requisitos que estão em [`docs/`](docs).

- Não usa nenhuma biblioteca externa nem banco de dados para instalar: basta ter o **JDK 8 ou superior**.
- Os dados ficam gravados no arquivo `dados/costureira.dat`, criado na pasta onde o programa é executado.
- Na primeira vez que abre, o sistema pergunta se você quer carregar **dados de exemplo** para testar.

## Como executar

**Windows:** dê dois cliques em `executar.bat`.
**Linux/macOS:** `./executar.sh`

**NetBeans / IntelliJ / Eclipse:** abra a pasta como projeto Maven (`pom.xml`) e execute a classe `costureira.Main`.
Pelo Maven no terminal: `mvn package` e depois `java -jar target/costureira-iraci.jar`.

## O que o sistema tem

| Enunciado | Onde está no sistema |
|---|---|
| Cadastro de cliente | Cadastros › Clientes (com botão **Medidas do cliente...**) |
| Cadastro de serviços | Cadastros › Serviços oferecidos |
| Medidas e Materiais (diagrama de classes) | Cadastros › Medidas dos clientes / Materiais |
| Movimentação de ordem de serviço / agendamento | Movimentações › Ordens de serviço |
| Movimentação de recebimento (à vista, na entrega) | Movimentações › Recebimentos |
| Pelo menos 4 relatórios | Menu Relatórios (7 relatórios) |

A tela principal mostra um resumo: ordens agendadas, em execução, **entregas atrasadas** e o total recebido no mês.

### Casos de uso → telas

| Caso de uso (diagrama) | Tela / botão |
|---|---|
| Cadastrar cliente | Tela de clientes |
| Cadastrar medidas `<<extend>>` Cadastrar cliente | Botão **Medidas do cliente...** na tela de clientes |
| Cadastrar serviço `<<include>>` Adicionar itens do serviço | Tela de ordens de serviço: a ordem só é salva com pelo menos um item (peça + serviço) |
| Cadastrar materiais `<<extend>>` Adicionar itens do serviço | Aba **Materiais utilizados** da ordem, com botão **Novo material...** |
| Registrar recebimento `<<include>>` Consultar serviço | Tela de recebimentos: ao escolher a ordem, o quadro **Consultar serviço** mostra o resumo dela |

### Métodos do diagrama de classes → botões

Todas as telas de cadastro têm os botões **Cadastrar, Editar, Salvar, Cancelar e Excluir**, que correspondem aos
métodos `Cadastrar()`, `Editar()`, `Salvar()`, `Cancelar()` e `Excluir()` das classes. Nas classes do pacote
`model`, `salvar()` valida as regras de negócio e grava, e `excluir()` só remove se as regras permitirem.
Na ordem de serviço e no recebimento, `cancelar()` também existe como movimentação (botões **Cancelar OS** e
**Cancelar recebimento**).

## Modelo de dados

O diagrama de classes da imagem foi seguido, separando o "Servico" em duas partes, como pede o enunciado
(cadastro de serviços + movimentação de ordem de serviço):

| Classe Java | Corresponde a | Atributos principais |
|---|---|---|
| `Cliente` | Cliente | id, status (a/i), nome, cpf, telefone, e-mail, gênero (f/m/n), data de nascimento, endereço, data de cadastro |
| `Medida` | Medidas | id, status, cliente, tipo de medida, valor (cm), data da aferição |
| `Material` | Materiais | id, status, tipo, marca, cor, unidade, qtd. em estoque, valor do produto |
| `Servico` | serviço (cadastro) | id, status, nome, descrição, valor padrão, tempo estimado (min) |
| `OrdemServico` | Servico (movimentação) / ordem de serviço | id, status (a/c/e/f), cliente, data e hora do agendamento, entrega prevista, data de entrega/retirada, retirada por, valor total, obs. |
| `ItemOrdemServico` | Itens servico | id, ordem, serviço, descrição da peça, quantidade, valor unitário, subtotal |
| `ItemMaterial` | Servico — Materiais | id, ordem, material, quantidade, valor unitário, subtotal |
| `Recebimento` | Recebimento | id, status, ordem, data, forma de pagamento (d/p/db/cr), valor recebido, retirada por, obs. |

A relação N:N cliente ↔ serviço é resolvida por `OrdemServico` (de um cliente) + `ItemOrdemServico` (que liga a
ordem aos serviços).

## Regras de negócio implementadas

- O recebimento é sempre à vista, na entrega, e o **valor recebido tem que ser igual ao total da ordem**.
- Cada ordem tem no máximo um recebimento. Registrar o recebimento **finaliza** a ordem (entrega).
- Não dá para registrar recebimento de ordem cancelada nem de ordem ainda agendada.
- A ordem nasce **Agendada**, passa para **Em execução** (botão *Iniciar execução*) e termina **Finalizada**
  (no recebimento). Pode ser **Cancelada** enquanto não estiver finalizada.
- Ordem finalizada ou cancelada não pode mais ser editada.
- Toda ordem precisa ter pelo menos um item.
- Só é possível fazer ordem para cliente, serviço e material **ativos**.
- O valor unitário do item é copiado do valor padrão do serviço na hora (mudar o preço depois não altera
  ordens antigas). Subtotal = quantidade × valor unitário. Total = soma dos itens + materiais.
- A data prevista de entrega não pode ser anterior à data do agendamento.
- O CPF tem que ser válido e não pode se repetir.
- Cliente, serviço ou material que já foi usado em ordem não pode ser excluído, só inativado.
- Materiais usados saem do estoque (e voltam se a ordem for cancelada); não deixa usar mais do que tem.
- Ordens com entrega vencida e não entregues aparecem **em vermelho**.

## Relatórios

1. Ordens de serviço por período e situação (detalhado ou agrupado por dia, semana ou mês)
2. Serviços mais solicitados (ranking por quantidade e valor)
3. Recebimentos por período e forma de pagamento (com total e subtotal por forma)
4. Clientes que mais utilizaram os serviços
5. Entregas pendentes e atrasadas
6. Clientes cadastrados (ativos / inativos)
7. Estoque de materiais (destaca o que precisa comprar)

Todos podem ser impressos pelo botão **Imprimir** (no Windows dá para salvar em PDF escolhendo a impressora "Microsoft Print to PDF").

## Estrutura do código

```
src/main/java/costureira/
├── Main.java              ponto de entrada
├── DadosExemplo.java      dados fictícios para teste
├── model/                 classes do diagrama + regras de negócio
├── dao/BancoDados.java    gravação dos dados em arquivo
├── relatorio/             os relatórios
├── util/                  formatação de datas/dinheiro, validação de CPF
└── view/                  telas (Swing)
docs/                      diagramas e documento de requisitos
```
