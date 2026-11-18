package br.com.starlog;

import br.com.starlog.dao.ProdutoDAO;
import br.com.starlog.dao.ProdutoDAOSqlite;
import br.com.starlog.model.Produto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class Main {

    private static final Path ARQUIVO_BANCO = Path.of("starlog.db");
    private static final Locale PT_BR = new Locale("pt", "BR");

    private static final String LINHA = "-".repeat(56);
    private static final String FORMATO_LINHA = "%-5s %-28s %12s %8s%n";

    public static void main(String[] args) throws IOException {
        // Banco limpo a cada execução: a simulação precisa ser determinística
        Files.deleteIfExists(ARQUIVO_BANCO);
        ProdutoDAO dao = new ProdutoDAOSqlite("jdbc:sqlite:" + ARQUIVO_BANCO);

        // ---------------------------------------------------------------- CREATE
        titulo("[CREATE] Cadastrando produtos");
        Produto teclado = new Produto("Teclado Mecânico RGB", 349.90, 8);
        Produto mousepad = new Produto("Mousepad Speed XL", 89.90, 25);

        dao.salvar(teclado);
        System.out.printf("Salvo: %-22s -> id gerado = %d%n", teclado.getNome(), teclado.getId());
        dao.salvar(mousepad);
        System.out.printf("Salvo: %-22s -> id gerado = %d%n", mousepad.getNome(), mousepad.getId());

        // ------------------------------------------------------------------ READ
        titulo("[READ] Listando todos os produtos");
        imprimirTabela(dao.listarTodos());

        // ---------------------------------------------------------------- UPDATE
        titulo("[UPDATE] Teclado: estoque -> 15, preço -> R$ 289,90");
        teclado.setEstoque(15);
        teclado.setPreco(289.90);
        boolean atualizado = dao.atualizar(teclado);
        System.out.println("atualizar() retornou: " + atualizado);

        if (atualizado) {
            Optional<Produto> doBanco = dao.buscarPorId(teclado.getId());
            System.out.println("Registro relido do banco:");
            imprimirTabela(doBanco.map(List::of).orElse(List.of()));
        } else {
            System.out.println("Nenhuma linha afetada: o teclado não foi encontrado no banco.");
        }

        // ---------------------------------------------------------------- DELETE
        titulo("[DELETE] Removendo produtos");
        excluirComFeedback(dao, mousepad.getId(), "Mousepad (id real)");
        excluirComFeedback(dao, 888L, "id inexistente");

        // ----------------------------------------------------------- FINAL STATE
        titulo("[FINAL STATE] Estado final do banco");
        List<Produto> restantes = dao.listarTodos();
        imprimirTabela(restantes);
        System.out.printf("Total de produtos no banco: %d %s%n",
                restantes.size(), restantes.size() == 1 ? "(OK: exatamente 1)" : "(ESPERADO: 1)");
    }

    /** Trata o retorno booleano do DAO sem lançar exceção quando nada é excluído. */
    private static void excluirComFeedback(ProdutoDAO dao, Long id, String descricao) {
        boolean excluido = dao.excluir(id);
        if (excluido) {
            System.out.printf("id %d (%s): excluído com sucesso.%n", id, descricao);
        } else {
            System.out.printf("id %d (%s): nenhum produto com esse id, nada foi excluído.%n", id, descricao);
        }
    }

    private static void imprimirTabela(List<Produto> produtos) {
        System.out.println(LINHA);
        System.out.printf(FORMATO_LINHA, "ID", "NOME", "PREÇO", "ESTOQUE");
        System.out.println(LINHA);
        if (produtos.isEmpty()) {
            System.out.println("(nenhum produto)");
        }
        for (Produto p : produtos) {
            System.out.printf(FORMATO_LINHA,
                    p.getId(),
                    p.getNome(),
                    String.format(PT_BR, "R$ %,.2f", p.getPreco()),
                    p.getEstoque());
        }
        System.out.println(LINHA);
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println("== " + texto + " ==");
    }
}
