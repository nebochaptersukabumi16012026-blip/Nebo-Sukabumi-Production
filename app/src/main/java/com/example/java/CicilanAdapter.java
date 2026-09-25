package com.example.java;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.R;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CicilanAdapter extends RecyclerView.Adapter<CicilanAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(CicilanAnggota item);
    }

    private final List<CicilanAnggota> listAnggota;
    private OnItemClickListener listener;

    public CicilanAdapter(List<CicilanAnggota> listAnggota) {
        this.listAnggota = listAnggota != null ? listAnggota : new ArrayList<>();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cicilan_anggota, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CicilanAnggota item = listAnggota.get(position);

        holder.tvNamaAnggota.setText(item.getNama().isEmpty() ? "Anggota Nebo" : item.getNama());
        holder.tvNraAnggota.setText("NRA: " + item.getNra());
        holder.tvSisaCicilan.setText(formatRupiah(item.getSisa_cicilan()));
        holder.tvHargaBarang.setText(formatRupiah(item.getHarga_barang()));

        double dibayar = item.getTotal_dibayar() > 0 ? item.getTotal_dibayar() : item.getSudah_dibayar();
        holder.tvSudahDibayar.setText(formatRupiah(dibayar));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listAnggota.size();
    }

    public void updateData(List<CicilanAnggota> newList) {
        this.listAnggota.clear();
        if (newList != null) {
            this.listAnggota.addAll(newList);
        }
        notifyDataSetChanged();
    }

    private String formatRupiah(double amount) {
        try {
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
            formatter.setMaximumFractionDigits(0);
            return formatter.format(amount).replace("Rp", "Rp ").replace(",00", "");
        } catch (Exception e) {
            return "Rp " + String.format(new Locale("in", "ID"), "%,.0f", amount);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvNamaAnggota;
        public final TextView tvNraAnggota;
        public final TextView tvSisaCicilan;
        public final TextView tvHargaBarang;
        public final TextView tvSudahDibayar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNamaAnggota = itemView.findViewById(R.id.tvNamaAnggota);
            tvNraAnggota = itemView.findViewById(R.id.tvNraAnggota);
            tvSisaCicilan = itemView.findViewById(R.id.tvSisaCicilan);
            tvHargaBarang = itemView.findViewById(R.id.tvHargaBarang);
            tvSudahDibayar = itemView.findViewById(R.id.tvSudahDibayar);
        }
    }
}
