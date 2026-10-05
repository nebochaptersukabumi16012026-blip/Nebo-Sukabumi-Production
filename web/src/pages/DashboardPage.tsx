import React, { useEffect, useState } from 'react';
import { apiService } from '../services/api';
import { DashboardData } from '../types';
import { StatCard } from '../components/StatCard';
import { Users, Wallet, CreditCard, Compass, ArrowUpRight, ShieldCheck, RefreshCw, AlertTriangle } from 'lucide-react';

export const DashboardPage: React.FC = () => {
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchDashboard = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiService.getDashboard();
      if (res.status === 'success') {
        setData(res.data || res as DashboardData);
      } else {
        setError('Gagal memuat data dashboard');
      }
    } catch (err: any) {
      setError(err.message || 'Gagal terhubung ke API backend');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboard();
  }, []);

  const formatRupiah = (amount?: number) => {
    if (amount === undefined || amount === null) return 'Rp 0';
    return new Intl.NumberFormat('id-ID', {
      style: 'currency',
      currency: 'IDR',
      maximumFractionDigits: 0,
    }).format(amount);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="flex flex-col items-center space-y-3">
          <div className="w-10 h-10 border-4 border-blue-500/30 border-t-blue-500 rounded-full animate-spin" />
          <span className="text-sm font-semibold text-slate-400">Memuat data dashboard live...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6 rounded-2xl bg-rose-500/10 border border-rose-500/20 text-center space-y-4 max-w-lg mx-auto my-10">
        <AlertTriangle className="w-10 h-10 text-rose-400 mx-auto" />
        <h3 className="text-lg font-bold text-white">Gagal Memuat Dashboard</h3>
        <p className="text-sm text-slate-300">{error}</p>
        <button
          onClick={fetchDashboard}
          className="px-4 py-2 bg-rose-600 hover:bg-rose-500 text-white rounded-xl text-xs font-bold transition-all"
        >
          Coba Lagi
        </button>
      </div>
    );
  }

  const d = data || {};
  const totalAnggota = d.total_anggota ?? 0;
  const kasUtamaSaldo = d.kas_utama?.saldo_kas ?? d.total_kas ?? 0;
  const kasKelilingSaldo = d.kas_keliling_data?.saldo_keliling ?? d.kas_keliling ?? 0;
  const kasAnivSaldo = d.kas_anniversary_data?.saldo_aniv ?? d.kas_anniversary ?? 0;
  const sisaCicilan = d.cicilan?.total_sisa_cicilan ?? d.total_sisa_cicilan ?? 0;
  const totalPengeluaran = d.kas_utama?.total_pengeluaran ?? d.total_pengeluaran ?? 0;
  const totalSaldoKumulatif = (kasUtamaSaldo + kasKelilingSaldo + kasAnivSaldo);

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="p-6 rounded-3xl bg-gradient-to-r from-blue-900/40 via-indigo-900/30 to-slate-900 border border-blue-500/20 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <div className="inline-flex items-center space-x-1.5 px-3 py-1 rounded-full bg-blue-500/10 text-blue-400 border border-blue-500/20 text-xs font-semibold mb-2">
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Integrated cPanel & Android App</span>
          </div>
          <h2 className="text-2xl font-extrabold text-white tracking-tight">Ringkasan Keuangan Live</h2>
          <p className="text-sm text-slate-400 mt-1">Data keuangan aktual Nebo Sukabumi dari database produksi</p>
        </div>
        <button
          onClick={fetchDashboard}
          className="flex items-center space-x-2 px-4 py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded-xl text-xs font-bold transition-all self-start md:self-auto"
        >
          <RefreshCw className="w-4 h-4" />
          <span>Refresh Data</span>
        </button>
      </div>

      {/* Primary KPI Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Saldo Kas"
          value={formatRupiah(totalSaldoKumulatif)}
          subtitle="Gabungan Kas Utama, Keliling & Aniv"
          icon={Wallet}
          variant="blue"
        />
        <StatCard
          title="Kas Keliling"
          value={formatRupiah(kasKelilingSaldo)}
          subtitle="Saldo Operasional Keliling"
          icon={Compass}
          variant="emerald"
        />
        <StatCard
          title="Kas Anniversary"
          value={formatRupiah(kasAnivSaldo)}
          subtitle="Dana Terkumpul Anniversary"
          icon={CreditCard}
          variant="amber"
        />
        <StatCard
          title="Total Anggota"
          value={`${totalAnggota} Anggota`}
          subtitle="Terdaftar di database"
          icon={Users}
          variant="purple"
        />
      </div>

      {/* Secondary KPI Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatCard
          title="Sisa Tagihan Cicilan"
          value={formatRupiah(sisaCicilan)}
          subtitle={`${d.cicilan?.anggota_mencicil ?? d.anggota_mencicil ?? 0} Anggota aktif mencicil`}
          icon={CreditCard}
          variant="amber"
        />
        <StatCard
          title="Total Pengeluaran Kas"
          value={formatRupiah(totalPengeluaran)}
          subtitle="Total akumulasi pengeluaran"
          icon={ArrowUpRight}
          variant="rose"
        />
        <StatCard
          title="Belum Bayar Kas"
          value={`${d.belum_bayar_kas ?? d.belum_kas ?? 0} Anggota`}
          subtitle="Kewajiban kas bulanan"
          icon={Users}
          variant="slate"
        />
      </div>

      {/* Detailed Breakdown Card */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-4">
          <h3 className="text-base font-bold text-white flex items-center space-x-2">
            <Wallet className="w-5 h-5 text-blue-400" />
            <span>Rincian Arus Kas Utama</span>
          </h3>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="p-4 rounded-xl bg-slate-800/50 border border-slate-800">
              <span className="text-xs text-slate-400 font-semibold uppercase">Total Pemasukan</span>
              <div className="text-lg font-bold text-emerald-400 mt-1">
                {formatRupiah(d.kas_utama?.total_pemasukan ?? d.pemasukan_kas ?? 0)}
              </div>
            </div>
            <div className="p-4 rounded-xl bg-slate-800/50 border border-slate-800">
              <span className="text-xs text-slate-400 font-semibold uppercase">Total Pengeluaran</span>
              <div className="text-lg font-bold text-rose-400 mt-1">
                {formatRupiah(d.kas_utama?.total_pengeluaran ?? d.pengeluaran_kas ?? 0)}
              </div>
            </div>
            <div className="p-4 rounded-xl bg-slate-800/50 border border-slate-800">
              <span className="text-xs text-slate-400 font-semibold uppercase">Saldo Kas Utama</span>
              <div className="text-lg font-bold text-blue-400 mt-1">
                {formatRupiah(kasUtamaSaldo)}
              </div>
            </div>
          </div>
        </div>

        <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-4">
          <h3 className="text-base font-bold text-white flex items-center space-x-2">
            <Compass className="w-5 h-5 text-emerald-400" />
            <span>Kas Keliling</span>
          </h3>
          <div className="space-y-3">
            <div className="flex justify-between items-center text-xs text-slate-400 pb-2 border-b border-slate-800">
              <span>Pemasukan:</span>
              <span className="font-bold text-emerald-400">
                {formatRupiah(d.kas_keliling_data?.total_pemasukan ?? d.pemasukan_kas_keliling ?? 0)}
              </span>
            </div>
            <div className="flex justify-between items-center text-xs text-slate-400 pb-2 border-b border-slate-800">
              <span>Pengeluaran:</span>
              <span className="font-bold text-rose-400">
                {formatRupiah(d.kas_keliling_data?.total_pengeluaran ?? d.pengeluaran_kas_keliling ?? 0)}
              </span>
            </div>
            <div className="flex justify-between items-center text-sm font-bold text-white pt-1">
              <span>Saldo Net:</span>
              <span className="text-emerald-300">{formatRupiah(kasKelilingSaldo)}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
