import React, { useEffect, useState } from 'react';
import { apiService } from '../services/api';
import { PengeluaranItem } from '../types';
import { useAuth } from '../context/AuthContext';
import { Modal } from '../components/Modal';
import { ArrowUpRight, Plus, Trash2, Search, AlertCircle, Loader2 } from 'lucide-react';

export const PengeluaranPage: React.FC = () => {
  const { isAdminOrTreasurer, user } = useAuth();
  const [pengeluaranList, setPengeluaranList] = useState<PengeluaranItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('ALL');

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [formData, setFormData] = useState({
    keterangan: '',
    nominal: '',
    tanggal: new Date().toISOString().split('T')[0],
    jenis_kas: 'kas_utama',
  });

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await apiService.getPengeluaran();
      if (res && res.data) {
        setPengeluaranList(res.data);
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
      setErrorMsg('Keterangan dan nominal wajib diisi');
      return;
    }

    setIsSubmitting(true);
    setErrorMsg(null);

    try {
      const res = await apiService.addPengeluaran({
        keterangan: formData.keterangan,
        nominal: parseFloat(formData.nominal),
        tanggal: formData.tanggal,
        jenis_kas: formData.jenis_kas,
        created_by: user?.nama || user?.username || 'Admin',
      });

      if (res.status === 'success') {
        setIsModalOpen(false);
        setFormData({
          keterangan: '',
          nominal: '',
          tanggal: new Date().toISOString().split('T')[0],
          jenis_kas: 'kas_utama',
        });
        fetchData();
      } else {
        setErrorMsg(res.message || 'Gagal menyimpan pengeluaran');
      }
    } catch (err: any) {
      setErrorMsg(err.message || 'Gagal menyimpan pengeluaran');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Hapus pencatatan pengeluaran ini?')) return;
    try {
      const res = await apiService.deletePengeluaran(id);
      if (res.status === 'success') {
        fetchData();
      } else {
        alert(res.message || 'Gagal menghapus pengeluaran');
      }
    } catch (err: any) {
      alert(err.message || 'Gagal menghapus pengeluaran');
    }
  };

  const filteredList = pengeluaranList.filter((item) => {
    const matchSearch = (item.keterangan || '').toLowerCase().includes(search.toLowerCase());
    if (categoryFilter === 'ALL') return matchSearch;
    return matchSearch && (item.jenis_kas || '').toLowerCase() === categoryFilter.toLowerCase();
  });

  const totalFiltered = filteredList.reduce((acc, curr) => acc + (curr.nominal || 0), 0);

  return (
    <div className="space-y-6">
      {/* Top Bar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-white">Catatan Pengeluaran Kas</h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Riwayat dan pencatatan dana pengeluaran resmi Nebo Sukabumi
          </p>
        </div>

        {isAdminOrTreasurer && (
          <button
            onClick={() => {
              setErrorMsg(null);
              setIsModalOpen(true);
            }}
            className="flex items-center space-x-2 px-4 py-2.5 bg-rose-600 hover:bg-rose-500 text-white rounded-xl text-xs font-bold transition-all shadow-lg shadow-rose-600/20 self-start md:self-auto"
          >
            <Plus className="w-4 h-4" />
            <span>Catat Pengeluaran Baru</span>
          </button>
        )}
      </div>

      {/* Summary */}
      <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <span className="text-xs text-slate-400 font-semibold uppercase">Total Akumulasi Pengeluaran</span>
          <div className="text-2xl font-extrabold text-rose-400 mt-1">{formatRupiah(totalFiltered)}</div>
        </div>
        <div className="text-xs text-slate-400">
          Menampilkan <span className="text-white font-bold">{filteredList.length}</span> transaksi pengeluaran
        </div>
      </div>

      {/* Search & Filter Bar */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="w-4 h-4 absolute left-3.5 top-3.5 text-slate-400" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Cari keterangan pengeluaran..."
            className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
          />
        </div>

        <select
          value={categoryFilter}
          onChange={(e) => setCategoryFilter(e.target.value)}
          className="px-3 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-xs text-slate-300 focus:outline-none focus:border-blue-500"
        >
          <option value="ALL">Semua Sumber Kas</option>
          <option value="kas_utama">Kas Utama</option>
          <option value="kas_keliling">Kas Keliling</option>
          <option value="kas_aniv">Kas Anniversary</option>
        </select>
      </div>

      {/* Table */}
      {loading ? (
        <div className="p-12 text-center text-slate-400 text-sm">Memuat data pengeluaran...</div>
      ) : filteredList.length === 0 ? (
        <div className="p-12 text-center text-slate-500 text-sm bg-slate-900 border border-slate-800 rounded-2xl">
          Belum ada catatan pengeluaran.
        </div>
      ) : (
        <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-950 text-slate-400 font-bold uppercase border-b border-slate-800">
                <tr>
                  <th className="px-4 py-3.5">Tanggal</th>
                  <th className="px-4 py-3.5">Sumber Kas</th>
                  <th className="px-4 py-3.5">Nominal</th>
                  <th className="px-4 py-3.5">Keterangan</th>
                  <th className="px-4 py-3.5">Dicatat Oleh</th>
                  {isAdminOrTreasurer && <th className="px-4 py-3.5 text-right">Aksi</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {filteredList.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-800/40 transition-colors">
                    <td className="px-4 py-3 text-slate-400">{item.tanggal}</td>
                    <td className="px-4 py-3">
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-800 text-amber-300 border border-slate-700">
                        {item.jenis_kas ? item.jenis_kas.toUpperCase() : 'KAS UTAMA'}
                      </span>
                    </td>
                    <td className="px-4 py-3 font-bold text-rose-400">-{formatRupiah(item.nominal)}</td>
                    <td className="px-4 py-3 text-slate-200">{item.keterangan}</td>
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
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Add Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Catat Pengeluaran Baru"
      >
        {errorMsg && (
          <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs flex items-center space-x-2">
            <AlertCircle className="w-4 h-4 text-rose-400 flex-shrink-0" />
            <span>{errorMsg}</span>
          </div>
        )}

        <form onSubmit={handleAddSubmit} className="space-y-4 text-xs">
          <div>
            <label className="block text-slate-400 font-bold mb-1">Sumber Kas</label>
            <select
              value={formData.jenis_kas}
              onChange={(e) => setFormData({ ...formData, jenis_kas: e.target.value })}
              className="w-full px-3 py-2.5 rounded-xl bg-slate-800 border border-slate-700 text-white focus:outline-none focus:border-blue-500"
            >
              <option value="kas_utama">Kas Utama</option>
              <option value="kas_keliling">Kas Keliling</option>
              <option value="kas_aniv">Kas Anniversary</option>
            </select>
          </div>

          <div>
            <label className="block text-slate-400 font-bold mb-1">Nominal Pengeluaran (Rp)</label>
            <input
              type="number"
              value={formData.nominal}
              onChange={(e) => setFormData({ ...formData, nominal: e.target.value })}
              placeholder="Contoh: 150000"
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
            <label className="block text-slate-400 font-bold mb-1">Keterangan / Keperluan</label>
            <input
              type="text"
              value={formData.keterangan}
              onChange={(e) => setFormData({ ...formData, keterangan: e.target.value })}
              placeholder="Rincian barang/layanan yang dibeli"
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
              className="px-4 py-2 bg-rose-600 hover:bg-rose-500 text-white rounded-xl font-bold flex items-center space-x-1"
            >
              {isSubmitting && <Loader2 className="w-4 h-4 animate-spin mr-1" />}
              <span>Simpan Pengeluaran</span>
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
