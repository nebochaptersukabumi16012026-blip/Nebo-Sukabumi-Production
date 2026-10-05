import React from 'react';
import { useAuth } from '../context/AuthContext';
import { Settings, Shield, Server, Globe, User, CheckCircle2 } from 'lucide-react';

export const SettingsPage: React.FC = () => {
  const { user } = useAuth();

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h2 className="text-xl font-bold text-white">Pengaturan & Informasi Sistem</h2>
        <p className="text-xs text-slate-400 mt-0.5">
          Informasi akun terhubung dan konfigurasi server portal Nebo Sukabumi
        </p>
      </div>

      {/* Account Info */}
      <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-4 shadow-xl">
        <h3 className="text-sm font-bold text-white flex items-center space-x-2">
          <User className="w-4 h-4 text-blue-400" />
          <span>Informasi Akun Anda</span>
        </h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
          <div className="p-3 rounded-xl bg-slate-800/60">
            <span className="text-slate-400 font-semibold">Nama Lengkap</span>
            <div className="text-white font-bold text-sm mt-0.5">{user?.nama || '-'}</div>
          </div>
          <div className="p-3 rounded-xl bg-slate-800/60">
            <span className="text-slate-400 font-semibold">Username / NRA</span>
            <div className="text-white font-bold text-sm mt-0.5">{user?.username} ({user?.nra || 'N/A'})</div>
          </div>
          <div className="p-3 rounded-xl bg-slate-800/60">
            <span className="text-slate-400 font-semibold">Role Hak Akses</span>
            <div className="text-blue-400 font-bold text-sm mt-0.5">{user?.role}</div>
          </div>
          <div className="p-3 rounded-xl bg-slate-800/60">
            <span className="text-slate-400 font-semibold">Status Sesi</span>
            <div className="text-emerald-400 font-bold text-sm mt-0.5 flex items-center space-x-1">
              <CheckCircle2 className="w-4 h-4" />
              <span>Terautentikasi</span>
            </div>
          </div>
        </div>
      </div>

      {/* Backend Infrastructure */}
      <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 space-y-4 shadow-xl">
        <h3 className="text-sm font-bold text-white flex items-center space-x-2">
          <Server className="w-4 h-4 text-emerald-400" />
          <span>Infrastruktur Backend API Production</span>
        </h3>
        <div className="space-y-3 text-xs text-slate-300">
          <div className="flex justify-between items-center p-3 rounded-xl bg-slate-800/40">
            <span className="text-slate-400 font-medium">Domain Target:</span>
            <span className="font-mono text-emerald-400 font-bold">https://nebosukabumi.net</span>
          </div>
          <div className="flex justify-between items-center p-3 rounded-xl bg-slate-800/40">
            <span className="text-slate-400 font-medium">Endpoint API Active:</span>
            <span className="font-mono text-blue-400 font-bold">https://nebosukabumi.net/api/</span>
          </div>
          <div className="flex justify-between items-center p-3 rounded-xl bg-slate-800/40">
            <span className="text-slate-400 font-medium">Database Backend:</span>
            <span className="font-mono text-slate-200">MySQL (Shared cPanel Production)</span>
          </div>
          <div className="flex justify-between items-center p-3 rounded-xl bg-slate-800/40">
            <span className="text-slate-400 font-medium">Android & Web Sync Mode:</span>
            <span className="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 font-bold text-[10px]">
              Direct Shared API Contract
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
