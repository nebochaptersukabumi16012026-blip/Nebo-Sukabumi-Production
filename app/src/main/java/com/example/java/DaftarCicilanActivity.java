package com.example.java;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.R;
import com.example.ui.SessionManager;
import com.google.gson.Gson;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DaftarCicilanActivity extends AppCompatActivity {

    private TextView tvSubtitleAnggota;
    private TextView tvTotalSisaCicilan;
    private TextView tvTotalHargaBarang;
    private TextView tvTotalDibayar;
    private EditText etSearch;
    private RecyclerView rvCicilan;
    private LinearLayout layoutEmptyState;
    private ProgressBar progressBar;
    private ImageButton btnBack;
    private ImageButton btnRefresh;

    private CicilanAdapter adapter;
    private final List<CicilanAnggota> masterList = new ArrayList<>();
    private final List<CicilanAnggota> filteredList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daftar_cicilan);

        initViews();
        setupRecyclerView();
        setupListeners();
        loadCicilanData();
    }

    private void initViews() {
        tvSubtitleAnggota = findViewById(R.id.tvSubtitleAnggota);
        tvTotalSisaCicilan = findViewById(R.id.tvTotalSisaCicilan);
        tvTotalHargaBarang = findViewById(R.id.tvTotalHargaBarang);
        tvTotalDibayar = findViewById(R.id.tvTotalDibayar);
        etSearch = findViewById(R.id.etSearch);
        rvCicilan = findViewById(R.id.rvCicilan);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);
        btnRefresh = findViewById(R.id.btnRefresh);
    }

    private void setupRecyclerView() {
        adapter = new CicilanAdapter(filteredList);
        adapter.setOnItemClickListener(item -> {
            Toast.makeText(this, "Anggota: " + item.getNama() + " (" + item.getNra() + ")", Toast.LENGTH_SHORT).show();
        });
        rvCicilan.setLayoutManager(new LinearLayoutManager(this));
        rvCicilan.setAdapter(adapter);
    }

    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> loadCicilanData());
        }

        // TextWatcher untuk penyaringan Nama atau NRA anggota secara real-time
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterData(s != null ? s.toString() : "");
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
    }

    /**
     * Memanggil API cPanel get_daftar_cicilan_aktif.php menggunakan Library Volley & Gson
     * dengan query parameter role dan nra dari user login
     */
    public void loadData() {
        // 1. Ambil data user yang sedang login dari SessionManager
        String userRole = SessionManager.INSTANCE.getRole(this);
        if (userRole == null || userRole.trim().isEmpty()) {
            userRole = "MEMBER";
        }
        userRole = userRole.toUpperCase(Locale.ROOT);
        String userNra = SessionManager.INSTANCE.getUserNra(this);
        if (userNra == null || userNra.trim().isEmpty()) {
            userNra = SessionManager.INSTANCE.getNra(this);
        }
        if (userNra == null || userNra.trim().isEmpty()) {
            userNra = "0001";
        }

        // 2. Format URL API dengan query parameter
        String url;
        try {
            url = "https://nebosukabumi.net/api/get_daftar_cicilan_aktif.php?role="
                    + URLEncoder.encode(userRole, "UTF-8")
                    + "&nra=" + URLEncoder.encode(userNra, "UTF-8");
        } catch (Exception e) {
            url = "https://nebosukabumi.net/api/get_daftar_cicilan_aktif.php?role=" + userRole + "&nra=" + userNra;
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                url,
                responseString -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    try {
                        Gson gson = new Gson();
                        CicilanResponse response = gson.fromJson(responseString, CicilanResponse.class);
                        updateUI(response);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(DaftarCicilanActivity.this, "Gagal memproses data JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        showEmptyState(true);
                    }
                },
                error -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    error.printStackTrace();
                    Toast.makeText(DaftarCicilanActivity.this, "Gagal terhubung ke server CPanel", Toast.LENGTH_SHORT).show();
                    showEmptyState(true);
                }
        );

        queue.add(stringRequest);
    }

    private void loadCicilanData() {
        loadData();
    }

    /**
     * Update data rekapitulasi dan daftar anggota pada UI
     */
    private void updateUI(CicilanResponse response) {
        if (response == null) {
            showEmptyState(true);
            return;
        }

        // 1. Ambil data dengan Null Safety Check
        double totalSisa = response.getTotal_sisa_cicilan();
        double totalHarga = response.getTotal_harga_barang();
        double totalDibayar = response.getTotal_dibayar();

        // 2. Set ke TextView UI
        if (tvTotalSisaCicilan != null) {
            tvTotalSisaCicilan.setText(formatRupiah(totalSisa));
        }
        if (tvTotalHargaBarang != null) {
            tvTotalHargaBarang.setText("Barang: " + formatRupiah(totalHarga));
        }
        if (tvTotalDibayar != null) {
            tvTotalDibayar.setText("Dibayar: " + formatRupiah(totalDibayar));
        }

        masterList.clear();
        List<CicilanAnggota> dataList = response.getData();

        if (dataList != null && !dataList.isEmpty()) {
            masterList.addAll(dataList);
            int count = response.getTotal_anggota_mencicil() > 0 ? response.getTotal_anggota_mencicil() : dataList.size();
            if (tvSubtitleAnggota != null) {
                tvSubtitleAnggota.setText("Anggota Memiliki Cicilan (" + count + ")");
            }

            showEmptyState(false);
            filterData(etSearch != null ? etSearch.getText().toString() : "");
        } else {
            int count = response.getTotal_anggota_mencicil();
            if (tvSubtitleAnggota != null) {
                tvSubtitleAnggota.setText("Anggota Memiliki Cicilan (" + count + ")");
            }
            showEmptyState(true);
        }
    }

    /**
     * Filter daftar cicilan secara real-time berdasarkan query Nama / NRA
     */
    private void filterData(String query) {
        String trimmed = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        filteredList.clear();

        if (trimmed.isEmpty()) {
            filteredList.addAll(masterList);
        } else {
            for (CicilanAnggota item : masterList) {
                String nama = item.getNama().toLowerCase(Locale.ROOT);
                String nra = item.getNra().toLowerCase(Locale.ROOT);
                if (nama.contains(trimmed) || nra.contains(trimmed)) {
                    filteredList.add(item);
                }
            }
        }

        if (adapter != null) {
            adapter.updateData(filteredList);
        }
        showEmptyState(filteredList.isEmpty());
    }

    /**
     * Kontrol visibilitas RecyclerView dan Empty State Container
     */
    private void showEmptyState(boolean isEmpty) {
        if (rvCicilan != null) {
            rvCicilan.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
        if (layoutEmptyState != null) {
            layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Helper pemformatan nilai mata uang ke Rupiah Indonesia (Locale in, ID)
     */
    public static String formatRupiah(double number) {
        try {
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
            formatter.setMaximumFractionDigits(0);
            return formatter.format(number).replace("Rp", "Rp ").replace(",00", "");
        } catch (Exception e) {
            return "Rp " + String.format(new Locale("in", "ID"), "%,.0f", number);
        }
    }
}
