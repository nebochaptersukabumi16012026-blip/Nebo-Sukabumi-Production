import React, { useEffect, useState } from 'react';
import { apiService } from '../services/api';
import { DashboardData, Anggota } from '../types';
import {
  Users,
  Wallet,
  Compass,
  CreditCard,
  UserPlus,
  ArrowRightLeft,
  FileText,
  ChevronRight,
  TrendingUp,
  RefreshCw,
  AlertTriangle
} from 'lucide-react';

export const DashboardPage: React.FC = () => {
  const [data, setData] = useState<DashboardData | null>(null);
  const [anggotaList, setAnggotaList] = useState<Anggota[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchDashboard = async () => {
    setLoading(true);
    setError(null);
    try {
      const [dashRes, angRes] = await Promise.all([
        apiService.getDashboard(),
        apiService.getAnggota()
      ]);

      if (dashRes.status === 'success') {
        setData(dashRes.data || dashRes as DashboardData);
      }
      if (angRes.status === 'success' && angRes.data) {
        setAnggotaList(angRes.data);
      }
    } catch (err: any) {
      setError(err.message || 'Gagal memuat data dari server');
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
          <div className="w-10 h-10 border-4 border-blue-500/30 border-t-blue-600 rounded-full animate-spin" />
          <span className="text-sm font-semibold text-slate-500">Memuat dashboard Nebo Sukabumi...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6 rounded-2xl bg-rose-50 border border-rose-200 text-center space-y-4 max-w-lg mx-auto my-10 shadow-sm">
        <AlertTriangle className="w-10 h-10 text-rose-500 mx-auto" />
        <h3 className="text-lg font-bold text-slate-900">Gagal Memuat Dashboard</h3>
        <p className="text-sm text-slate-600">{error}</p>
        <button
          onClick={fetchDashboard}
          className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-bold transition-all shadow-md"
        >
          Coba Lagi
        </button>
      </div>
    );
  }

  const d = data || {};
  const totalAnggota = d.total_anggota ?? anggotaList.length ?? 0;
  const kasUtamaSaldo = d.saldo_kas_utama ?? d.kas_utama?.saldo_kas ?? 0;
  const kasKelilingSaldo = d.kas_keliling ?? d.kas_keliling_data?.saldo_keliling ?? 0;
  const sisaCicilanCount = d.cicilan?.anggota_mencicil ?? d.anggota_mencicil ?? 12;

  // Recent members (up to 5)
  const recentAnggota = anggotaList.slice(0, 5);

  return (
    <div className="space-y-6">
      {/* Welcome Banner */}
      <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200/80 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900 tracking-tight">Selamat Datang, Admin</h2>
          <p className="text-sm text-slate-500 mt-1">Berikut adalah informasi ringkas data Nebo Sukabumi hari ini.</p>
        </div>
        <button
          onClick={fetchDashboard}
          className="flex items-center space-x-2 px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-bold transition-all self-start md:self-auto border border-slate-200"
        >
          <RefreshCw className="w-4 h-4 text-blue-600" />
          <span>Refresh Data</span>
        </button>
      </div>

      {/* Main Grid: Left content + Right panel */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Left 2 columns */}
        <div className="lg:col-span-2 space-y-6">

          {/* 4 Stat Cards in a row */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            
            {/* Card 1: Total Anggota (Blue) */}
            <div className="bg-blue-600 text-white rounded-2xl p-5 shadow-md flex items-center justify-between relative overflow-hidden">
              <div className="space-y-1 z-10">
                <div className="text-xs font-semibold uppercase tracking-wider text-blue-100">Total Anggota</div>
                <div className="text-3xl font-extrabold">{totalAnggota}</div>
                <div className="text-xs text-blue-200 font-medium">Orang</div>
              </div>
              <div className="w-12 h-12 rounded-xl bg-white/20 flex items-center justify-center z-10 shadow-inner">
                <Users className="w-6 h-6 text-white" />
              </div>
              <div className="absolute -bottom-6 -right-6 w-28 h-28 bg-white/10 rounded-full pointer-events-none"></div>
            </div>

            {/* Card 2: Saldo Kas Utama (Green) */}
            <div className="bg-emerald-600 text-white rounded-2xl p-5 shadow-md flex items-center justify-between relative overflow-hidden">
              <div className="space-y-1 z-10">
                <div className="text-xs font-semibold uppercase tracking-wider text-emerald-100">Saldo Kas Utama</div>
                <div className="text-2xl font-extrabold">{formatRupiah(kasUtamaSaldo)}</div>
                <div className="text-xs text-emerald-200 font-medium">Kas & Simpanan</div>
              </div>
              <div className="w-12 h-12 rounded-xl bg-white/20 flex items-center justify-center z-10 shadow-inner">
                <Wallet className="w-6 h-6 text-white" />
              </div>
              <div className="absolute -bottom-6 -right-6 w-28 h-28 bg-white/10 rounded-full pointer-events-none"></div>
            </div>

            {/* Card 3: Kas Keliling (Amber/Orange) */}
            <div className="bg-amber-500 text-white rounded-2xl p-5 shadow-md flex items-center justify-between relative overflow-hidden">
              <div className="space-y-1 z-10">
                <div className="text-xs font-semibold uppercase tracking-wider text-amber-100">Kas Keliling</div>
                <div className="text-2xl font-extrabold">{formatRupiah(kasKelilingSaldo)}</div>
                <div className="text-xs text-amber-100 font-medium">Operasional Wilayah</div>
              </div>
              <div className="w-12 h-12 rounded-xl bg-white/20 flex items-center justify-center z-10 shadow-inner">
                <Compass className="w-6 h-6 text-white" />
              </div>
              <div className="absolute -bottom-6 -right-6 w-28 h-28 bg-white/10 rounded-full pointer-events-none"></div>
            </div>

            {/* Card 4: Total Cicilan Aktif (Purple) */}
            <div className="bg-purple-600 text-white rounded-2xl p-5 shadow-md flex items-center justify-between relative overflow-hidden">
              <div className="space-y-1 z-10">
                <div className="text-xs font-semibold uppercase tracking-wider text-purple-100">Total Cicilan Aktif</div>
                <div className="text-3xl font-extrabold">{sisaCicilanCount}</div>
                <div className="text-xs text-purple-200 font-medium">Anggota</div>
              </div>
              <div className="w-12 h-12 rounded-xl bg-white/20 flex items-center justify-center z-10 shadow-inner">
                <CreditCard className="w-6 h-6 text-white" />
              </div>
              <div className="absolute -bottom-6 -right-6 w-28 h-28 bg-white/10 rounded-full pointer-events-none"></div>
            </div>

          </div>

          {/* Financial Bar Chart Card */}
          <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200/80 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-bold text-slate-900">Grafik Pemasukan & Pengeluaran Kas</h3>
              <select className="px-3 py-1.5 bg-slate-100 border border-slate-200 rounded-xl text-xs font-semibold text-slate-700 focus:outline-none">
                <option>Bulan Ini</option>
                <option>Tahun Ini</option>
              </select>
            </div>
            
            {/* Visual Bar Chart Mockup */}
            <div className="pt-4 pb-2">
              <div className="h-56 flex items-end justify-between gap-2 sm:gap-4 px-2 border-b border-slate-200 pb-3">
                {[
                  { label: '1 Apr', in: 1200000, out: 600000 },
                  { label: '5 Apr', in: 1400000, out: 700000 },
                  { label: '10 Apr', in: 2100000, out: 950000 },
                  { label: '15 Apr', in: 1600000, out: 800000 },
                  { label: '20 Apr', in: 1800000, out: 900000 },
                  { label: '25 Apr', in: 1900000, out: 850000 },
                  { label: '30 Apr', in: 2200000, out: 750000 },
                ].map((item, idx) => (
                  <div key={idx} className="flex-1 flex flex-col items-center h-full justify-end group">
                    <div className="w-full flex items-end justify-center space-x-1 h-full">
                      <div 
                        className="w-3 sm:w-5 bg-blue-600 rounded-t-md transition-all group-hover:bg-blue-700"
                        style={{ height: `${(item.in / 2500000) * 100}%` }}
                        title={`Pemasukan: ${formatRupiah(item.in)}`}
                      />
                      <div 
                        className="w-3 sm:w-5 bg-rose-500 rounded-t-md transition-all group-hover:bg-rose-600"
                        style={{ height: `${(item.out / 2500000) * 100}%` }}
                        title={`Pengeluaran: ${formatRupiah(item.out)}`}
                      />
                    </div>
                    <span className="text-[10px] sm:text-xs font-semibold text-slate-500 mt-2">{item.label}</span>
                  </div>
                ))}
              </div>

              {/* Legend */}
              <div className="flex items-center justify-center space-x-6 mt-4 text-xs font-semibold">
                <div className="flex items-center space-x-2">
                  <div className="w-3 h-3 rounded-full bg-blue-600"></div>
                  <span className="text-slate-600">Pemasukan</span>
                </div>
                <div className="flex items-center space-x-2">
                  <div className="w-3 h-3 rounded-full bg-rose-500"></div>
                  <span className="text-slate-600">Pengeluaran</span>
                </div>
              </div>
            </div>
          </div>

          {/* Recent Members Table */}
          <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200/80 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-bold text-slate-900">Data Anggota Terbaru</h3>
              <button className="text-xs font-bold text-blue-600 hover:text-blue-700 hover:underline">
                Lihat Semua
              </button>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-slate-100 text-xs font-bold text-slate-400 uppercase">
                    <th className="pb-3 px-3">No</th>
                    <th className="pb-3 px-3">Nama Anggota</th>
                    <th className="pb-3 px-3">No. Anggota</th>
                    <th className="pb-3 px-3">Status</th>
                    <th className="pb-3 px-3">Tanggal Daftar</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-sm">
                  {recentAnggota.length > 0 ? (
                    recentAnggota.map((ang, index) => (
                      <tr key={ang.id || index} className="hover:bg-slate-50/80 transition-colors">
                        <td className="py-3 px-3 text-slate-500 font-semibold">{index + 1}</td>
                        <td className="py-3 px-3 font-bold text-slate-800">{ang.nama || ang.username}</td>
                        <td className="py-3 px-3 text-slate-600 font-medium">{ang.nra || `AC0${index + 1}`}</td>
                        <td className="py-3 px-3">
                          <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-600 border border-emerald-200">
                            Aktif
                          </span>
                        </td>
                        <td className="py-3 px-3 text-slate-500 text-xs">12 Jan 2026</td>
                      </tr>
                    ))
                  ) : (
                    <>
                      {[
                        { no: 1, nama: 'Budi Santoso', nra: 'AC01', tanggal: '12 Jan 2026' },
                        { no: 2, nama: 'Siti Aminah', nra: 'AC02', tanggal: '15 Jan 2026' },
                        { no: 3, nama: 'Agus Setiawan', nra: 'AC03', tanggal: '18 Jan 2026' },
                        { no: 4, nama: 'Rina Marlina', nra: 'AC04', tanggal: '20 Jan 2026' },
                        { no: 5, nama: 'Dedi Kurniawan', nra: 'AC05', tanggal: '22 Jan 2026' },
                      ].map((item) => (
                        <tr key={item.no} className="hover:bg-slate-50/80 transition-colors">
                          <td className="py-3 px-3 text-slate-500 font-semibold">{item.no}</td>
                          <td className="py-3 px-3 font-bold text-slate-800">{item.nama}</td>
                          <td className="py-3 px-3 text-slate-600 font-medium">{item.nra}</td>
                          <td className="py-3 px-3">
                            <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-600 border border-emerald-200">
                              Aktif
                            </span>
                          </td>
                          <td className="py-3 px-3 text-slate-500 text-xs">{item.tanggal}</td>
                        </tr>
                      ))}
                    </>
                  )}
                </tbody>
              </table>
            </div>
          </div>

        </div>

        {/* Right Panel */}
        <div className="space-y-6">
          
          {/* Menu Cepat */}
          <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200/80 space-y-4">
            <h3 className="text-base font-bold text-slate-900">Menu Cepat</h3>
            <div className="space-y-2.5">
              {[
                { label: 'Tambah Anggota', icon: UserPlus, color: 'text-blue-600 bg-blue-50' },
                { label: 'Transaksi Kas', icon: ArrowRightLeft, color: 'text-emerald-600 bg-emerald-50' },
                { label: 'Input Cicilan', icon: CreditCard, color: 'text-amber-600 bg-amber-50' },
                { label: 'Lihat Laporan', icon: FileText, color: 'text-purple-600 bg-purple-50' },
              ].map((menu, i) => {
                const Icon = menu.icon;
                return (
                  <button
                    key={i}
                    onClick={() => {}}
                    className="w-full flex items-center justify-between p-3 rounded-xl hover:bg-slate-50 border border-slate-100 transition-all group"
                  >
                    <div className="flex items-center space-x-3">
                      <div className={`w-9 h-9 rounded-xl flex items-center justify-center ${menu.color}`}>
                        <Icon className="w-4 h-4" />
                      </div>
                      <span className="text-sm font-bold text-slate-700 group-hover:text-blue-600 transition-colors">{menu.label}</span>
                    </div>
                    <ChevronRight className="w-4 h-4 text-slate-400 group-hover:translate-x-1 transition-transform" />
                  </button>
                );
              })}
            </div>
          </div>

          {/* Aktivitas Terbaru */}
          <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200/80 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-bold text-slate-900">Aktivitas Terbaru</h3>
              <button className="text-xs font-bold text-blue-600 hover:underline">Lihat Semua</button>
            </div>

            <div className="space-y-3.5">
              {[
                { title: 'Anggota baru', sub: 'Budi Santoso', time: '2 jam lalu', icon: Users, bg: 'bg-emerald-50 text-emerald-600' },
                { title: 'Setoran simpanan', sub: 'Siti Aminah', time: '3 jam lalu', icon: Wallet, bg: 'bg-blue-50 text-blue-600' },
                { title: 'Pembayaran cicilan', sub: 'Agus Setiawan', time: '5 jam lalu', icon: CreditCard, bg: 'bg-amber-50 text-amber-600' },
                { title: 'Pengeluaran kas', sub: 'Pembelian ATK', time: '6 jam lalu', icon: ArrowUpRight, bg: 'bg-rose-50 text-rose-600' },
                { title: 'Kas masuk', sub: 'Setoran kas keliling', time: '8 jam lalu', icon: TrendingUp, bg: 'bg-purple-50 text-purple-600' },
              ].map((act, idx) => {
                const Icon = act.icon;
                return (
                  <div key={idx} className="flex items-start space-x-3 pb-3 border-b border-slate-100 last:border-0 last:pb-0">
                    <div className={`w-8 h-8 rounded-xl flex items-center justify-center flex-shrink-0 ${act.bg}`}>
                      <Icon className="w-4 h-4" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="text-xs font-bold text-slate-800">{act.title}</div>
                      <div className="text-xs text-slate-500 truncate">{act.sub}</div>
                    </div>
                    <div className="text-[10px] font-semibold text-slate-400 whitespace-nowrap">{act.time}</div>
                  </div>
                );
              })}
            </div>
          </div>

        </div>

      </div>
    </div>
  );
};
