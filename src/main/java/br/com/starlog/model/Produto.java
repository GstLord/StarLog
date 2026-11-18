package br.com.starlog.model;

public class Produto {

    private static final int NOME_MIN = 3;

    private Long id;
    private final String nome;
    private double preco;
    private int estoque;

    /** Construtor de persistência — hidrata registros vindos do banco. */
    public Produto(Long id, String nome, double preco, int estoque) {
        this.id = id;
        this.nome = validarNome(nome);
        this.preco = validarPreco(preco);
        this.estoque = validarEstoque(estoque);
    }

    /** Construtor de negócio — id será gerado pelo banco. */
    public Produto(String nome, double preco, int estoque) {
        this(null, nome, preco, estoque);
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public double getPreco() {
        return preco;
    }

    public int getEstoque() {
        return estoque;
    }

    /** Sincroniza a PK gerada pelo banco. */
    public void setId(Long id) {
        this.id = id;
    }

    public void setPreco(double preco) {
        this.preco = validarPreco(preco);
    }

    public void setEstoque(int estoque) {
        this.estoque = validarEstoque(estoque);
    }

    // ---- Invariantes: única fonte de verdade para construtores e setters ----

    /** Nome não nulo com pelo menos 3 caracteres, desconsiderando espaços nas pontas. */
    private static String validarNome(String nome) {
        if (nome == null || nome.strip().length() < NOME_MIN) {
            throw new IllegalArgumentException(
                    "Nome deve ter ao menos " + NOME_MIN + " caracteres: " + (nome == null ? "null" : "'" + nome + "'"));
        }
        return nome.strip();
    }

    /** Preço estritamente positivo (NaN e infinito também são rejeitados). */
    private static double validarPreco(double preco) {
        if (!Double.isFinite(preco) || preco <= 0.0) {
            throw new IllegalArgumentException("Preço deve ser maior que zero: " + preco);
        }
        return preco;
    }

    /** Estoque zero é válido; apenas negativos são rejeitados. */
    private static int validarEstoque(int estoque) {
        if (estoque < 0) {
            throw new IllegalArgumentException("Estoque não pode ser negativo: " + estoque);
        }
        return estoque;
    }

    @Override
    public String toString() {
        return String.format("Produto[id=%d, nome='%s', preco=R$ %.2f, estoque=%d]",
                id, nome, preco, estoque);
    }
}
