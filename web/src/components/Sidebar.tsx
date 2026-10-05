import React from 'react';
import {
  LayoutDashboard,
  Users,
  CreditCard,
  Wallet,
  Compass,
  ArrowUpRight,
  FileText,
  Settings,
  X,
  ShieldAlert
} from 'lucide-react';

export interface NavItem {
  id: string;
  label: string;
  icon: React.ElementType;
}

export const navItems: NavItem[] = [
  { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { id: 'anggota', label: 'Anggota', icon: Users },
  { id: 'cicilan', label: 'Cicilan', icon: CreditCard },
  { id: 'kas', label: 'Uang Kas', icon: Wallet },
  { id: 'kas_keliling', label: 'Kas Keliling', icon: Compass },
  { id: 'pengeluaran', label: 'Pengeluaran', icon: ArrowUpRight },
  { id: 'laporan', label: 'Laporan', icon: FileText },
  { id: 'settings', label: 'Pengaturan', icon: Settings },
];

interface SidebarProps {
  activeTab: string;
  onSelectTab: (tabId: string) => void;
  isOpen: boolean;
  onClose: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ activeTab, onSelectTab, isOpen, onClose }) => {
  return (
    <>
      {/* Mobile Backdrop */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-slate-950/80 backdrop-blur-sm lg:hidden transition-opacity"
          onClick={onClose}
        />
      )}

      {/* Sidebar Drawer */}
      <aside
        className={`fixed top-0 left-0 z-50 h-full w-64 bg-slate-900 border-r border-slate-800 flex flex-col transform transition-transform duration-300 ease-in-out lg:translate-x-0 lg:static lg:z-auto ${
          isOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Brand Header */}
        <div className="p-5 border-b border-slate-800 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-blue-700 to-indigo-500 flex items-center justify-center text-white font-extrabold shadow-lg shadow-blue-500/20">
              NS
            </div>
            <div>
              <div className="font-extrabold text-white text-base tracking-wider">NEBO</div>
              <div className="text-[10px] font-semibold text-blue-400 tracking-widest uppercase">SUKABUMI</div>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800 lg:hidden"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Navigation Items */}
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => {
                  onSelectTab(item.id);
                  onClose();
                }}
                className={`w-full flex items-center space-x-3 px-3.5 py-2.5 rounded-xl text-sm font-semibold transition-all ${
                  isActive
                    ? 'bg-blue-600 text-white shadow-lg shadow-blue-600/25'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                }`}
              >
                <Icon className={`w-5 h-5 ${isActive ? 'text-white' : 'text-slate-400'}`} />
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>

        {/* Footer info */}
        <div className="p-4 border-t border-slate-800/80">
          <div className="bg-slate-800/40 rounded-xl p-3 border border-slate-800/80 text-xs">
            <div className="font-semibold text-slate-300">Nebo Sukabumi Web</div>
            <div className="text-[11px] text-slate-500 mt-0.5">v2.5.0 • Official Cloud Portal</div>
          </div>
        </div>
      </aside>
    </>
  );
};
