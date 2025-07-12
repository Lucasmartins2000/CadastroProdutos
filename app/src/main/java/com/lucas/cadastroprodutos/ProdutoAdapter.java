package com.lucas.cadastroprodutos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ProdutoAdapter extends RecyclerView.Adapter<ProdutoAdapter.ProdutoViewHolder> {
    private ArrayList<String> listaProdutos;
    private Context context;

    public ProdutoAdapter(ArrayList<String> lista) {
        this.listaProdutos = lista;
    }

    @Override
    public ProdutoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        View item = LayoutInflater.from(context).inflate(R.layout.item_produto, parent, false);
        return new ProdutoViewHolder(item);
    }

    @Override
    public void onBindViewHolder(ProdutoViewHolder holder, int position) {
        try {
            String[] dados = listaProdutos.get(position).split(";");
            String tipo = dados[0];

            if (tipo.equals("REMOTO")) {
                // Produto remoto
                String nome = dados[1];
                String descricao = dados[2];
                String estoqueMaximo = dados[3].replace("Máx: ", "");
                String pontoPedido = dados[4].replace("Ponto: ", "");
                String estoqueMinimo = dados[5].replace("Mín: ", "");

                holder.nome.setText("Nome: " + nome + " (Remoto)");
                holder.descricao.setText("Descrição: " + descricao);
                holder.estoque.setText("Máximo: " + estoqueMaximo + " | Ponto: " + pontoPedido + " | Mínimo: " + estoqueMinimo);
                holder.itemView.setVisibility(View.VISIBLE);
                holder.itemView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            } else {
                // Produto local
                int id = Integer.parseInt(dados[0]);
                String nome = dados[1];
                String descricao = dados[2];
                String estoqueMaximo = dados[3].replace("Máx: ", "");
                String pontoPedido = dados[4].replace("Ponto: ", "");
                String estoqueMinimo = dados[5].replace("Mín: ", "");

                holder.nome.setText("ID: " + id + " | Nome: " + nome);
                holder.descricao.setText("Descrição: " + descricao);
                holder.estoque.setText("Máximo: " + estoqueMaximo + " | Ponto: " + pontoPedido + " | Mínimo: " + estoqueMinimo);
                holder.itemView.setVisibility(View.VISIBLE);
                holder.itemView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            }

            // Sempre esconde o botão editar!
            holder.btnEditar.setVisibility(View.GONE);

        } catch (Exception e) {
            // Oculta o item se der erro (não aparece na lista)
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.getLayoutParams().height = 0;
        }
    }

    @Override
    public int getItemCount() {
        return listaProdutos.size();
    }

    public static class ProdutoViewHolder extends RecyclerView.ViewHolder {
        TextView nome, descricao, estoque;
        Button btnEditar;

        public ProdutoViewHolder(View itemView) {
            super(itemView);
            nome = itemView.findViewById(R.id.txtNome);
            descricao = itemView.findViewById(R.id.txtDescricao);
            estoque = itemView.findViewById(R.id.txtEstoque);
            btnEditar = itemView.findViewById(R.id.btnEditar);
        }
    }
}