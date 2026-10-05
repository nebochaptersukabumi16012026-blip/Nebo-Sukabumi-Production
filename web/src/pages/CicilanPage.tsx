import React, { useEffect, useState } from 'react';
import { apiService } from '../services/api';
import { CicilanAktifItem, CicilanTransaction, Anggota } from '../types';
import { useAuth } from '../context/AuthContext';
import { Modal } from '../components/Modal';
import { CreditCard, Plus, CheckCircle2, Clock, Trash2, Search, AlertCircle, Loader2 } from 'lucide-react';

export const CicilanPage: React.FC = () => {
  const { isAdminOrTreasurer, user } = useAuth();
  const [aktifList, setAktifList] = useState<CicilanAktifItem[]>([]);
  const [historyList, setHistoryList] = useState<CicilanTransaction[]>([]);
  const [anggotaList, setAnggotaList] = useState<Anggota[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'aktif' | 'history'>('aktif');
  const [search, setSearch] = useState('');

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [formData, setFormData] = useState({
    anggota_id: 0,
    nominal: '',
    tanggal: new Date().toISOString().split('T')[0],
    keterangan: 'Pembayaran Cicilan',
  });

  const fetchData = async () => {
    setLoading(true);
    try {
      const [resAktif, resHist, resAnggota] = await Promise.all([
        apiService.getDaftarCicilanAktif(),
        apiService.getCicilanHistory(),
        apiService.getAnggotaList(),
      ]);

      if (resAktif.status === 'success' && resAktif.data) setAktifList(resAktif.data);
      if (resHist.status === 'success' && resHist.data) setHistoryList(resHist.data);
      if (resAnggota.status === 'success' && resAnggota.data) setAnggotaList(resAnggota.data);
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
    if (!formData.anggota_id || !formData.nominal || parseFloat(formData.nominal) <= 0) {
      setErrorMsg('Pilih Anggota dan masukkan nominal valid');
      return;
    }

    setIsSubmitting(true);
    setErrorMsg(null);

    try {
      const res = await apiService.addCicilan(
        formData.anggota_id,
        parseFloat(formData.nominal),
        formData.tanggal,
        formData.keterangan,
        user?.role || 'ADMIN'
      );

      if (res.status === 'success') {
        setIsModalOpen(false);
        setFormData({
          anggota_id: 0,
          nominal: '',
          tanggal: new Date().toISOString().split('T')[0],
          keterangan: 'Pembayaran Cicilan',
        });
        fetchData();
      } else {
        setErrorMsg(res.message || 'Gagal menambahkan cicilan');
      }
    } catch (err: any) {
      setErrorMsg(err.message || 'Gagal memproses transaksi cicilan');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteHistory = async (id: number) => {
    if (!window.confirm('Hapus transaksi cicilan ini?')) return;
    try {
      const res = await apiService.deleteCicilan(id, user?.role || 'ADMIN');
      if (res.status === 'success') {
        fetchData();
      } else {
        alert(res.message || 'Gagal menghapus cicilan');
      }
    } catch (err: any) {
      alert(err.message || 'Gagal memproses penghapusan');
    }
  };

  const filteredAktif = aktifList.filter((item) =>
    (item.nama || '').toLowerCase().includes(search.toLowerCase()) ||
    (item.nra || '').toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-6">
      {/* Top Bar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-white">Kelola Cicilan Anggota</h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Daftar cicilan barang & riwayat angsuran anggota Nebo Sukabumi
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
            <span>Tambah Setoran Cicilan</span>
          </button>
        )}
      </div>

      {/* Tabs */}
      <div className="flex border-b border-slate-800 space-x-6 text-sm font-bold">
        <button
          onClick={() => setActiveTab('aktif')}
          className={`pb-3 transition-colors border-b-2 ${
            activeTab === 'aktif' ? 'border-blue-500 text-blue-400' : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          Cicilan Aktif ({aktifList.length})
        </button>
        <button
          onClick={() => setActiveTab('history')}
          className={`pb-3 transition-colors border-b-2 ${
            activeTab === 'history' ? 'border-blue-500 text-blue-400' : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          Riwayat Transaksi ({historyList.length})
        </button>
      </div>

      {/* Search Input */}
      <div className="relative max-w-md">
        <Search className="w-4 h-4 absolute left-3.5 top-3.5 text-slate-400" />
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Cari nama atau NRA..."
          className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
        />
      </div>

      {/* Content Body */}
      {loading ? (
        <div className="p-12 text-center text-slate-400 text-sm">Memuat data cicilan...</div>
      ) : activeTab === 'aktif' ? (
        filteredAktif.length === 0 ? (
          <div className="p-12 text-center text-slate-500 text-sm bg-slate-900 border border-slate-800 rounded-2xl">
            Tidak ada cicilan aktif saat ini. Seluruh anggota telah lunas atau belum ada cicilan baru.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {filteredAktif.map((item) => {
              const progressPct = item.harga_barang > 0 ? Math.min(100, Math.round((item.sudah_dibayar / item.harga_barang) * 100)) : 0;
              return (
                <div key={item.id} className="p-5 rounded-2xl bg-slate-900 border border-slate-800 space-y-3 shadow-lg">
                  <div className="flex justify-between items-start">
                    <div>
                      <h4 className="font-bold text-white text-base">{item.nama}</h4>
                      <p className="text-xs text-slate-400">NRA: {item.nra || '-'}</p>
                    </div>
                    <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-amber-500/10 text-amber-400 border border-amber-500/20">
                      Mencicil
                    </span>
                  </div>

                  <div className="space-y-1 text-xs text-slate-300 pt-2 border-t border-slate-800">
                    <div className="flex justify-between">
                      <span>Harga Barang:</span>
                      <span className="font-bold">{formatRupiah(item.harga_barang)}</span>
                    </div>
                    <div className="flex justify-between text-emerald-400">
                      <span>Sudah Dibayar:</span>
                      <span className="font-bold">{formatRupiah(item.sudah_dibayar)}</span>
                    </div>
                    <div className="flex justify-between text-amber-400 font-bold pt-1 border-t border-slate-800/80">
                      <span>Sisa Tagihan:</span>
                      <span>{formatRupiah(item.sisa_cicilan)}</span>
                    </div>
                  </div>

                  {/* Progress Bar */}
                  <div className="space-y-1 pt-2">
                    <div className="flex justify-between text-[10px] text-slate-400 font-semibold">
                      <span>Progres Pelunasan</span>
                      <span>{progressPct}%</span>
                    </div>
                    <div className="w-full bg-slate-800 h-2 rounded-full overflow-hidden">
                      <div
                        className="bg-gradient-to-r from-blue-500 to-emerald-400 h-full transition-all duration-500"
                        style={{ width: `${progressPct}%` }}
                      />
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )
      ) : (
        /* History Tab */
        <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-950 text-slate-400 font-bold uppercase border-b border-slate-800">
                <tr>
                  <th className="px-4 py-3.5">Tanggal</th>
                  <th className="px-4 py-3.5">Anggota ID</th>
                  <th className="px-4 py-3.5">Nominal Setoran</th>
                  <th className="px-4 py-3.5">Keterangan</th>
                  {isAdminOrTreasurer && <th className="px-4 py-3.5 text-right">Aksi</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {historyList.map((h) => (
                  <tr key={h.id} className="hover:bg-slate-800/40 transition-colors">
                    <td className="px-4 py-3 text-slate-400">{h.tanggal}</td>
                    <td className="px-4 py-3 font-semibold text-white">
                      ID #{h.anggota_id}
                    </td>
                    <td className="px-4 py-3 font-bold text-emerald-400">{formatRupiah(h.nominal)}</td>
                    <td className="px-4 py-3 text-slate-400">{h.keterangan || 'Pembayaran Cicilan'}</td>
                    {isAdminOrTreasurer && (
                      <td className="px-4 py-3 text-right">
                        <button
                          onClick={() => handleDeleteHistory(h.id)}
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

      {/* Add Cicilan Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Tambah Setoran Cicilan"
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
              value={formData.anggota_id}
              onChange={(e) => setFormData({ ...formData, anggota_id: parseInt(e.target.value) || 0 })}
              className="w-full px-3 py-2.5 rounded-xl bg-slate-800 border border-slate-700 text-white focus:outline-none focus:border-blue-500"
              required
            >
              <option value={0}>-- Pilih Anggota --</option>
              {anggotaList.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.nama} (NRA: {a.nra || '-'}) - Sisa: {formatRupiah(a.sisa_cicilan)}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-slate-400 font-bold mb-1">Nominal Setoran (Rp)</label>
            <input
              type="number"
              value={formData.nominal}
              onChange={(e) => setFormData({ ...formData, nominal: e.target.value })}
              placeholder="Contoh: 100000"
              className="w-full px-3 py-2.5 rounded-xl bg-slate-800 border border-slate-700 text-white focus:outline-none focus:border-blue-500"
              required
            />
          </div>

          <div>
            <label className="block text-slate-400 font-bold mb-1">Tanggal Bayar</label>
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
              placeholder="Keterangan transaksi"
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
              <span>Simpan Setoran</span>
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
