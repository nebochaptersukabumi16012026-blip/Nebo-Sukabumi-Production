import React, { useEffect, useState } from 'react';
import { apiService } from '../services/api';
import { RiwayatKasItem, Anggota } from '../types';
import { useAuth } from '../context/AuthContext';
import { Modal } from '../components/Modal';
import { Wallet, Plus, Trash2, Search, AlertCircle, Loader2 } from 'lucide-react';

export const KasPage: React.FC = () => {
  const { isAdminOrTreasurer, user } = useAuth();
  const [kasHistory, setKasHistory] = useState<RiwayatKasItem[]>([]);
  const [anggotaList, setAnggotaList] = useState<Anggota[]>([]);
  const [summary, setSummary] = useState({ total_pemasukan: 0, total_pengeluaran: 0, saldo: 0 });
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [formData, setFormData] = useState({
    id_anggota: 0,
    nominal: '',
    keterangan: 'Iuran Kas Anggota',
  });

  const fetchData = async () => {
    setLoading(true);
    try {
      const [resDetail, resAnggota] = await Promise.all([
        apiService.getDetailKas(),
        apiService.getAnggotaList(),
      ]);

      if (resDetail.status === 'success' && resDetail.data) {
        setSummary({
          total_pemasukan: resDetail.data.total_pemasukan || 0,
          total_pengeluaran: resDetail.data.total_pengeluaran || 0,
          saldo: resDetail.data.saldo || 0,
        });
        setKasHistory(resDetail.data.riwayat || []);
      }

      if (resAnggota.status === 'success' && resAnggota.data) {
        setAnggotaList(resAnggota.data);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const formatRupiah = (val?: number) => {
    return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(val || 0);
  };

  const handleAddSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.id_anggota || !formData.nominal || parseFloat(formData.nominal) <= 0) {
      setErrorMsg('Pilih Anggota dan masukkan nominal valid');
      return;
    }

    setIsSubmitting(true);
    setErrorMsg(null);

    try {
      const res = await apiService.addKas(
        formData.id_anggota,
        parseFloat(formData.nominal),
        formData.keterangan,
        user?.role || 'ADMIN'
      );

      if (res.status === 'success') {
        setIsModalOpen(false);
        setFormData({ id_anggota: 0, nominal: '', keterangan: 'Iuran Kas Anggota' });
        fetchData();
      } else {
        setErrorMsg(res.message || 'Gagal menambah iuran kas');
      }
    } catch (err: any) {
      setErrorMsg(err.message || 'Gagal memproses kas');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Hapus transaksi kas ini?')) return;
    try {
      const res = await apiService.deleteKas(id, user?.role || 'ADMIN');
      if (res.status === 'success') {
        fetchData();
      } else {
        alert(res.message || 'Gagal menghapus kas');
      }
    } catch (err: any) {
      alert(err.message || 'Gagal memproses hapus');
    }
  };

  const filteredHistory = kasHistory.filter((item) =>
    (item.nama || '').toLowerCase().includes(search.toLowerCase()) ||
    (item.keterangan || '').toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-6">
      {/* Top Bar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-white">Uang Kas Anggota</h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Pengelolaan iuran kas bulanan & riwayat transaksi kas Nebo Sukabumi
          </p>
        </div>

        {isAdminOrTreasurer && (
          <button
            onClick={() => {
              setErrorMsg(null);
              setIsModalOpen(true);
            }}
            className="flex items-center space-x-2 px-4 py-2.5 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-xs font-bold transition-all shadow-lg shadow-blue-600/20 self-start md:self-auto"
          >
            <Plus className="w-4 h-4" />
            <span>Bayar Uang Kas</span>
          </button>
        )}
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800">
          <span className="text-xs text-slate-400 font-semibold uppercase">Total Pemasukan Kas</span>
          <div className="text-xl font-extrabold text-emerald-400 mt-1">{formatRupiah(summary.total_pemasukan)}</div>
        </div>
        <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800">
          <span className="text-xs text-slate-400 font-semibold uppercase">Total Pengeluaran Kas</span>
          <div className="text-xl font-extrabold text-rose-400 mt-1">{formatRupiah(summary.total_pengeluaran)}</div>
        </div>
        <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800">
          <span className="text-xs text-slate-400 font-semibold uppercase">Saldo Bersih Kas</span>
          <div className="text-xl font-extrabold text-blue-400 mt-1">{formatRupiah(summary.saldo)}</div>
        </div>
      </div>

      {/* Search Input */}
      <div className="relative max-w-md">
        <Search className="w-4 h-4 absolute left-3.5 top-3.5 text-slate-400" />
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Cari transaksi kas atau nama anggota..."
          className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
        />
      </div>

      {/* Transactions Table */}
      {loading ? (
        <div className="p-12 text-center text-slate-400 text-sm">Memuat data kas...</div>
      ) : filteredHistory.length === 0 ? (
        <div className="p-12 text-center text-slate-500 text-sm bg-slate-900 border border-slate-800 rounded-2xl">
          Belum ada riwayat transaksi kas yang sesuai.
        </div>
      ) : (
        <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-950 text-slate-400 font-bold uppercase border-b border-slate-800">
                <tr>
                  <th className="px-4 py-3.5">Tanggal</th>
                  <th className="px-4 py-3.5">Anggota</th>
                  <th className="px-4 py-3.5">Nominal</th>
                  <th className="px-4 py-3.5">Keterangan</th>
                  {isAdminOrTreasurer && <th className="px-4 py-3.5 text-right">Aksi</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {filteredHistory.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-800/40 transition-colors">
                    <td className="px-4 py-3 text-slate-400">{item.tanggal || 'Hari Ini'}</td>
                    <td className="px-4 py-3 font-semibold text-white">{item.nama || 'Anggota'}</td>
                    <td className="px-4 py-3 font-bold text-emerald-400">{formatRupiah(item.nominal)}</td>
                    <td className="px-4 py-3 text-slate-400">{item.keterangan || 'Iuran Kas'}</td>
                    {isAdminOrTreasurer && (
                      <td className="px-4 py-3 text-right">
                        <button
                          onClick={() => handleDelete(item.id)}
                          className="p-1.5 text-rose-400 hover:text-rose-300 bg-rose-500/10 hover:bg-rose-500/20 rounded-lg"
                          title="Hapus"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Add Kas Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Pembayaran Uang Kas"
      >
        {errorMsg && (
          <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs flex items-center space-x-2">
            <AlertCircle className="w-4 h-4 text-rose-400 flex-shrink-0" />
            <span>{errorMsg}</span>
          </div>
        )}

        <form onSubmit={handleAddSubmit} className="space-y-4 text-xs">
          <div>
            <label className="block text-slate-400 font-bold mb-1">Pilih Anggota</label>
            <select
              value={formData.id_anggota}
              onChange={(e) => setFormData({ ...formData, id_anggota: parseInt(e.target.value) || 0 })}
              className="w-full px-3 py-2.5 rounded-xl bg-slate-800 border border-slate-700 text-white focus:outline-none focus:border-blue-500"
              required
            >
              <option value={0}>-- Pilih Anggota --</option>
              {anggotaList.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.nama} (NRA: {a.nra || '-'})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-slate-400 font-bold mb-1">Nominal Iuran (Rp)</label>
            <input
              type="number"
              value={formData.nominal}
              onChange={(e) => setFormData({ ...formData, nominal: e.target.value })}
              placeholder="Contoh: 20000"
              className="w-full px-3 py-2.5 rounded-xl bg-slate-800 border border-slate-700 text-white focus:outline-none focus:border-blue-500"
              required
            />
          </div>

          <div>
            <label className="block text-slate-400 font-bold mb-1">Keterangan</label>
            <input
              type="text"
              value={formData.keterangan}
              onChange={(e) => setFormData({ ...formData, keterangan: e.target.value })}
              placeholder="Iuran Kas Anggota"
              className="w-full px-3 py-2.5 rounded-xl bg-slate-800 border border-slate-700 text-white focus:outline-none focus:border-blue-500"
            />
          </div>

          <div className="pt-3 flex justify-end space-x-2">
            <button
              type="button"
              onClick={() => setIsModalOpen(false)}
              className="px-4 py-2 bg-slate-800 text-slate-300 rounded-xl font-bold"
            >
              Batal
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl font-bold flex items-center space-x-1"
            >
              {isSubmitting && <Loader2 className="w-4 h-4 animate-spin mr-1" />}
              <span>Simpan Kas</span>
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
