import React, { useEffect, useState } from 'react';
import { apiService } from '../services/api';
import { LaporanResponse } from '../types';
import { FileText, Printer, Download, RefreshCw, Wallet, Compass, CreditCard, PieChart } from 'lucide-react';

export const LaporanPage: React.FC = () => {
  const [laporan, setLaporan] = useState<LaporanResponse | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchLaporan = async () => {
    setLoading(true);
    try {
      const res = await apiService.getLaporan();
      if (res && (res.data || res.status === 'success')) {
        setLaporan(res.data || (res as unknown as LaporanResponse));
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLaporan();
  }, []);

  const formatRupiah = (val?: number) => {
    return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(val || 0);
  };

  const handlePrint = () => {
    window.print();
  };

  if (loading) {
    return <div className="p-12 text-center text-slate-400 text-sm">Memuat rekapitulasi laporan...</div>;
  }

  const l = laporan || {};
  const kasUtama = l.kas_utama || { total_pemasukan: 0, total_pengeluaran: 0, saldo_kas: 0 };
  const kasKeliling = l.kas_keliling || { total_pemasukan: 0, total_pengeluaran: 0, saldo_keliling: 0 };
  const kasAniv = l.kas_anniversary || { total_pemasukan: 0, total_pengeluaran: 0, saldo_aniv: 0 };
  const cicilan = l.cicilan || { total_harga_barang: 0, total_sudah_dibayar: 0, total_sisa_cicilan: 0 };

  const totalSemuaPemasukan = (kasUtama.total_pemasukan || 0) + (kasKeliling.total_pemasukan || 0) + (kasAniv.total_pemasukan || 0);
  const totalSemuaPengeluaran = (kasUtama.total_pengeluaran || 0) + (kasKeliling.total_pengeluaran || 0) + (kasAniv.total_pengeluaran || 0);
  const totalSaldoBersih = totalSemuaPemasukan - totalSemuaPengeluaran;

  return (
    <div className="space-y-6">
      {/* Top Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-white">Laporan Rekapitulasi Keuangan</h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Laporan lengkap konsolidasi seluruh jenis kas & tagihan Nebo Sukabumi
          </p>
        </div>

        <div className="flex items-center space-x-2 self-start md:self-auto">
          <button
            onClick={fetchLaporan}
            className="flex items-center space-x-1.5 px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded-xl text-xs font-bold transition-all"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Refresh</span>
          </button>
          <button
            onClick={handlePrint}
            className="flex items-center space-x-1.5 px-3.5 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-xs font-bold transition-all shadow-lg shadow-blue-600/20"
          >
            <Printer className="w-3.5 h-3.5" />
            <span>Cetak / PDF</span>
          </button>
        </div>
      </div>

      {/* Overall Summary Card */}
      <div className="p-6 rounded-3xl bg-gradient-to-r from-slate-900 via-blue-950/40 to-slate-900 border border-blue-500/20 space-y-4">
        <div className="flex items-center space-x-2 text-blue-400 font-bold text-sm">
          <PieChart className="w-5 h-5" />
          <span>Konsolidasi Seluruh Akun Kas</span>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-2">
          <div className="p-4 rounded-2xl bg-slate-800/60 border border-slate-700/50">
            <span className="text-xs text-slate-400 font-bold uppercase">Akumulasi Pemasukan</span>
            <div className="text-xl font-extrabold text-emerald-400 mt-1">{formatRupiah(totalSemuaPemasukan)}</div>
          </div>
          <div className="p-4 rounded-2xl bg-slate-800/60 border border-slate-700/50">
            <span className="text-xs text-slate-400 font-bold uppercase">Akumulasi Pengeluaran</span>
            <div className="text-xl font-extrabold text-rose-400 mt-1">{formatRupiah(totalSemuaPengeluaran)}</div>
          </div>
          <div className="p-4 rounded-2xl bg-slate-800/60 border border-slate-700/50">
            <span className="text-xs text-slate-400 font-bold uppercase">Saldo Net Konsolidasi</span>
            <div className="text-xl font-extrabold text-blue-300 mt-1">{formatRupiah(totalSaldoBersih)}</div>
          </div>
        </div>
      </div>

      {/* Grid Reports */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Kas Utama */}
        <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 space-y-3">
          <div className="flex items-center space-x-2 text-blue-400 font-bold text-sm">
            <Wallet className="w-5 h-5" />
            <span>Rekap Kas Utama</span>
          </div>
          <div className="space-y-2 text-xs text-slate-300 pt-2 border-t border-slate-800">
            <div className="flex justify-between py-1">
              <span>Total Pemasukan:</span>
              <span className="font-bold text-emerald-400">{formatRupiah(kasUtama.total_pemasukan)}</span>
            </div>
            <div className="flex justify-between py-1">
              <span>Total Pengeluaran:</span>
              <span className="font-bold text-rose-400">{formatRupiah(kasUtama.total_pengeluaran)}</span>
            </div>
            <div className="flex justify-between py-1.5 border-t border-slate-800 font-bold text-white text-sm">
              <span>Saldo Kas Utama:</span>
              <span className="text-blue-300">{formatRupiah(kasUtama.saldo_kas)}</span>
            </div>
          </div>
        </div>

        {/* Kas Keliling */}
        <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 space-y-3">
          <div className="flex items-center space-x-2 text-emerald-400 font-bold text-sm">
            <Compass className="w-5 h-5" />
            <span>Rekap Kas Keliling</span>
          </div>
          <div className="space-y-2 text-xs text-slate-300 pt-2 border-t border-slate-800">
            <div className="flex justify-between py-1">
              <span>Total Pemasukan:</span>
              <span className="font-bold text-emerald-400">{formatRupiah(kasKeliling.total_pemasukan)}</span>
            </div>
            <div className="flex justify-between py-1">
              <span>Total Pengeluaran:</span>
              <span className="font-bold text-rose-400">{formatRupiah(kasKeliling.total_pengeluaran)}</span>
            </div>
            <div className="flex justify-between py-1.5 border-t border-slate-800 font-bold text-white text-sm">
              <span>Saldo Kas Keliling:</span>
              <span className="text-emerald-300">{formatRupiah(kasKeliling.saldo_keliling)}</span>
            </div>
          </div>
        </div>

        {/* Kas Anniversary */}
        <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 space-y-3">
          <div className="flex items-center space-x-2 text-purple-400 font-bold text-sm">
            <FileText className="w-5 h-5" />
            <span>Rekap Kas Anniversary</span>
          </div>
          <div className="space-y-2 text-xs text-slate-300 pt-2 border-t border-slate-800">
            <div className="flex justify-between py-1">
              <span>Total Pemasukan Aniv:</span>
              <span className="font-bold text-emerald-400">{formatRupiah(kasAniv.total_pemasukan)}</span>
            </div>
            <div className="flex justify-between py-1">
              <span>Total Pengeluaran Aniv:</span>
              <span className="font-bold text-rose-400">{formatRupiah(kasAniv.total_pengeluaran)}</span>
            </div>
            <div className="flex justify-between py-1.5 border-t border-slate-800 font-bold text-white text-sm">
              <span>Saldo Dana Anniversary:</span>
              <span className="text-purple-300">{formatRupiah(kasAniv.saldo_aniv)}</span>
            </div>
          </div>
        </div>

        {/* Portofolio Cicilan */}
        <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 space-y-3">
          <div className="flex items-center space-x-2 text-amber-400 font-bold text-sm">
            <CreditCard className="w-5 h-5" />
            <span>Portofolio Cicilan</span>
          </div>
          <div className="space-y-2 text-xs text-slate-300 pt-2 border-t border-slate-800">
            <div className="flex justify-between py-1">
              <span>Total Harga Barang:</span>
              <span className="font-bold">{formatRupiah(cicilan.total_harga_barang)}</span>
            </div>
            <div className="flex justify-between py-1">
              <span>Total Angsuran Masuk:</span>
              <span className="font-bold text-emerald-400">{formatRupiah(cicilan.total_sudah_dibayar)}</span>
            </div>
            <div className="flex justify-between py-1.5 border-t border-slate-800 font-bold text-white text-sm">
              <span>Sisa Piutang Cicilan:</span>
              <span className="text-amber-300">{formatRupiah(cicilan.total_sisa_cicilan)}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
