/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Primary - Forest green
        primary: {
          50: '#E8F5EC',
          100: '#D1EBD9',
          200: '#A3D7B3',
          300: '#75C38D',
          400: '#47AF67',
          500: '#2D5A47',  // Main forest color
          600: '#3D7A5F',  // Light
          700: '#1D3A2F',  // Dark
          800: '#152A23',
          900: '#0D1A15',
        },
        // Secondary - Warm neutrals
        secondary: {
          50: '#FFFDF8',   // Cream
          100: '#F5F0E8',  // Warm Sand
          200: '#E8E4DC',  // Stone
          300: '#D8D4CC',
          400: '#C8C4BC',
          500: '#B8B4AC',
          600: '#989490',
          700: '#787470',
          800: '#585450',
          900: '#383430',
        },
        // Accent - Terracotta
        accent: {
          50: '#FCF0EC',
          100: '#F9E1D9',
          200: '#F3C3B3',
          300: '#EDA58D',
          400: '#E78767',
          500: '#C4705A',
          600: '#9D5A48',
          700: '#764336',
          800: '#4F2D24',
          900: '#281612',
        },
        // Gold
        gold: {
          50: '#FDF8E8',
          100: '#FBF1D1',
          200: '#F7E3A3',
          300: '#F3D575',
          400: '#EFC747',
          500: '#D4A853',
          600: '#AA8642',
          700: '#7F6532',
          800: '#554321',
          900: '#2A2211',
        },
        // Semantic
        error: {
          50: '#FCF0F0',
          100: '#F9E1E1',
          200: '#F3C3C3',
          300: '#EDA5A5',
          400: '#E78787',
          500: '#C45A5A',
          600: '#9D4848',
          700: '#763636',
          800: '#4F2424',
          900: '#281212',
        },
        success: {
          50: '#E8F5EC',
          100: '#D1EBD9',
          200: '#A3D7B3',
          300: '#75C38D',
          400: '#47AF67',
          500: '#3A8A5C',
          600: '#2E6E4A',
          700: '#235237',
          800: '#173725',
          900: '#0C1B12',
        },
        warning: {
          50: '#FDF8E8',
          100: '#FBF1D1',
          200: '#F7E3A3',
          300: '#F3D575',
          400: '#EFC747',
          500: '#D4A853',
          600: '#996B00',
          700: '#7F6532',
          800: '#554321',
          900: '#2A2211',
        },
        info: {
          50: '#E8F0FC',
          100: '#D1E1F9',
          200: '#A3C3F3',
          300: '#75A5ED',
          400: '#4787E7',
          500: '#5A8AC4',
          600: '#486E9D',
          700: '#365376',
          800: '#24374F',
          900: '#121C28',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        serif: ['Lora', 'Georgia', 'serif'],
      },
      spacing: {
        '18': '4.5rem',
        '88': '22rem',
        '128': '32rem',
      },
      borderRadius: {
        'sm': '4px',
        'md': '8px',
        'lg': '16px',
      },
      boxShadow: {
        'sm': '0 1px 2px rgba(0, 0, 0, 0.05)',
        'md': '0 4px 12px rgba(0, 0, 0, 0.08)',
        'lg': '0 8px 24px rgba(0, 0, 0, 0.12)',
        'inner': 'inset 0 2px 4px rgba(0, 0, 0, 0.05)',
      },
      transitionDuration: {
        'fast': '150ms',
        'normal': '250ms',
        'slow': '400ms',
      },
    },
  },
  plugins: [],
}
