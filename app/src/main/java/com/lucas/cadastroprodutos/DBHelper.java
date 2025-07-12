package com.lucas.cadastroprodutos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "produtosDB";
    private static final int DB_VERSION = 4; // versão alterada para recriar todas as tabelas

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tabela de produtos
        db.execSQL(
                "CREATE TABLE produtos (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "nome TEXT, " +
                        "descricao TEXT, " +
                        "estoque_maximo INTEGER, " +
                        "ponto_pedido INTEGER, " +
                        "estoque_minimo INTEGER, " +
                        "id_unmedi INTEGER, " +
                        "sincronizado INTEGER DEFAULT 0)"
        );

        // Tabela de unidades de medida
        db.execSQL(
                "CREATE TABLE unimedida (" +
                        "id_unmedi INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "descricao TEXT NOT NULL, " +
                        "uniabrev TEXT, " +
                        "sincronizado INTEGER DEFAULT 0)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS produtos");
        db.execSQL("DROP TABLE IF EXISTS unimedida");
        onCreate(db);
    }

    // === PRODUTOS ===

    public boolean inserirProduto(String nome, String descricao, int estoqueMaximo, int pontoPedido, int estoqueMinimo, int idUnmedi) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put("nome", nome);
        valores.put("descricao", descricao);
        valores.put("estoque_maximo", estoqueMaximo);
        valores.put("ponto_pedido", pontoPedido);
        valores.put("estoque_minimo", estoqueMinimo);
        valores.put("id_unmedi", idUnmedi);
        valores.put("sincronizado", 0);
        long resultado = db.insert("produtos", null, valores);
        return resultado != -1;
    }

    public Cursor listarProdutos() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM produtos", null);
    }

    public Cursor listarProdutosNaoSincronizados() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM produtos WHERE sincronizado = 0", null);
    }

    public void marcarProdutoComoSincronizado(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put("sincronizado", 1);
        db.update("produtos", valores, "id = ?", new String[]{String.valueOf(id)});
    }

    public boolean atualizarProdutoPorId(int id, String nome, String descricao, int estoqueMaximo, int pontoPedido, int estoqueMinimo) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put("nome", nome);
        valores.put("descricao", descricao);
        valores.put("estoque_maximo", estoqueMaximo);
        valores.put("ponto_pedido", pontoPedido);
        valores.put("estoque_minimo", estoqueMinimo);
        int linhasAfetadas = db.update("produtos", valores, "id = ?", new String[]{String.valueOf(id)});
        return linhasAfetadas > 0;
    }

    // === UNIMEDIDA ===

    public boolean inserirUnidade(String descricao, String uniabrev) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put("descricao", descricao);
        valores.put("uniabrev", uniabrev);
        valores.put("sincronizado", 0);
        long resultado = db.insert("unimedida", null, valores);
        return resultado != -1;
    }

    public Cursor listarUnidades() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM unimedida", null);
    }

    public Cursor listarUnidadesNaoSincronizadas() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM unimedida WHERE sincronizado = 0", null);
    }

    public void marcarUnidadeComoSincronizada(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put("sincronizado", 1);
        db.update("unimedida", valores, "id_unmedi = ?", new String[]{String.valueOf(id)});
    }

    public boolean unidadeExiste(String descricao) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM unimedida WHERE descricao = ?", new String[]{descricao});
        boolean existe = cursor.moveToFirst();
        cursor.close();
        return existe;
    }
}
