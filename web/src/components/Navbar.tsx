import React from 'react';
import { useAuth } from '../context/AuthContext';
import { LogOut, User as UserIcon, Shield, Menu } from 'lucide-react';

interface NavbarProps {
  onToggleSidebar?: () => void;
  activeTabTitle: string;
}

export const Navbar: React.FC<NavbarProps> = ({ onToggleSidebar, activeTabTitle }) => {
  const { user, logout } = useAuth();

  const getRoleBadge = (role?: string) => {
    const r = (role || 'MEMBER').toUpperCase();
    if (r === 'DEVELOPER') return 'bg-purple-500/20 text-purple-300 border-purple-500/30';
    if (r === 'ADMIN') return 'bg-rose-500/20 text-rose-300 border-rose-500/30';
    if (r === 'BENDAHARA') return 'bg-amber-500/20 text-amber-300 border-amber-500/30';
    return 'bg-blue-500/20 text-blue-300 border-blue-500/30';
  };

  return (
    <header className="sticky top-0 z-30 bg-slate-900/80 backdrop-blur-md border-b border-slate-800 px-4 py-3 sm:px-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <button
            onClick={onToggleSidebar}
            className="p-2 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800 lg:hidden"
            title="Menu"
          >
            <Menu className="w-6 h-6" />
          </button>
          <div>
            <h1 className="text-xl font-bold text-white tracking-tight">{activeTabTitle}</h1>
            <p className="text-xs text-slate-400 hidden sm:block">NEBO SUKABUMI Management Portal</p>
          </div>
        </div>

        <div className="flex items-center space-x-3">
          {user && (
            <div className="flex items-center space-x-3 bg-slate-800/80 rounded-xl px-3 py-1.5 border border-slate-700/50">
              <div className="w-8 h-8 rounded-lg bg-blue-600/30 text-blue-400 flex items-center justify-center font-bold">
                {user.nama ? user.nama.charAt(0).toUpperCase() : 'U'}
              </div>
              <div className="hidden md:block text-left">
                <div className="text-sm font-semibold text-slate-200">{user.nama || user.username}</div>
                <div className="flex items-center space-x-1">
                  <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded border ${getRoleBadge(user.role)}`}>
                    {user.role}
                  </span>
                  {user.nra && <span className="text-[10px] text-slate-400">NRA: {user.nra}</span>}
                </div>
              </div>
            </div>
          )}

          <button
            onClick={logout}
            className="flex items-center space-x-1.5 px-3 py-1.5 text-xs font-semibold text-rose-400 hover:text-rose-300 bg-rose-500/10 hover:bg-rose-500/20 rounded-xl border border-rose-500/20 transition-all"
            title="Keluar"
          >
            <LogOut className="w-4 h-4" />
            <span className="hidden sm:inline">Keluar</span>
          </button>
        </div>
      </div>
    </header>
  );
};
