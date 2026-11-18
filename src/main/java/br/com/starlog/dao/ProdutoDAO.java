package br.com.starlog.dao;

import br.com.starlog.model.Produto;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistência de {@link Produto}, independente de banco de dados.
 */
public interface ProdutoDAO {

    /** Insere o produto e sincroniza o id gerado via {@link Produto#setId(Long)}. */
    void salvar(Produto produto);

    /** Retorna o produto com o id informado, ou {@link Optional#empty()} se não existir. */
    Optional<Produto> buscarPorId(Long id);

    /** Retorna todos os produtos cadastrados (lista vazia se não houver nenhum). */
    List<Produto> listarTodos();

    /** Atualiza o produto existente; retorna {@code true} se algum registro foi alterado. */
    boolean atualizar(Produto produto);

    /** Remove o produto pelo id; retorna {@code true} se algum registro foi removido. */
    boolean excluir(Long id);
}
