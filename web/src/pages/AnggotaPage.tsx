import React, { useEffect, useState } from 'react';
import { apiService } from '../services/api';
import { Anggota } from '../types';
import { useAuth } from '../context/AuthContext';
import { Modal } from '../components/Modal';
import { Search, Plus, UserCheck, Shield, Phone, MapPin, AlertCircle, Trash2, Edit2, Loader2 } from 'lucide-react';

export const AnggotaPage: React.FC = () => {
  const { isAdminOrTreasurer, user } = useAuth();
  const [anggotaList, setAnggotaList] = useState<Anggota[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [selectedMember, setSelectedMember] = useState<Anggota | null>(null);

  // Modal states
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formData, setFormData] = useState({
    nama: '',
    nra: '',
    alamat: '',
    no_hp: '',
    role: 'MEMBER',
    username: '',
    password: '',
  });
  const [actionError, setActionError] = useState<string | null>(null);

  const fetchAnggota = async () => {
    setLoading(true);
    try {
      const res = await apiService.getAnggotaList();
      if (res.status === 'success' && res.data) {
        setAnggotaList(res.data);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAnggota();
  }, []);

  const filteredList = anggotaList.filter((m) => {
    const matchSearch =
      (m.nama || '').toLowerCase().includes(search.toLowerCase()) ||
      (m.nra || '').toLowerCase().includes(search.toLowerCase()) ||
      (m.username || '').toLowerCase().includes(search.toLowerCase());
    
    if (roleFilter === 'ALL') return matchSearch;
    return matchSearch && (m.role || '').toUpperCase() === roleFilter;
  });

  const handleAddSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.nama.trim()) {
      setActionError('Nama Anggota wajib diisi');
      return;
    }

    setIsSubmitting(true);
    setActionError(null);

    try {
      const res = await apiService.addAnggota({
        ...formData,
        role: formData.role.toUpperCase(),
      });
      if (res.status === 'success') {
        setIsAddModalOpen(false);
        setFormData({ nama: '', nra: '', alamat: '', no_hp: '', role: 'MEMBER', username: '', password: '' });
        fetchAnggota();
      } else {
        setActionError(res.message || 'Gagal menambah anggota');
      }
    } catch (err: any) {
      setActionError(err.message || 'Gagal menyimpan data anggota');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Apakah Anda yakin ingin menghapus anggota ini?')) return;
    try {
      const res = await apiService.deleteAnggota(id, user?.role || 'ADMIN');
      if (res.status === 'success') {
        fetchAnggota();
        setSelectedMember(null);
      } else {
        alert(res.message || 'Gagal menghapus anggota');
      }
    } catch (err: any) {
      alert(err.message || 'Gagal menghapus anggota');
    }
  };

  const formatRupiah = (val?: number) => {
    return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(val || 0);
  };

  return (
    <div className="space-y-6">
      {/* Header & Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-white">Daftar Anggota Nebo Sukabumi</h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Total {anggotaList.length} Anggota terdaftar di database
          </p>
        </div>

        {isAdminOrTreasurer && (
          <button
            onClick={() => {
              setActionError(null);
              setIsAddModalOpen(true);
            }}
            className="flex items-center space-x-2 px-4 py-2.5 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-xs font-bold transition-all shadow-lg shadow-blue-600/20 self-start md:self-auto"
          >
            <Plus className="w-4 h-4" />
            <span>Tambah Anggota Baru</span>
          </button>
        )}
      </div>

      {/* Search & Filter Bar */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="w-4 h-4 absolute left-3.5 top-3.5 text-slate-400" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Cari nama, NRA, atau username..."
            className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
          />
        </div>

        <select
          value={roleFilter}
          onChange={(e) => setRoleFilter(e.target.value)}
          className="px-3 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-xs text-slate-300 focus:outline-none focus:border-blue-500"
        >
          <option value="ALL">Semua Role</option>
          <option value="ADMIN">ADMIN</option>
          <option value="BENDAHARA">BENDAHARA</option>
          <option value="MEMBER">MEMBER / ANGGOTA</option>
          <option value="GUEST">GUEST</option>
          <option value="DEVELOPER">DEVELOPER</option>
        </select>
      </div>

      {/* Member Table / Grid */}
      {loading ? (
        <div className="p-12 text-center text-slate-400 text-sm">Memuat data anggota...</div>
      ) : filteredList.length === 0 ? (
        <div className="p-12 text-center text-slate-500 text-sm bg-slate-900 border border-slate-800 rounded-2xl">
          Tidak ada data anggota yang sesuai pencarian.
        </div>
      ) : (
        <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-950 text-slate-400 font-bold uppercase border-b border-slate-800">
                <tr>
                  <th className="px-4 py-3.5">Anggota</th>
                  <th className="px-4 py-3.5">NRA</th>
                  <th className="px-4 py-3.5">Role</th>
                  <th className="px-4 py-3.5">Saldo Kas</th>
                  <th className="px-4 py-3.5">Sisa Cicilan</th>
                  <th className="px-4 py-3.5 text-right">Aksi</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {filteredList.map((m) => (
                  <tr key={m.id} className="hover:bg-slate-800/40 transition-colors">
                    <td className="px-4 py-3 font-semibold text-white flex items-center space-x-3">
                      <div className="w-8 h-8 rounded-full bg-blue-600/20 text-blue-400 flex items-center justify-center font-bold">
                        {m.nama ? m.nama.charAt(0).toUpperCase() : 'A'}
                      </div>
                      <div>
                        <div>{m.nama}</div>
                        {m.no_hp && <div className="text-[10px] text-slate-500">{m.no_hp}</div>}
                      </div>
                    </td>
                    <td className="px-4 py-3 font-mono text-slate-400">{m.nra || '-'}</td>
                    <td className="px-4 py-3">
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-800 text-slate-300 border border-slate-700">
                        {m.role || 'MEMBER'}
                      </span>
                    </td>
                    <td className="px-4 py-3 font-bold text-emerald-400">{formatRupiah(m.uang_kas)}</td>
                    <td className="px-4 py-3 font-bold text-amber-400">{formatRupiah(m.sisa_cicilan)}</td>
                    <td className="px-4 py-3 text-right">
                      <button
                        onClick={() => setSelectedMember(m)}
                        className="px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-blue-400 rounded-lg text-[11px] font-bold"
                      >
                        Detail
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Member Detail Modal */}
      {selectedMember && (
        <Modal
          isOpen={!!selectedMember}
          onClose={() => setSelectedMember(null)}
          title={`Detail: ${selectedMember.nama}`}
        >
          <div className="space-y-4 text-xs text-slate-300">
            <div className="p-3 rounded-xl bg-slate-800/60 border border-slate-700/50 space-y-1">
              <div className="text-sm font-bold text-white">{selectedMember.nama}</div>
              <div className="text-slate-400">NRA: {selectedMember.nra || '-'}</div>
              <div className="text-slate-400">Username: {selectedMember.username || '-'}</div>
              <div className="text-slate-400">Role: <span className="text-blue-400 font-semibold">{selectedMember.role}</span></div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/20">
                <span className="text-[10px] font-bold uppercase text-emerald-400">Uang Kas</span>
                <div className="text-base font-bold text-emerald-300 mt-1">{formatRupiah(selectedMember.uang_kas)}</div>
              </div>
              <div className="p-3 rounded-xl bg-purple-500/10 border border-purple-500/20">
                <span className="text-[10px] font-bold uppercase text-purple-400">Iuran Aniv</span>
                <div className="text-base font-bold text-purple-300 mt-1">{formatRupiah(selectedMember.iuran_aniv)}</div>
              </div>
            </div>

            <div className="p-3 rounded-xl bg-amber-500/10 border border-amber-500/20 space-y-1">
              <span className="text-[10px] font-bold uppercase text-amber-400">Status Cicilan</span>
              <div className="flex justify-between items-center text-xs text-slate-300 pt-1">
                <span>Harga Barang:</span>
                <span className="font-bold">{formatRupiah(selectedMember.harga_barang)}</span>
              </div>
              <div className="flex justify-between items-center text-xs text-slate-300">
                <span>Total Dibayar:</span>
                <span className="font-bold text-emerald-400">{formatRupiah(selectedMember.total_cicilan)}</span>
              </div>
              <div className="flex justify-between items-center text-xs text-slate-300 font-bold border-t border-amber-500/20 pt-1 mt-1">
                <span>Sisa Cicilan:</span>
                <span className="text-amber-300">{formatRupiah(selectedMember.sisa_cicilan)}</span>
              </div>
            </div>

            {isAdminOrTreasurer && (
              <div className="pt-2 flex justify-end">
                <button
                  onClick={() => handleDelete(selectedMember.id)}
                  className="flex items-center space-x-1 px-3 py-2 bg-rose-500/20 hover:bg-rose-500/30 text-rose-400 rounded-xl font-bold transition-all"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  <span>Hapus Anggota</span>
                </button>
              </div>
            )}
          </div>
        </Modal>
      )}

      {/* Add Member Modal */}
      <Modal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        title="Tambah Anggota Baru"
      >
        {actionError && (
          <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs flex items-center space-x-2">
            <AlertCircle className="w-4 h-4 text-rose-400 flex-shrink-0" />
            <span>{actionError}</span>
          </div>
        )}

        <form onSubmit={handleAddSubmit} className="space-y-3 text-xs">
          <div>
            <label className="block text-slate-400 font-bold mb-1">Nama Lengkap</label>
            <input
              type="text"
              value={formData.nama}
              onChange={(e) => setFormData({ ...formData, nama: e.target.value })}
              className="w-full px-3 py-2 rounded-xl bg-slate-800 border border-slate-700 text-white focus:outline-none focus:border-blue-500"
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-400 font-bold mb-1">NRA</label>
              <input
                type="text"
                value={formData.nra}
                onChange={(e) => setFormData({ ...formData, nra: e.target.value })}
                className="w-full px-3 py-2 rounded-xl bg-slate-800 border border-slate-700 text-white focus:outline-none focus:border-blue-500"
              />
            </div>
            <div>
              <label className="block text-slate-400 font-bold mb-1">Role</label>
              <select
                value={formData.role}
                onChange={(e) => setFormData({ ...formData, role: e.target.value })}
                className="w-full px-3 py-2 rounded-xl bg-slate-800 border border-slate-700 text-white focus:outline-none focus:border-blue-500"
              >
                <option value="MEMBER">MEMBER</option>
                <option value="BENDAHARA">BENDAHARA</option>
                <option value="ADMIN">ADMIN</option>
                <option value="GUEST">GUEST</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-400 font-bold mb-1">Username</label>
              <input
                type="text"
                value={formData.username}
                onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                className="w-full px-3 py-2 rounded-xl bg-slate-800 border border-slate-700 text-white focus:outline-none focus:border-blue-500"
              />
            </div>
            <div>
              <label className="block text-slate-400 font-bold mb-1">Password</label>
              <input
                type="password"
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                className="w-full px-3 py-2 rounded-xl bg-slate-800 border border-slate-700 text-white focus:outline-none focus:border-blue-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-slate-400 font-bold mb-1">No. HP</label>
            <input
              type="text"
              value={formData.no_hp}
              onChange={(e) => setFormData({ ...formData, no_hp: e.target.value })}
              className="w-full px-3 py-2 rounded-xl bg-slate-800 border border-slate-700 text-white focus:outline-none focus:border-blue-500"
            />
          </div>

          <div className="pt-3 flex justify-end space-x-2">
            <button
              type="button"
              onClick={() => setIsAddModalOpen(false)}
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
              <span>Simpan Anggota</span>
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
