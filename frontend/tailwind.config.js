/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // CineSync Palette
        dark: {
          bg: '#0A0D14',
          card: '#161C28',
          surface: '#121622',
          border: '#262E42',
        },
        primary: {
          400: '#FF8A5B',
          500: '#FF6B35',
          600: '#E85520',
          700: '#CC4412',
        },
        accent: {
          cyan: '#38BDF8',
          blue: '#38BDF8',
          emerald: '#10B981',
          amber: '#F59E0B',
        },
        cinesync: {
          bg: '#0A0D14',
          surface: '#121622',
          card: '#1A2030',
          'card-hover': '#222A3F',
          border: '#262E42',
          orange: '#FF6B35',
          'orange-hover': '#FF5722',
          cyan: '#38BDF8',
          green: '#10B981',
          yellow: '#F59E0B',
        }
      },
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'Inter', 'sans-serif'],
      },
      boxShadow: {
        'glow-orange': '0 0 25px -5px rgba(255, 107, 53, 0.4)',
        'glow-cyan': '0 0 25px -5px rgba(56, 189, 248, 0.4)',
        'screen-glow': '0 10px 40px -10px rgba(56, 189, 248, 0.35)',
      }
    },
  },
  plugins: [],
}
