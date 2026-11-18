package br.com.starlog.dao;

import br.com.starlog.model.Produto;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementação de {@link ProdutoDAO} sobre SQLite via JDBC.
 * Cada operação abre e fecha a própria conexão (try-with-resources).
 */
public class ProdutoDAOSqlite implements ProdutoDAO {

    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS produtos (
                id      INTEGER PRIMARY KEY AUTOINCREMENT,
                nome    TEXT    NOT NULL,
                preco   REAL    NOT NULL,
                estoque INTEGER NOT NULL
            )""";

    private static final String SQL_INSERT =
            "INSERT INTO produtos (nome, preco, estoque) VALUES (?, ?, ?)";
    private static final String SQL_SELECT_POR_ID =
            "SELECT id, nome, preco, estoque FROM produtos WHERE id = ?";
    private static final String SQL_SELECT_TODOS =
            "SELECT id, nome, preco, estoque FROM produtos ORDER BY nome ASC";
    private static final String SQL_UPDATE =
            "UPDATE produtos SET nome = ?, preco = ?, estoque = ? WHERE id = ?";
    private static final String SQL_DELETE =
            "DELETE FROM produtos WHERE id = ?";

    private final String url;

    /**
     * @param url URL JDBC do banco, ex.: {@code jdbc:sqlite:starlog.db}
     */
    public ProdutoDAOSqlite(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL JDBC não pode ser nula ou vazia");
        }
        this.url = url;
        criarTabela();
    }

    private Connection conectar() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private void criarTabela() {
        try (Connection conn = conectar();
             Statement stmt = conn.createStatement()) {
            stmt.execute(DDL);
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao criar a tabela produtos", e);
        }
    }

    @Override
    public void salvar(Produto produto) {
        if (produto == null) {
            throw new IllegalArgumentException("Produto não pode ser nulo");
        }
        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, produto.getNome());
            stmt.setDouble(2, produto.getPreco());
            stmt.setInt(3, produto.getEstoque());
            stmt.executeUpdate();

            try (ResultSet chaves = stmt.getGeneratedKeys()) {
                if (!chaves.next()) {
                    throw new SQLException("O driver não retornou o id gerado");
                }
                produto.setId(chaves.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao salvar produto", e);
        }
    }

    @Override
    public Optional<Produto> buscarPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_POR_ID)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao buscar produto id=" + id, e);
        }
    }

    @Override
    public List<Produto> listarTodos() {
        List<Produto> produtos = new ArrayList<>();
        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_TODOS);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                produtos.add(mapear(rs));
            }
            return produtos;
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao listar produtos", e);
        }
    }

    @Override
    public boolean atualizar(Produto produto) {
        if (produto == null || produto.getId() == null) {
            throw new IllegalArgumentException("Produto e id não podem ser nulos para atualizar");
        }
        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            stmt.setString(1, produto.getNome());
            stmt.setDouble(2, produto.getPreco());
            stmt.setInt(3, produto.getEstoque());
            stmt.setLong(4, produto.getId());
            int afetadas = stmt.executeUpdate();
            return afetadas > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao atualizar produto id=" + produto.getId(), e);
        }
    }

    @Override
    public boolean excluir(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            stmt.setLong(1, id);
            int afetadas = stmt.executeUpdate();
            return afetadas > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao excluir produto id=" + id, e);
        }
    }

    /** Hidrata a linha atual do ResultSet usando o construtor de persistência. */
    private Produto mapear(ResultSet rs) throws SQLException {
        return new Produto(
                rs.getLong("id"),
                rs.getString("nome"),
                rs.getDouble("preco"),
                rs.getInt("estoque"));
    }
}
