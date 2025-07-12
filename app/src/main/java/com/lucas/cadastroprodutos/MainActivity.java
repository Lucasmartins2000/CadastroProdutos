package com.lucas.cadastroprodutos;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    EditText edtNome, edtDescricao, edtEstoqueMaximo, edtPontoPedido, edtEstoqueMinimo;
    Spinner spinnerUnidade;
    Button btnSalvar, btnListar, btnNovaUnidade;
    DBHelper dbHelper;

    ArrayList<String> listaUnidades = new ArrayList<>();
    ArrayList<Integer> listaIdsUnidade = new ArrayList<>();
    int idUnidadeSelecionada = -1;

    boolean modoEdicao = false;
    int idProduto = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtNome = findViewById(R.id.edtNome);
        edtDescricao = findViewById(R.id.edtDescricao);
        edtEstoqueMaximo = findViewById(R.id.edtEstoqueMaximo);
        edtPontoPedido = findViewById(R.id.edtPontoPedido);
        edtEstoqueMinimo = findViewById(R.id.edtEstoqueMinimo);
        spinnerUnidade = findViewById(R.id.spinnerUnidade);
        btnSalvar = findViewById(R.id.btnSalvar);
        btnListar = findViewById(R.id.btnListar);
        btnNovaUnidade = findViewById(R.id.btnNovaUnidade);

        dbHelper = new DBHelper(this);

        carregarUnidadesRemotas();

        sincronizarProdutosPendentes();

        Intent intent = getIntent();
        if (intent.hasExtra("id")) {
            idProduto = intent.getIntExtra("id", -1);
            edtNome.setText(intent.getStringExtra("nome"));
            edtDescricao.setText(intent.getStringExtra("descricao"));
            edtEstoqueMaximo.setText(String.valueOf(intent.getIntExtra("estoque_maximo", 0)));
            edtPontoPedido.setText(String.valueOf(intent.getIntExtra("ponto_pedido", 0)));
            edtEstoqueMinimo.setText(String.valueOf(intent.getIntExtra("estoque_minimo", 0)));
            btnSalvar.setText("Atualizar Produto");
            modoEdicao = true;
        }

        btnSalvar.setOnClickListener(v -> {
            String nome = edtNome.getText().toString();
            String descricao = edtDescricao.getText().toString();
            String estMaxStr = edtEstoqueMaximo.getText().toString();
            String pntPedStr = edtPontoPedido.getText().toString();
            String estMinStr = edtEstoqueMinimo.getText().toString();

            if (nome.isEmpty() || estMaxStr.isEmpty() || pntPedStr.isEmpty() || estMinStr.isEmpty() || spinnerUnidade.getSelectedItemPosition() == -1) {
                Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
                return;
            }

            int estoqueMaximo = Integer.parseInt(estMaxStr);
            int pontoPedido = Integer.parseInt(pntPedStr);
            int estoqueMinimo = Integer.parseInt(estMinStr);
            idUnidadeSelecionada = listaIdsUnidade.get(spinnerUnidade.getSelectedItemPosition());

            boolean resultado;
            if (modoEdicao && idProduto != -1) {
                resultado = dbHelper.atualizarProdutoPorId(idProduto, nome, descricao, estoqueMaximo, pontoPedido, estoqueMinimo);
                Toast.makeText(this, resultado ? "Produto atualizado!" : "Erro ao atualizar!", Toast.LENGTH_SHORT).show();
            } else {
                resultado = dbHelper.inserirProduto(nome, descricao, estoqueMaximo, pontoPedido, estoqueMinimo, idUnidadeSelecionada);
                Toast.makeText(this, resultado ? "Produto salvo!" : "Erro ao salvar!", Toast.LENGTH_SHORT).show();
            }

            if (resultado && verificarConexao()) {
                enviarProdutoParaAPI(nome, descricao, estoqueMaximo, pontoPedido, estoqueMinimo, idUnidadeSelecionada);
            }

            edtNome.setText("");
            edtDescricao.setText("");
            edtEstoqueMaximo.setText("");
            edtPontoPedido.setText("");
            edtEstoqueMinimo.setText("");
            spinnerUnidade.setSelection(0);
            btnSalvar.setText("Salvar Produto");
            modoEdicao = false;
            idProduto = -1;
        });

        btnListar.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ListActivity.class)));

        btnNovaUnidade.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CadastroUnidadeActivity.class));
        });
    }

    private void carregarUnidadesRemotas() {
        boolean temInternet = verificarConexao();

        if (!temInternet) {
            Cursor cursor = dbHelper.listarUnidades();
            listaUnidades.clear();
            listaIdsUnidade.clear();

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id_unmedi"));
                    String descricao = cursor.getString(cursor.getColumnIndexOrThrow("descricao"));
                    listaIdsUnidade.add(id);
                    listaUnidades.add(descricao);
                } while (cursor.moveToNext());
            }
            cursor.close();

            runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, listaUnidades);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerUnidade.setAdapter(adapter);
            });

            return;
        }

        AsyncTask.execute(() -> {
            try {
                URL url = new URL("http://160.20.22.99:5300/listar-unidades");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String linha;
                    while ((linha = reader.readLine()) != null) {
                        response.append(linha);
                    }
                    reader.close();

                    JSONArray unidades = new JSONArray(response.toString());
                    listaUnidades.clear();
                    listaIdsUnidade.clear();

                    for (int i = 0; i < unidades.length(); i++) {
                        JSONObject obj = unidades.getJSONObject(i);
                        int id = obj.getInt("id_unmedi");
                        String nome = obj.getString("descricao");
                        listaIdsUnidade.add(id);
                        listaUnidades.add(nome);
                    }

                    runOnUiThread(() -> {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, listaUnidades);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerUnidade.setAdapter(adapter);
                    });
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Erro ao carregar unidades", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void enviarProdutoParaAPI(String nome, String descricao, int estoqueMaximo, int pontoPedido, int estoqueMinimo, int idUnidade) {
        AsyncTask.execute(() -> {
            try {
                URL url = new URL("http://160.20.22.99:5300/cadastrar-produto");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                String json = "{" +
                        "\"nome\":\"" + nome + "\"," +
                        "\"descricao\":\"" + descricao + "\"," +
                        "\"estoque_maximo\":" + estoqueMaximo + "," +
                        "\"ponto_pedido\":" + pontoPedido + "," +
                        "\"estoque_minimo\":" + estoqueMinimo + "," +
                        "\"id_unmedi\":" + idUnidade +
                        "}";

                OutputStream os = conn.getOutputStream();
                os.write(json.getBytes("UTF-8"));
                os.flush();
                os.close();

                int resp = conn.getResponseCode();
                if (resp == 200) {
                    Cursor cursor = dbHelper.listarProdutosNaoSincronizados();
                    if (cursor.moveToLast()) {
                        int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                        dbHelper.marcarProdutoComoSincronizado(id);
                    }
                    cursor.close();
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void sincronizarProdutosPendentes() {
        if (!verificarConexao()) return;

        Cursor cursor = dbHelper.listarProdutosNaoSincronizados();
        if (cursor.moveToFirst()) {
            do {
                String nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                String descricao = cursor.getString(cursor.getColumnIndexOrThrow("descricao"));
                int estoqueMaximo = cursor.getInt(cursor.getColumnIndexOrThrow("estoque_maximo"));
                int pontoPedido = cursor.getInt(cursor.getColumnIndexOrThrow("ponto_pedido"));
                int estoqueMinimo = cursor.getInt(cursor.getColumnIndexOrThrow("estoque_minimo"));
                int idUnidade = cursor.getInt(cursor.getColumnIndexOrThrow("id_unmedi"));

                enviarProdutoParaAPI(nome, descricao, estoqueMaximo, pontoPedido, estoqueMinimo, idUnidade);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private boolean verificarConexao() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
}
