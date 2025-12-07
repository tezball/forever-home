# UI Style Guide

A mobile-first design system for Forever Home. Clean, professional, and trustworthy.

## Design Principles

1. **Trust First** - Every element should reinforce credibility. Users are making emotional decisions about living beings.
2. **Clarity Over Cleverness** - Clear labels, obvious actions, no ambiguity.
3. **Mobile Native** - Designed for thumbs, not cursors. Desktop is an enhancement.
4. **Warmth With Restraint** - Approachable but not childish. Professional but not cold.
5. **Content Forward** - Pet photos are the hero. UI stays out of the way.

---

## Color Palette

### Primary Colors

| Name | Hex | Usage |
|------|-----|-------|
| **Forest** | `#2D5A47` | Primary actions, headers, trust indicators |
| **Forest Light** | `#3D7A5F` | Hover states, secondary emphasis |
| **Forest Dark** | `#1D3A2F` | Active states, text on light backgrounds |

### Secondary Colors

| Name | Hex | Usage |
|------|-----|-------|
| **Warm Sand** | `#F5F0E8` | Page backgrounds, cards |
| **Cream** | `#FFFDF8` | Card backgrounds, modals |
| **Stone** | `#E8E4DC` | Borders, dividers |

### Accent Colors

| Name | Hex | Usage |
|------|-----|-------|
| **Terracotta** | `#C4705A` | Notifications, highlights, favorites |
| **Soft Gold** | `#D4A853` | Verified badges, success states |

### Semantic Colors

| Name | Hex | Usage |
|------|-----|-------|
| **Success** | `#3A8A5C` | Confirmations, completed status |
| **Warning** | `#D4A853` | Alerts, pending states |
| **Error** | `#C45A5A` | Errors, destructive actions |
| **Info** | `#5A8AC4` | Informational messages |

### Neutrals

| Name | Hex | Usage |
|------|-----|-------|
| **Text Primary** | `#1A1A1A` | Headings, body text |
| **Text Secondary** | `#5C5C5C` | Captions, metadata |
| **Text Muted** | `#8C8C8C` | Placeholders, disabled |
| **White** | `#FFFFFF` | Backgrounds, text on dark |

### CSS Variables

```css
:root {
  --color-primary: #2D5A47;
  --color-primary-light: #3D7A5F;
  --color-primary-dark: #1D3A2F;

  --color-bg: #F5F0E8;
  --color-surface: #FFFDF8;
  --color-border: #E8E4DC;

  --color-accent: #C4705A;
  --color-gold: #D4A853;

  --color-success: #3A8A5C;
  --color-warning: #D4A853;
  --color-error: #C45A5A;
  --color-info: #5A8AC4;

  --color-text: #1A1A1A;
  --color-text-secondary: #5C5C5C;
  --color-text-muted: #8C8C8C;
}
```

---

## Typography

### Font Stack

```css
--font-sans: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
--font-serif: 'Lora', Georgia, serif;
```

- **Inter** - UI text, body copy, buttons, labels
- **Lora** - Headings, pet names, emotional moments

### Type Scale (Mobile)

| Name | Size | Weight | Line Height | Usage |
|------|------|--------|-------------|-------|
| **Display** | 32px | 600 | 1.2 | Hero headings (Lora) |
| **H1** | 28px | 600 | 1.25 | Page titles (Lora) |
| **H2** | 24px | 600 | 1.3 | Section headers (Lora) |
| **H3** | 20px | 600 | 1.35 | Card titles, pet names (Lora) |
| **H4** | 18px | 500 | 1.4 | Subsections (Inter) |
| **Body** | 16px | 400 | 1.5 | Default text (Inter) |
| **Body Small** | 14px | 400 | 1.5 | Secondary text (Inter) |
| **Caption** | 12px | 400 | 1.4 | Labels, metadata (Inter) |
| **Overline** | 11px | 600 | 1.3 | Category labels, uppercase (Inter) |

### Type Scale (Desktop: 768px+)

Scale up Display to 40px, H1 to 32px, H2 to 28px. Body remains 16px.

### Usage Guidelines

- **Pet names** always use Lora at H3 weight
- **Status badges** use Caption, uppercase
- **Form labels** use Body Small, medium weight
- **Buttons** use Body, medium weight
- Line length: 65-75 characters max for readability

---

## Spacing System

Base unit: **4px**

| Token | Value | Usage |
|-------|-------|-------|
| `--space-1` | 4px | Tight gaps, icon padding |
| `--space-2` | 8px | Between related elements |
| `--space-3` | 12px | Form field gaps |
| `--space-4` | 16px | Card padding, section gaps |
| `--space-5` | 24px | Between sections |
| `--space-6` | 32px | Major section breaks |
| `--space-8` | 48px | Page margins (mobile) |
| `--space-10` | 64px | Hero spacing |

### Mobile Margins

```css
.container {
  padding-left: 16px;
  padding-right: 16px;
}

@media (min-width: 768px) {
  .container {
    padding-left: 32px;
    padding-right: 32px;
    max-width: 1200px;
    margin: 0 auto;
  }
}
```

---

## Border Radius

| Token | Value | Usage |
|-------|-------|-------|
| `--radius-sm` | 4px | Buttons, inputs, small elements |
| `--radius-md` | 8px | Cards, modals |
| `--radius-lg` | 16px | Image containers, hero cards |
| `--radius-full` | 9999px | Avatars, pills, circular buttons |

---

## Shadows

```css
--shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.05);
--shadow-md: 0 4px 12px rgba(0, 0, 0, 0.08);
--shadow-lg: 0 8px 24px rgba(0, 0, 0, 0.12);
--shadow-inner: inset 0 2px 4px rgba(0, 0, 0, 0.05);
```

- Cards at rest: `--shadow-sm`
- Cards on hover/focus: `--shadow-md`
- Modals, dropdowns: `--shadow-lg`
- Pressed buttons: `--shadow-inner`

---

## Components

### Buttons

#### Primary Button
```
Background: var(--color-primary)
Text: white
Padding: 12px 24px
Border Radius: var(--radius-sm)
Font: 16px / 500

Hover: var(--color-primary-light)
Active: var(--color-primary-dark)
Disabled: opacity 0.5, cursor not-allowed
```

#### Secondary Button
```
Background: transparent
Border: 1.5px solid var(--color-primary)
Text: var(--color-primary)
Padding: 12px 24px

Hover: Background var(--color-primary), Text white
```

#### Tertiary / Text Button
```
Background: transparent
Text: var(--color-primary)
Padding: 8px 12px

Hover: underline
```

#### Destructive Button
```
Background: var(--color-error)
Text: white

Use sparingly. Require confirmation for destructive actions.
```

#### Button Sizes

| Size | Padding | Font | Min Height |
|------|---------|------|------------|
| Small | 8px 16px | 14px | 36px |
| Default | 12px 24px | 16px | 44px |
| Large | 16px 32px | 18px | 52px |

**Touch target minimum: 44px height on mobile**

---

### Cards

#### Pet Card (List View)
```
┌─────────────────────────────────┐
│  ┌─────────┐                    │
│  │         │  Pet Name          │
│  │  Image  │  Breed · Age       │
│  │         │  ○ Available       │
│  └─────────┘           ♡        │
└─────────────────────────────────┘

Background: var(--color-surface)
Border: 1px solid var(--color-border)
Border Radius: var(--radius-md)
Padding: 12px
Shadow: var(--shadow-sm)

Image: 80px × 80px, radius-md, object-fit cover
```

#### Pet Card (Grid View)
```
┌───────────────────┐
│                   │
│      Image        │
│                   │
├───────────────────┤
│ Pet Name          │
│ Breed · Size      │
│ ○ Available    ♡  │
└───────────────────┘

Image: 100% width, 3:2 aspect ratio
Card Min Width: 160px
Card Max Width: 280px
Gap: 16px
```

---

### Form Elements

#### Text Input
```
Height: 48px
Padding: 12px 16px
Border: 1.5px solid var(--color-border)
Border Radius: var(--radius-sm)
Background: var(--color-surface)
Font: 16px (prevents iOS zoom)

Focus: Border var(--color-primary), shadow 0 0 0 3px rgba(45,90,71,0.15)
Error: Border var(--color-error)
Disabled: Background var(--color-bg), opacity 0.7
```

#### Text Area
```
Min Height: 120px
Resize: vertical
Same styling as text input
```

#### Select / Dropdown
```
Same as text input
Include chevron-down icon on right
Padding-right: 44px for icon space
```

#### Checkbox
```
Size: 20px × 20px
Border: 1.5px solid var(--color-border)
Border Radius: 4px

Checked: Background var(--color-primary), checkmark white
Focus: Ring same as input
```

#### Radio
```
Size: 20px × 20px
Border: 1.5px solid var(--color-border)
Border Radius: full

Selected: Border var(--color-primary), inner dot 10px
```

#### Form Layout
```
Label above input
Label: Body Small, medium weight, margin-bottom 6px
Field gap: 16px
Section gap: 24px
Error message: Caption, color-error, margin-top 4px
```

---

### Status Badges

```
Padding: 4px 10px
Border Radius: var(--radius-full)
Font: Caption, uppercase, 600 weight
```

| Status | Background | Text |
|--------|------------|------|
| Available | `#E8F5EC` | `#2D5A47` |
| Pending | `#FFF5E6` | `#996B00` |
| Adopted | `#F0E8F5` | `#5A4570` |
| On Hold | `#E8E4DC` | `#5C5C5C` |
| Withdrawn | `#F5E8E8` | `#8C5C5C` |

---

### Navigation

#### Bottom Navigation (Mobile)
```
Height: 64px + safe-area-inset-bottom
Background: var(--color-surface)
Border-top: 1px solid var(--color-border)
Shadow: 0 -2px 8px rgba(0,0,0,0.05)

Items: 4-5 max
Icon: 24px
Label: 11px
Active: var(--color-primary)
Inactive: var(--color-text-muted)
```

```
┌────────────────────────────────────┐
│  🏠      🐾      ❤️      👤       │
│ Home   Browse  Saved  Profile     │
└────────────────────────────────────┘
```

#### Top Navigation (Desktop)
```
Height: 64px
Background: var(--color-surface)
Border-bottom: 1px solid var(--color-border)
Logo on left, nav links center, user menu right
```

---

### Modals / Bottom Sheets

#### Bottom Sheet (Mobile)
```
Background: var(--color-surface)
Border-radius: 16px 16px 0 0
Padding: 24px 16px
Max-height: 90vh
Drag handle: 40px × 4px, centered, var(--color-border)

Backdrop: rgba(0,0,0,0.4)
Animation: slide up 250ms ease-out
```

#### Modal (Desktop)
```
Background: var(--color-surface)
Border-radius: var(--radius-md)
Padding: 24px
Max-width: 480px
Shadow: var(--shadow-lg)

Backdrop: rgba(0,0,0,0.4)
Animation: fade + scale 200ms ease-out
```

---

### Image Gallery

#### Pet Profile Gallery
```
Primary image: Full width, 4:3 aspect ratio
Thumbnails: 64px × 64px strip below
Swipe enabled on mobile
Tap to open fullscreen viewer
```

#### Fullscreen Viewer
```
Background: black
Swipe to navigate
Pinch to zoom
X button top-right
Image counter bottom-center
```

---

## Icons

Use a consistent icon set. Recommended: **Lucide** or **Heroicons (outline)**

| Icon | Usage |
|------|-------|
| Heart (outline) | Favorite (inactive) |
| Heart (filled) | Favorite (active) |
| MapPin | Location |
| Phone | Contact |
| Globe | Website |
| Check | Success, verified |
| AlertCircle | Warning, info |
| X | Close, remove |
| ChevronRight | Navigate forward |
| ChevronDown | Dropdown, expand |
| Search | Search field |
| Filter | Filter controls |
| Plus | Add new |
| Edit | Edit action |
| Trash | Delete action |
| Camera | Upload image |
| User | Profile |
| Home | Home nav |
| PawPrint | Browse pets |

### Icon Sizes

| Context | Size |
|---------|------|
| Inline with text | 16px |
| Buttons | 20px |
| Navigation | 24px |
| Empty states | 48px |

---

## Motion

### Principles
- **Purposeful**: Motion guides attention, not decorates
- **Quick**: Most transitions 150-250ms
- **Natural**: Use ease-out for entering, ease-in for exiting

### Durations
```css
--duration-fast: 150ms;
--duration-normal: 250ms;
--duration-slow: 400ms;
```

### Easing
```css
--ease-out: cubic-bezier(0.25, 0.46, 0.45, 0.94);
--ease-in: cubic-bezier(0.55, 0.06, 0.68, 0.19);
--ease-in-out: cubic-bezier(0.65, 0.05, 0.36, 1);
```

### Common Animations

| Element | Animation |
|---------|-----------|
| Button hover | Background 150ms |
| Card hover | Shadow + translateY(-2px) 200ms |
| Modal enter | Fade + scale from 0.95 250ms |
| Bottom sheet | Slide up 250ms |
| Page transition | Fade 200ms |
| Favorite heart | Scale pulse 300ms |
| Loading spinner | Rotate 1s linear infinite |

---

## Responsive Breakpoints

```css
/* Mobile first - default styles are mobile */

/* Tablet */
@media (min-width: 768px) { }

/* Desktop */
@media (min-width: 1024px) { }

/* Wide */
@media (min-width: 1280px) { }
```

### Layout Adjustments

| Breakpoint | Grid Columns | Container | Navigation |
|------------|--------------|-----------|------------|
| < 768px | 2 | 100% - 32px | Bottom bar |
| 768px+ | 3 | 100% - 64px | Top bar |
| 1024px+ | 4 | max 1200px | Top bar |

---

## Accessibility

### Color Contrast
- All text meets WCAG AA (4.5:1 for body, 3:1 for large)
- Interactive elements have visible focus states
- Don't rely on color alone for meaning

### Touch Targets
- Minimum 44px × 44px for all interactive elements
- 8px minimum gap between touch targets

### Focus States
```css
:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}
```

### Screen Readers
- All images have descriptive alt text
- Icons with meaning have aria-labels
- Form inputs have associated labels
- Status changes announced via aria-live

### Motion
```css
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
```

---

## Dark Mode (Future)

Reserve dark mode for future implementation. When added:
- Swap `--color-bg` and `--color-surface` to dark variants
- Ensure green primary remains accessible
- Test pet photos against dark backgrounds
- Consider OLED-friendly true black option

---

## Sample Screens

### Pet Browse (Mobile)
```
┌────────────────────────────────────┐
│ ← Browse Pets            [Filter]  │
├────────────────────────────────────┤
│ ┌──────────────┐ ┌──────────────┐  │
│ │              │ │              │  │
│ │    Image     │ │    Image     │  │
│ │              │ │              │  │
│ ├──────────────┤ ├──────────────┤  │
│ │ Luna         │ │ Max          │  │
│ │ Husky · Med  │ │ Lab · Large  │  │
│ │ ○ Available ♡│ │ ○ Available ♡│  │
│ └──────────────┘ └──────────────┘  │
│                                    │
│ ┌──────────────┐ ┌──────────────┐  │
│ │              │ │              │  │
│ │    Image     │ │    Image     │  │
│ ...                                │
├────────────────────────────────────┤
│  🏠      🐾      ❤️      👤       │
│ Home   Browse  Saved  Profile     │
└────────────────────────────────────┘
```

### Pet Profile (Mobile)
```
┌────────────────────────────────────┐
│ ←                              ♡   │
├────────────────────────────────────┤
│                                    │
│           [Pet Photo]              │
│                                    │
│    ○  ○  ●  ○  ○  (indicators)    │
├────────────────────────────────────┤
│                                    │
│  Luna                              │
│  Siberian Husky                    │
│                                    │
│  ┌────────┬────────┬────────┐     │
│  │ 2 yrs  │ Medium │  ♀     │     │
│  │  Age   │  Size  │ Female │     │
│  └────────┴────────┴────────┘     │
│                                    │
│  ✓ Verified by Dr. Smith          │
│  ○ Available for adoption         │
│                                    │
│  About Luna                        │
│  ─────────                         │
│  Luna is a friendly, energetic     │
│  husky who loves long walks and    │
│  playing in the snow...            │
│                                    │
│  Rescue Organization               │
│  ─────────────────                 │
│  Happy Tails Rescue                │
│  📍 Portland, OR                   │
│  📞 (503) 555-0123                 │
│                                    │
├────────────────────────────────────┤
│  ┌──────────────────────────────┐  │
│  │     Apply to Adopt Luna      │  │
│  └──────────────────────────────┘  │
│                                    │
└────────────────────────────────────┘
```
