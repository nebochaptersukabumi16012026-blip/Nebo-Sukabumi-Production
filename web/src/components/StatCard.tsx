import React from 'react';
import { LucideIcon } from 'lucide-react';

interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: LucideIcon;
  variant?: 'blue' | 'emerald' | 'amber' | 'purple' | 'rose' | 'slate';
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  subtitle,
  icon: Icon,
  variant = 'blue'
}) => {
  const getStyles = () => {
    switch (variant) {
      case 'emerald':
        return {
          bg: 'bg-emerald-500/10 border-emerald-500/20',
          iconBg: 'bg-emerald-500/20 text-emerald-400',
          valueColor: 'text-emerald-300',
        };
      case 'amber':
        return {
          bg: 'bg-amber-500/10 border-amber-500/20',
          iconBg: 'bg-amber-500/20 text-amber-400',
          valueColor: 'text-amber-300',
        };
      case 'purple':
        return {
          bg: 'bg-purple-500/10 border-purple-500/20',
          iconBg: 'bg-purple-500/20 text-purple-400',
          valueColor: 'text-purple-300',
        };
      case 'rose':
        return {
          bg: 'bg-rose-500/10 border-rose-500/20',
          iconBg: 'bg-rose-500/20 text-rose-400',
          valueColor: 'text-rose-300',
        };
      case 'slate':
        return {
          bg: 'bg-slate-800/50 border-slate-700/50',
          iconBg: 'bg-slate-700/50 text-slate-300',
          valueColor: 'text-slate-200',
        };
      default:
        return {
          bg: 'bg-blue-500/10 border-blue-500/20',
          iconBg: 'bg-blue-500/20 text-blue-400',
          valueColor: 'text-blue-300',
        };
    }
  };

  const styles = getStyles();

  return (
    <div className={`p-4 sm:p-5 rounded-2xl border ${styles.bg} backdrop-blur-sm transition-all hover:scale-[1.01]`}>
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">{title}</span>
        <div className={`p-2.5 rounded-xl ${styles.iconBg}`}>
          <Icon className="w-5 h-5" />
        </div>
      </div>
      <div className={`text-xl sm:text-2xl font-extrabold mt-2 tracking-tight ${styles.valueColor}`}>
        {value}
      </div>
      {subtitle && (
        <div className="text-xs text-slate-400 mt-1 font-medium">{subtitle}</div>
      )}
    </div>
  );
};
