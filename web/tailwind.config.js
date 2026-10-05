/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        nebo: {
          50: '#F0F5FF',
          100: '#E0EBFF',
          200: '#C0D7FF',
          300: '#90B8FF',
          400: '#528FFF',
          500: '#2563EB',
          600: '#1D4ED8',
          700: '#1E40AF',
          800: '#1E3A8A',
          900: '#0F172A',
          gold: '#F59E0B',
          amber: '#D97706',
        }
      }
    },
  },
  plugins: [],
}
