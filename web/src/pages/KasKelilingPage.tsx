import React, { useEffect, useState } from 'react';
import { apiService } from '../services/api';
import { KasKelilingItem } from '../types';
import { useAuth } from '../context/AuthContext';
import { Modal } from '../components/Modal';
import { Compass, Plus, Trash2, ArrowDownRight, ArrowUpRight, Search, AlertCircle, Loader2 } from 'lucide-react';

export const KasKelilingPage: React.FC = () => {
  const { isAdminOrTreasurer, user } = useAuth();
  const [transaksiList, setTransaksiList] = useState<KasKelilingItem[]>([]);
  const [totals, setTotals] = useState({ total_pemasukan: 0, total_pengeluaran: 0, saldo_kas_keliling: 0 });
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [formData, setFormData] = useState({
    jenis_transaksi: 'PEMASUKAN',
    nominal: '',
    tanggal: new Date().toISOString().split('T')[0],
    keterangan: '',
    bulan: '',
    tahun: new Date().getFullYear().toString(),
  });

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await apiService.getKasKeliling();
      if (res && res.data) {
        const d = res.data;
        setTransaksiList(d.transaksi || []);
        setTotals({
          total_pemasukan: d.total_pemasukan || 0,
          total_pengeluaran: d.total_pengeluaran || 0,
          saldo_kas_keliling: d.saldo_kas_keliling || 0,
        });
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
    if (!formData.nominal || parseFloat(formData.nominal) <= 0 || !formData.keterangan.trim()) {
      setErrorMsg('Nominal dan keterangan wajib diisi');
      return;
    }

    setIsSubmitting(true);
    setErrorMsg(null);

    try {
      const res = await apiService.addKasKeliling({
        jenis_transaksi: formData.jenis_transaksi,
        nominal: parseFloat(formData.nominal),
        tanggal: formData.tanggal,
        keterangan: formData.keterangan,
        bulan: formData.bulan || undefined,
        tahun: formData.tahun || undefined,
        created_by: user?.nama || user?.username || 'Admin',
      });

      if (res.status === 'success') {
        setIsModalOpen(false);
        setFormData({
          jenis_transaksi: 'PEMASUKAN',
          nominal: '',
          tanggal: new Date().toISOString().split('T')[0],
          keterangan: '',
          bulan: '',
          tahun: new Date().getFullYear().toString(),
        });
        fetchData();
      } else {
        setErrorMsg(res.message || 'Gagal menyimpan kas keliling');
      }
    } catch (err: any) {
      setErrorMsg(err.message || 'Gagal menyimpan kas keliling');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Hapus transaksi kas keliling ini?')) return;
    try {
      const res = await apiService.deleteKasKeliling(id);
      if (res.status === 'success') {
        fetchData();
      } else {
        alert(res.message || 'Gagal menghapus transaksi');
      }
    } catch (err: any) {
      alert(err.message || 'Gagal menghapus transaksi');
    }
  };

  const filteredList = transaksiList.filter((t) =>
    (t.keterangan || '').toLowerCase().includes(search.toLowerCase()) ||
    (t.created_by || '').toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-6">
      {/* Top Bar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-white">Kas Keliling Komunitas</h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Pemasukan & pengeluaran operasional Kas Keliling Nebo Sukabumi
          </p>
        </div>

        {isAdminOrTreasurer && (
          <button
            onClick={() => {
              setErrorMsg(null);
              setIsModalOpen(true);
            }}
            className="flex items-center space-x-2 px-4 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-bold transition-all shadow-lg shadow-emerald-600/20 self-start md:self-auto"
          >
            <Plus className="w-4 h-4" />
            <span>Tambah Kas Keliling</span>
          </button>
        )}
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800">
          <span className="text-xs text-slate-400 font-semibold uppercase">Total Pemasukan</span>
          <div className="text-xl font-extrabold text-emerald-400 mt-1">{formatRupiah(totals.total_pemasukan)}</div>
        </div>
        <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800">
          <span className="text-xs text-slate-400 font-semibold uppercase">Total Pengeluaran</span>
          <div className="text-xl font-extrabold text-rose-400 mt-1">{formatRupiah(totals.total_pengeluaran)}</div>
        </div>
        <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800">
          <span className="text-xs text-slate-400 font-semibold uppercase">Saldo Kas Keliling</span>
          <div className="text-xl font-extrabold text-blue-400 mt-1">{formatRupiah(totals.saldo_kas_keliling)}</div>
        </div>
      </div>

      {/* Search Input */}
      <div className="relative max-w-md">
        <Search className="w-4 h-4 absolute left-3.5 top-3.5 text-slate-400" />
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Cari transaksi atau keterangan..."
          className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
        />
      </div>

      {/* Table */}
      {loading ? (
        <div className="p-12 text-center text-slate-400 text-sm">Memuat data kas keliling...</div>
      ) : filteredList.length === 0 ? (
        <div className="p-12 text-center text-slate-500 text-sm bg-slate-900 border border-slate-800 rounded-2xl">
          Belum ada transaksi kas keliling.
        </div>
      ) : (
        <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-950 text-slate-400 font-bold uppercase border-b border-slate-800">
                <tr>
                  <th className="px-4 py-3.5">Tanggal</th>
                  <th className="px-4 py-3.5">Jenis</th>
                  <th className="px-4 py-3.5">Nominal</th>
                  <th className="px-4 py-3.5">Keterangan</th>
                  <th className="px-4 py-3.5">Oleh</th>
                  {isAdminOrTreasurer && <th className="px-4 py-3.5 text-right">Aksi</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {filteredList.map((item) => {
                  const isMasuk = (item.jenis_transaksi || '').toUpperCase() === 'PEMASUKAN';
                  return (
                    <tr key={item.id} className="hover:bg-slate-800/40 transition-colors">
                      <td className="px-4 py-3 text-slate-400">{item.tanggal}</td>
                      <td className="px-4 py-3">
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          isMasuk ? 'bg-emerald-500/20 text-emerald-300' : 'bg-rose-500/20 text-rose-300'
                        }`}>
                          {item.jenis_transaksi}
                        </span>
                      </td>
                      <td className={`px-4 py-3 font-bold ${isMasuk ? 'text-emerald-400' : 'text-rose-400'}`}>
                        {isMasuk ? '+' : '-'}{formatRupiah(item.nominal)}
                      </td>
                      <td className="px-4 py-3 text-slate-300">{item.keterangan}</td>
                      <td className="px-4 py-3 text-slate-500">{item.created_by || '-'}</td>
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
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Add Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Tambah Kas Keliling"
      >
        {errorMsg && (
          <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs flex items-center space-x-2">
            <AlertCircle className="w-4 h-4 text-rose-400 flex-shrink-0" />
            <span>{errorMsg}</span>
          </div>
        )}

        <form onSubmit={handleAddSubmit} className="space-y-4 text-xs">
          <div>
            <label className="block text-slate-400 font-bold mb-1">Jenis Transaksi</label>
            <select
              value={formData.jenis_transaksi}
              onChange={(e) => setFormData({ ...formData, jenis_transaksi: e.target.value })}
              className="w-full px-3 py-2.5 rounded-xl bg-slate-800 border border-slate-700 text-white focus:outline-none focus:border-blue-500"
            >
              <option value="PEMASUKAN">PEMASUKAN (+)</option>
              <option value="PENGELUARAN">PENGELUARAN (-)</option>
            </select>
          </div>

          <div>
            <label className="block text-slate-400 font-bold mb-1">Nominal (Rp)</label>
            <input
              type="number"
              value={formData.nominal}
              onChange={(e) => setFormData({ ...formData, nominal: e.target.value })}
              placeholder="Contoh: 50000"
              className="w-full px-3 py-2.5 rounded-xl bg-slate-800 border border-slate-700 text-white focus:outline-none focus:border-blue-500"
              required
            />
          </div>

          <div>
            <label className="block text-slate-400 font-bold mb-1">Tanggal</label>
            <input
              type="date"
              value={formData.tanggal}
              onChange={(e) => setFormData({ ...formData, tanggal: e.target.value })}
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
              placeholder="Rincian kas keliling"
              className="w-full px-3 py-2.5 rounded-xl bg-slate-800 border border-slate-700 text-white focus:outline-none focus:border-blue-500"
              required
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
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl font-bold flex items-center space-x-1"
            >
              {isSubmitting && <Loader2 className="w-4 h-4 animate-spin mr-1" />}
              <span>Simpan Transaksi</span>
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
