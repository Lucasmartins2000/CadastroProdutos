package com.lucas.cadastroprodutos;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class CadastroUnidadeActivity extends AppCompatActivity {

    EditText edtDescricaoUnidade, edtAbreviacaoUnidade;
    Button btnSalvarUnidade;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro_unidade);

        edtDescricaoUnidade = findViewById(R.id.edtDescricaoUnidade);
        edtAbreviacaoUnidade = findViewById(R.id.edtAbreviacaoUnidade);
        btnSalvarUnidade = findViewById(R.id.btnSalvarUnidade);
        dbHelper = new DBHelper(this);

        sincronizarUnidadesPendentes();

        btnSalvarUnidade.setOnClickListener(v -> {
            String descricao = edtDescricaoUnidade.getText().toString().trim();
            String abreviacao = edtAbreviacaoUnidade.getText().toString().trim();

            if (descricao.isEmpty()) {
                Toast.makeText(this, "Preencha a descrição!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dbHelper.unidadeExiste(descricao)) {
                Toast.makeText(this, "Unidade já cadastrada!", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean resultado = dbHelper.inserirUnidade(descricao, abreviacao);
            if (resultado) {
                Toast.makeText(this, "✅ Unidade salva localmente!", Toast.LENGTH_SHORT).show();
                edtDescricaoUnidade.setText("");
                edtAbreviacaoUnidade.setText("");

                setResult(RESULT_OK);
                finish();

                if (verificarConexao()) {
                    sincronizarUnidadesPendentes();
                }
            } else {
                Toast.makeText(this, "❌ Erro ao salvar no SQLite!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sincronizarUnidadesPendentes() {
        if (!verificarConexao()) return;

        AsyncTask.execute(() -> {
            var cursor = dbHelper.listarUnidadesNaoSincronizadas();
            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id_unmedi"));
                    String descricao = cursor.getString(cursor.getColumnIndexOrThrow("descricao"));
                    String uniabrev = cursor.getString(cursor.getColumnIndexOrThrow("uniabrev"));

                    try {
                        URL url = new URL("http://160.20.22.99:5300/cadastrar-unidade");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                        conn.setDoOutput(true);

                        String json = "{\"descricao\":\"" + descricao + "\", \"uniabrev\":\"" + (uniabrev == null ? "" : uniabrev) + "\"}";
                        OutputStream os = conn.getOutputStream();
                        os.write(json.getBytes("UTF-8"));
                        os.flush();
                        os.close();

                        int responseCode = conn.getResponseCode();
                        conn.disconnect();

                        if (responseCode == 200) {
                            dbHelper.marcarUnidadeComoSincronizada(id);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        });
    }

    private boolean verificarConexao() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
}
