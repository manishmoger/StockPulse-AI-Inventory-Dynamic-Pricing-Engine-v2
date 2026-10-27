/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        accent: { DEFAULT: '#c2410c', light: '#ea580c', muted: '#fed7aa' },
        surface: '#ffffff',
        page: '#fafaf9',
        border: '#e7e5e4',
        text: { primary: '#1c1917', secondary: '#78716c', muted: '#a8a29e' },
      },
      fontFamily: { sans: ['Inter', 'system-ui', 'sans-serif'] },
    },
  },
  plugins: [],
}
