import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { LoginPage } from './pages/LoginPage';
import { Navbar } from './components/Navbar';
import { Sidebar, navItems } from './components/Sidebar';
import { MobileNav } from './components/MobileNav';

import { DashboardPage } from './pages/DashboardPage';
import { AnggotaPage } from './pages/AnggotaPage';
import { CicilanPage } from './pages/CicilanPage';
import { KasPage } from './pages/KasPage';
import { KasKelilingPage } from './pages/KasKelilingPage';
import { PengeluaranPage } from './pages/PengeluaranPage';
import { LaporanPage } from './pages/LaporanPage';
import { SettingsPage } from './pages/SettingsPage';

const MainLayout: React.FC = () => {
  const { isAuthenticated } = useAuth();
  const [activeTab, setActiveTab] = useState('dashboard');
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  const currentTabInfo = navItems.find((n) => n.id === activeTab) || navItems[0];

  const renderContent = () => {
    switch (activeTab) {
      case 'dashboard':
        return <DashboardPage />;
      case 'anggota':
        return <AnggotaPage />;
      case 'cicilan':
        return <CicilanPage />;
      case 'kas':
        return <KasPage />;
      case 'kas_keliling':
        return <KasKelilingPage />;
      case 'pengeluaran':
        return <PengeluaranPage />;
      case 'laporan':
        return <LaporanPage />;
      case 'settings':
        return <SettingsPage />;
      default:
        return <DashboardPage />;
    }
  };

  return (
    <div className="min-h-screen flex bg-slate-950 text-slate-100">
      {/* Sidebar Navigation */}
      <Sidebar
        activeTab={activeTab}
        onSelectTab={setActiveTab}
        isOpen={isSidebarOpen}
        onClose={() => setIsSidebarOpen(false)}
      />

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 pb-16 lg:pb-0">
        <Navbar
          activeTabTitle={currentTabInfo.label}
          onToggleSidebar={() => setIsSidebarOpen(!isSidebarOpen)}
        />

        <main className="flex-1 p-4 sm:p-6 lg:p-8 max-w-7xl w-full mx-auto">
          {renderContent()}
        </main>
      </div>

      {/* Bottom Mobile Navigation */}
      <MobileNav
        activeTab={activeTab}
        onSelectTab={setActiveTab}
        onOpenMenu={() => setIsSidebarOpen(true)}
      />
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <MainLayout />
    </AuthProvider>
  );
};

export default App;
