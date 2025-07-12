package com.lucas.cadastroprodutos;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class ListActivity extends AppCompatActivity {
    DBHelper dbHelper;
    RecyclerView recyclerView;
    SwipeRefreshLayout swipeRefresh;
    ArrayList<String> listaProdutos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        recyclerView = findViewById(R.id.recyclerViewProdutos);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        swipeRefresh = findViewById(R.id.swipeRefresh);

        dbHelper = new DBHelper(this);
        carregarProdutos();

        swipeRefresh.setOnRefreshListener(() -> {
            carregarProdutos();
            swipeRefresh.setRefreshing(false);
        });
    }

    private void carregarProdutos() {
        listaProdutos = new ArrayList<>();

        // 1. Carrega produtos do banco local
        try {
            Cursor cursor = dbHelper.listarProdutos();
            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                    String descricao = cursor.getString(cursor.getColumnIndexOrThrow("descricao"));
                    int estoqueMaximo = cursor.getInt(cursor.getColumnIndexOrThrow("estoque_maximo"));
                    int pontoPedido = cursor.getInt(cursor.getColumnIndexOrThrow("ponto_pedido"));
                    int estoqueMinimo = cursor.getInt(cursor.getColumnIndexOrThrow("estoque_minimo"));

                    // Prefixo LOCAL
                    String item = "LOCAL;" + id + ";" + nome + ";" + descricao + ";" +
                            "Máx: " + estoqueMaximo + ";" +
                            "Ponto: " + pontoPedido + ";" +
                            "Mín: " + estoqueMinimo;

                    listaProdutos.add(item);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Busca produtos do servidor remoto
        new Thread(() -> {
            try {
                URL url = new URL("http://160.20.22.99:5300/listar-produtos");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONArray produtos = new JSONArray(response.toString());

                    for (int i = 0; i < produtos.length(); i++) {
                        JSONObject p = produtos.getJSONObject(i);
                        String nome = p.optString("nome", "Sem nome");
                        String descricao = p.optString("descricao", "");
                        int estoqueMaximo = p.optInt("estoque_maximo", 0);
                        int pontoPedido = p.optInt("ponto_pedido", 0);
                        int estoqueMinimo = p.optInt("estoque_minimo", 0);

                        // Prefixo REMOTO
                        String item = "REMOTO;" + nome + ";" + descricao + ";" +
                                "Máx: " + estoqueMaximo + ";" +
                                "Ponto: " + pontoPedido + ";" +
                                "Mín: " + estoqueMinimo;

                        listaProdutos.add(item);
                    }

                    // Atualiza a lista na tela (na thread principal)
                    runOnUiThread(() -> {
                        ProdutoAdapter adapter = new ProdutoAdapter(listaProdutos);
                        recyclerView.setAdapter(adapter);
                    });

                } else {
                    Log.e("API", "Erro ao buscar produtos remotos. Código: " + responseCode);
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("API", "❌ Erro na conexão com a API.");
            }
        }).start();

        // Atualiza lista local imediatamente
        ProdutoAdapter adapter = new ProdutoAdapter(listaProdutos);
        recyclerView.setAdapter(adapter);
    }
}
