import React from 'react';
import { useAuth } from '../context/AuthContext';
import { Menu, Bell, User as UserIcon } from 'lucide-react';

interface NavbarProps {
  onToggleSidebar?: () => void;
  activeTabTitle: string;
}

export const Navbar: React.FC<NavbarProps> = ({ onToggleSidebar, activeTabTitle }) => {
  const { user } = useAuth();

  return (
    <header className="sticky top-0 z-30 bg-[#2563EB] text-white shadow-md px-4 py-3.5 sm:px-8 flex items-center justify-between">
      <div className="flex items-center space-x-4">
        <button
          onClick={onToggleSidebar}
          className="p-2 text-white/80 hover:text-white rounded-lg hover:bg-white/10 lg:hidden"
          title="Menu"
        >
          <Menu className="w-6 h-6" />
        </button>
        <div className="flex items-center space-x-3">
          <div className="w-8 h-8 rounded-lg bg-white/20 flex items-center justify-center font-bold text-white lg:hidden">
            NS
          </div>
          <span className="font-bold text-lg tracking-wide hidden sm:inline">Nebo Sukabumi</span>
        </div>
      </div>

      <div className="flex items-center space-x-4">
        {/* Notification Bell with Badge */}
        <button className="relative p-2 rounded-xl bg-white/10 hover:bg-white/20 text-white transition-all" title="Notifikasi">
          <Bell className="w-5 h-5" />
          <span className="absolute top-1 right-1 w-2.5 h-2.5 bg-rose-500 rounded-full ring-2 ring-blue-600"></span>
        </button>

        {/* User Profile Badge */}
        <div className="flex items-center space-x-2.5 bg-white/10 rounded-xl px-3 py-1.5 border border-white/10">
          <div className="w-8 h-8 rounded-lg bg-white/20 text-white flex items-center justify-center font-bold text-sm">
            {user?.nama ? user.nama.charAt(0).toUpperCase() : 'A'}
          </div>
          <div className="text-left">
            <div className="text-sm font-bold text-white leading-tight">{user?.nama || user?.username || 'Admin'}</div>
            <div className="text-[10px] text-blue-100 font-medium">Admin Portal</div>
          </div>
        </div>
      </div>
    </header>
  );
};
