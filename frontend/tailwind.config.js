/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Primary - Slate blue (from logo roof/text)
        primary: {
          50: '#F0F7FA',
          100: '#E1EFF5',
          200: '#C3DFEB',
          300: '#A5CFE1',
          400: '#87BFD7',
          500: '#7ABED0',  // Main logo blue
          600: '#5A9DB0',
          700: '#3D7A8A',
          800: '#2A5660',
          900: '#1A3640',
        },
        // Secondary - Sage green (from logo house left)
        secondary: {
          50: '#F5FAF5',
          100: '#EBF5EB',
          200: '#D7EBD6',
          300: '#C3E1C2',
          400: '#AFD7AD',
          500: '#9EC69B',  // Logo sage green
          600: '#7DA87A',
          700: '#5D8A5A',
          800: '#3E6C3C',
          900: '#1F4E1E',
        },
        // Accent - Warm orange (from logo house right)
        accent: {
          50: '#FEF7F0',
          100: '#FDEFE1',
          200: '#FBDFC3',
          300: '#F9CFA5',
          400: '#F7BF87',
          500: '#E8A668',  // Logo orange
          600: '#D08A4A',
          700: '#A86E38',
          800: '#805228',
          900: '#583618',
        },
        // Gold (harmonized with logo orange)
        gold: {
          50: '#FEF9F0',
          100: '#FDF3E1',
          200: '#FBE7C3',
          300: '#F9DBA5',
          400: '#F7CF87',
          500: '#E8B868',
          600: '#D09A4A',
          700: '#A87C38',
          800: '#805E28',
          900: '#584018',
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
