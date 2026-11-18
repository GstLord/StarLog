# StarLog — CRUD de Produtos (Java + JDBC + SQLite)

Ciclo de vida completo de persistência (**Create, Read, Update, Delete**) de produtos, com invariantes
de domínio garantidas na entidade e um DAO desacoplado do banco de dados.

## Requisitos

- Java 17+
- Maven 3.8+

## Como rodar

```
mvn compile exec:java
```

O `Main` apaga e recria o arquivo `starlog.db` a cada execução, para que a simulação seja determinística.

## Estrutura

```
src/main/java/br/com/starlog/
├── Main.java                   <- simulação do ciclo completo de negócio
├── model/
│   └── Produto.java            <- entidade + invariantes
└── dao/
    ├── ProdutoDAO.java         <- contrato do CRUD (sem acoplamento a banco)
    └── ProdutoDAOSqlite.java   <- implementação JDBC/SQLite
```

## Entidade `Produto`

| Atributo  | Tipo     | Regra (viola → `IllegalArgumentException`)                         |
|-----------|----------|--------------------------------------------------------------------|
| `id`      | `Long`   | gerado pelo banco (`null` antes de salvar)                         |
| `nome`    | `String` | não nulo, ao menos 3 caracteres (espaços nas pontas são ignorados) |
| `preco`   | `double` | maior que 0.0 (`NaN` e infinito também são rejeitados)             |
| `estoque` | `int`    | não negativo (0 é válido)                                          |

- **Construtor de persistência** `Produto(Long id, String nome, double preco, int estoque)`: hidrata registros do banco.
- **Construtor de negócio** `Produto(String nome, double preco, int estoque)`: delega ao de persistência com `id = null`.
- `setPreco` e `setEstoque` usam os mesmos validadores dos construtores; um valor inválido não altera o objeto.
- `setId` sincroniza a chave gerada pelo banco após o `salvar`.

## Contrato `ProdutoDAO`

```java
public interface ProdutoDAO {
    void salvar(Produto produto);
    Optional<Produto> buscarPorId(Long id);
    List<Produto> listarTodos();
    boolean atualizar(Produto produto);
    boolean excluir(Long id);
}
```

## Implementação `ProdutoDAOSqlite`

```java
ProdutoDAO dao = new ProdutoDAOSqlite("jdbc:sqlite:starlog.db");
```

- **DDL automático no construtor**:
  ```sql
  CREATE TABLE IF NOT EXISTS produtos (
      id      INTEGER PRIMARY KEY AUTOINCREMENT,
      nome    TEXT    NOT NULL,
      preco   REAL    NOT NULL,
      estoque INTEGER NOT NULL
  )
  ```
- **`salvar`** grava o produto e preenche o id autoincrement no objeto via `setId`.
- **`buscarPorId`** devolve `Optional.empty()` quando o id não existe.
- **`listarTodos`** usa `ORDER BY nome ASC` para manter uma ordem estável na exibição.
- **`atualizar` / `excluir`** inspecionam `int afetadas = stmt.executeUpdate()` e retornam `afetadas > 0`.
- Cada operação abre e fecha a própria conexão (`try-with-resources`). Por isso, use um arquivo `.db`;
  o banco em memória (`jdbc:sqlite::memory:`) não mantém dados entre chamadas.
- Argumentos nulos geram `IllegalArgumentException`; falhas de SQL são relançadas como `RuntimeException` com a causa original.

## Simulação no `Main`

| Etapa           | O que faz                                                                                  |
|-----------------|--------------------------------------------------------------------------------------------|
| **Create**      | cadastra "Teclado Mecânico RGB" e "Mousepad Speed XL" e mostra os ids gerados               |
| **Read**        | lista todos os produtos em colunas (ID, NOME, PREÇO, ESTOQUE)                              |
| **Update**      | teclado: estoque 15 e preço R$ 289,90; confere o retorno `true` e relê o registro do banco |
| **Delete**      | remove o mousepad pelo id e tenta excluir o id inexistente 888 (retorno `false` tratado)   |
| **Final State** | lista os produtos e confirma que restou exatamente 1                                       |

Saída esperada (resumida):

```
== [CREATE] Cadastrando produtos ==
Salvo: Teclado Mecânico RGB   -> id gerado = 1
Salvo: Mousepad Speed XL      -> id gerado = 2
...
== [UPDATE] Teclado: estoque -> 15, preço -> R$ 289,90 ==
atualizar() retornou: true
...
== [DELETE] Removendo produtos ==
id 2 (Mousepad (id real)): excluído com sucesso.
id 888 (id inexistente): nenhum produto com esse id, nada foi excluído.

== [FINAL STATE] Estado final do banco ==
--------------------------------------------------------
ID    NOME                                PREÇO  ESTOQUE
--------------------------------------------------------
1     Teclado Mecânico RGB            R$ 289,90       15
--------------------------------------------------------
Total de produtos no banco: 1 (OK: exatamente 1)
```

> Antes da saída podem aparecer avisos do SLF4J e do Java (`restricted method`) emitidos pelo driver
> SQLite. Eles não afetam a execução.

## Checklist

- [x] Validações no construtor de persistência (nome, preço, estoque)
- [x] Construtor de negócio delegando para o de persistência
- [x] `setPreco` e `setEstoque` respeitando as mesmas invariantes
- [x] Contrato `ProdutoDAO` + implementação `ProdutoDAOSqlite`
- [x] `Main`: simulação do ciclo completo (Create, Read, Update, Delete, Final State)
- [ ] `Main`: demonstrar casos inválidos (`IllegalArgumentException`) e o limite estoque 0
