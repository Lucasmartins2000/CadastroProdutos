const express = require('express');
const mysql = require('mysql2');
const bodyParser = require('body-parser');
const cors = require('cors');
const app = express();
const port = 5300;

app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// Pool de conexões com o banco de dados
const db = mysql.createPool({
    host: '160.20.22.99',
    user: 'aluno31',
    password: 'pVrtwsRSAo4=',
    database: 'fasiclin',
    port: 3360,
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});

// Teste inicial de conexão
db.getConnection((err, connection) => {
    if (err) {
        console.error('❌ Erro ao conectar ao banco de dados:', err);
        process.exit(1);
    }
    console.log('✅ Conexão com banco estabelecida.');
    connection.release();
});

/**
 * ============================
 *    UNIDADES DE MEDIDA
 * ============================
 */

// Cadastrar unidade de medida
app.post('/cadastrar-unidade', (req, res) => {
    const { descricao, uniabrev } = req.body;

    if (!descricao || !uniabrev) {
        return res.status(400).json({ erro: 'Descrição e abreviação (uniabrev) são obrigatórias.' });
    }
    const sql = 'INSERT INTO UNIMEDIDA (DESCRICAO, UNIABREV) VALUES (?, ?)';
    db.query(sql, [descricao.trim(), uniabrev.trim()], (err, result) => {
        if (err) {
            console.error('❌ Erro ao cadastrar unidade:', err.sqlMessage);
            return res.status(500).json({ erro: 'Erro ao cadastrar unidade.' });
        }
        res.json({ mensagem: '✅ Unidade cadastrada com sucesso!', id: result.insertId });
    });
});

// Listar unidades de medida
app.get('/listar-unidades', (req, res) => {
    const sql = 'SELECT IDUNMEDI, DESCRICAO, UNIABREV FROM UNIMEDIDA ORDER BY DESCRICAO';
    db.query(sql, (err, rows) => {
        if (err) {
            console.error('❌ Erro ao listar unidades:', err.sqlMessage);
            return res.status(500).json({ erro: 'Erro ao listar unidades.' });
        }
        const unidades = rows.map(u => ({
            id_unmedi: u.IDUNMEDI,
            descricao: u.DESCRICAO,
            uniabrev: u.UNIABREV
        }));
        res.json(unidades);
    });
});

/**
 * ============================
 *         PRODUTOS
 * ============================
 */

// Cadastrar produto
app.post('/cadastrar-produto', (req, res) => {
    const { nome, descricao, estoque_maximo, ponto_pedido, estoque_minimo, id_unmedi } = req.body;

    if (!nome || !estoque_maximo || !ponto_pedido || !estoque_minimo || !id_unmedi) {
        return res.status(400).json({ erro: 'Preencha todos os campos obrigatórios.' });
    }

    const sql = `
        INSERT INTO PRODUTO (NOME, DESCRICAO, STQMAX, PNTPEDIDO, STQMIN, ID_UNMEDI)
        VALUES (?, ?, ?, ?, ?, ?)
    `;
    db.query(
        sql,
        [nome, descricao || '', estoque_maximo, ponto_pedido, estoque_minimo, id_unmedi],
        (err, result) => {
            if (err) {
                console.error('❌ Erro ao cadastrar produto:', err.sqlMessage);
                return res.status(500).json({ erro: 'Erro ao cadastrar produto.' });
            }
            res.json({ mensagem: '✅ Produto cadastrado com sucesso!', id: result.insertId });
        }
    );
});

// Listar produtos
app.get('/listar-produtos', (req, res) => {
    // Pega os principais campos + JOIN para retornar a unidade de medida (opcional)
    const sql = `
        SELECT 
            p.IDPRODUTO, p.NOME, p.DESCRICAO, p.STQMAX, p.PNTPEDIDO, p.STQMIN, p.ID_UNMEDI,
            u.DESCRICAO as unimedida_descricao, u.UNIABREV as unimedida_abrev
        FROM PRODUTO p
        LEFT JOIN UNIMEDIDA u ON p.ID_UNMEDI = u.IDUNMEDI
        ORDER BY p.IDPRODUTO DESC
    `;
    db.query(sql, (err, rows) => {
        if (err) {
            console.error('❌ Erro ao listar produtos:', err.sqlMessage);
            return res.status(500).json({ erro: 'Erro ao listar produtos.' });
        }
        const produtos = rows.map(p => ({
            id: p.IDPRODUTO,
            nome: p.NOME,
            descricao: p.DESCRICAO,
            estoque_maximo: p.STQMAX,
            ponto_pedido: p.PNTPEDIDO,
            estoque_minimo: p.STQMIN,
            id_unmedi: p.ID_UNMEDI,
            unidade_descricao: p.unimedida_descricao,
            unidade_abrev: p.unimedida_abrev
        }));
        res.json(produtos);
    });
});

/**
 * ============================
 *         DEBUG
 * ============================
 */

// Endpoint de debug de tabela (opcional)
app.get('/api/debug/:tabela', (req, res) => {
    const tabela = req.params.tabela;
    const sql = `SELECT * FROM ${tabela} LIMIT 5`;
    db.query(sql, (err, rows) => {
        if (err) {
            console.error('Erro ao consultar tabela:', tabela, err.sqlMessage);
            return res.status(500).json({ erro: `Erro ao consultar tabela ${tabela}` });
        }
        res.json(rows);
    });
});

// Iniciar o servidor
app.listen(port, '0.0.0.0', () => {
    console.log(`🚀 API rodando em http://0.0.0.0:${port}`);
});
