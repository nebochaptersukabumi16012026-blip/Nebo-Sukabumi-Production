import React from 'react';
import { navItems } from './Sidebar';

interface MobileNavProps {
  activeTab: string;
  onSelectTab: (tabId: string) => void;
  onOpenMenu: () => void;
}

export const MobileNav: React.FC<MobileNavProps> = ({ activeTab, onSelectTab, onOpenMenu }) => {
  // Select top 4 items for bottom bar + More
  const quickItems = navItems.slice(0, 4);

  return (
    <div className="fixed bottom-0 left-0 right-0 z-30 bg-slate-900/90 backdrop-blur-lg border-t border-slate-800 py-2 px-3 lg:hidden">
      <div className="flex items-center justify-around">
        {quickItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => onSelectTab(item.id)}
              className={`flex flex-col items-center py-1 px-2 rounded-xl transition-all ${
                isActive ? 'text-blue-400 font-bold' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Icon className="w-5 h-5" />
              <span className="text-[10px] mt-1">{item.label}</span>
            </button>
          );
        })}
        <button
          onClick={onOpenMenu}
          className="flex flex-col items-center py-1 px-2 text-slate-400 hover:text-slate-200"
        >
          <div className="w-5 h-5 flex items-center justify-center font-bold text-xs border border-slate-600 rounded">
            •••
          </div>
          <span className="text-[10px] mt-1">Lainnya</span>
        </button>
      </div>
    </div>
  );
};
