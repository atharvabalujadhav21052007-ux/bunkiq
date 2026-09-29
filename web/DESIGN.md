---
name: Electric Neo-Brutalist Academic
colors:
  surface: '#faf9fd'
  surface-dim: '#dbd9dd'
  surface-bright: '#faf9fd'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f5f3f7'
  surface-container: '#efedf1'
  surface-container-high: '#e9e7ec'
  surface-container-highest: '#e3e2e6'
  on-surface: '#1b1b1f'
  on-surface-variant: '#454935'
  inverse-surface: '#2f3034'
  inverse-on-surface: '#f2f0f4'
  outline: '#767963'
  outline-variant: '#c6c9af'
  surface-tint: '#556500'
  primary: '#556500'
  on-primary: '#ffffff'
  primary-container: '#d4f34a'
  on-primary-container: '#5c6d00'
  inverse-primary: '#b6d32a'
  secondary: '#6339db'
  on-secondary: '#ffffff'
  secondary-container: '#7d56f5'
  on-secondary-container: '#fffbff'
  tertiary: '#b71945'
  on-tertiary: '#ffffff'
  tertiary-container: '#ffdddf'
  on-tertiary-container: '#c2234d'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d1f047'
  primary-fixed-dim: '#b6d32a'
  on-primary-fixed: '#181e00'
  on-primary-fixed-variant: '#3f4c00'
  secondary-fixed: '#e7deff'
  secondary-fixed-dim: '#ccbdff'
  on-secondary-fixed: '#1f005f'
  on-secondary-fixed-variant: '#4d17c5'
  tertiary-fixed: '#ffd9dc'
  tertiary-fixed-dim: '#ffb2ba'
  on-tertiary-fixed: '#400011'
  on-tertiary-fixed-variant: '#910031'
  background: '#faf9fd'
  on-background: '#1b1b1f'
  surface-variant: '#e3e2e6'
typography:
  headline-xl:
    fontFamily: Space Grotesk
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 52px
  headline-xl-mobile:
    fontFamily: Space Grotesk
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 36px
  headline-lg:
    fontFamily: Space Grotesk
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 38px
  headline-lg-mobile:
    fontFamily: Space Grotesk
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 30px
  headline-md:
    fontFamily: Space Grotesk
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '500'
    lineHeight: 26px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 15px
    fontWeight: '500'
    lineHeight: 22px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 18px
  label-lg:
    fontFamily: JetBrains Mono
    fontSize: 14px
    fontWeight: '700'
    lineHeight: 18px
  label-md:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
  label-sm:
    fontFamily: JetBrains Mono
    fontSize: 10px
    fontWeight: '700'
    lineHeight: 14px
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-desktop: 1.5rem
  margin: 1rem
  margin-tablet: 1.5rem
  margin-desktop: 2.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2.25rem
---

## Brand & Style
This design system embodies an irreverent, hyper-tactile, Neo-Brutalist visual language tailored specifically for university life. It merges raw structural brutality—unapologetic hard-edge drop shadows, thick ink strokes, and high-contrast boundaries—with an energetic Gen-Z palette of electric matcha, soft lavender, and punchy coral set against warm, tactile newsprint cream.

The emotional tone balances academic accountability with mischievous student culture: candid, smart, energetic, and witty. Microcopy should reject dry administrative terminology (e.g., replace "Attendance Deficit" with "Danger Zone: 1 Bunk Left"). Surfaces must feel punchy and pressable, turning a routine attendance-tracking routine into an engaging, game-like dashboard.

## Colors
The palette leverages chromatic clash and sharp value contrasts to create immediate visual hierarchy.

- **Primary (`#D4F34A`) — Electric Matcha:** The high-energy anchor. Used for primary calls-to-action, safe attendance states ("Safe to Bunk"), top-tier metrics, and active selected indicators.
- **Secondary (`#7048E8`) — Periwinkle Punch:** Used for key operational elements, subject categorizations, timetable markers, and contextual headers. Tinted lavender (`#C8B6FF`) functions as its supporting surface container.
- **Tertiary (`#FF5376`) — Hot Coral:** The critical warning beacon. Reserved for attendance alerts, threshold warnings ("Low Attendance"), destructive actions, and urgent reminders.
- **Neutral (`#121316`) — Ink Charcoal:** The fundamental structural anchor. Used for thick outlines, solid directional drop shadows, icon lines, and high-legibility display type.
- **Canvas (`#FBF9F1`) — Warm Newsprint Cream:** The foundational background surface across all primary viewports, providing a soft paper-like grain feel that prevents vibrant neons from overwhelming user focus.

## Typography
The typographic hierarchy balances expressive editorial flair with technical precision:

- **Display & Headlines (Space Grotesk):** Geometric, angular, and punchy. Drives personality in titles, primary attendance percentages, and critical hero metrics.
- **Body Text (Plus Jakarta Sans):** Balanced, modern, and highly legible. Mitigates the geometric rigidity of the headlines and handles all continuous reading, course descriptions, and microcopy.
- **Technical & Utility Labels (JetBrains Mono):** Monospaced functional tracking. Used strictly for status counters, ratio metrics (e.g., `24/30 Held`), timestamps, timetable rooms, and metric badges.

## Layout & Spacing
The layout follows a fluid-grid system anchored by strict structural alignment:

- **Mobile (< 768px):** 4-column layout with `1rem` margins and `1rem` gutters. Attendance metrics stack vertically; navigation relies on a chunky bottom dock.
- **Tablet (768px - 1023px):** 8-column layout with `1.5rem` outer margins and `1rem` gutters. Course cards reflow into a 2-column modular grid.
- **Desktop (≥ 1024px):** 12-column layout with `2.5rem` margins and `1.5rem` gutters. Side-by-side arrangement pairing global attendance analytics with class schedule feeds.

Vertical rhythm relies strictly on the `space-*` scale. Spacing within tactile containers is intentionally generous (`space-md` to `space-lg`) to offset the visual weight of thick outlines.

## Elevation & Depth
Depth is rendered exclusively through hard, un-blurred directional shadows (hard-edge neo-brutalist elevation) rather than blurred, ambient drop shadows:

- **Default Surface Elevation:** `box-shadow: 3px 3px 0px #121316`. Applied to standard attendance cards, input containers, and neutral panels.
- **Interactive Element Elevation:** `box-shadow: 4px 4px 0px #121316`. Used for pill buttons, interactive cards, and high-priority widgets.
- **Active / Pressed State:** All hard shadows collapse to `0px 0px 0px #121316`, translating the element `+3px` along both X and Y axes (`transform: translate(3px, 3px)`). This creates an immediate, physical stamp-down effect.
- **Floating Modals / Floating Sheets:** `box-shadow: 6px 6px 0px #121316` paired with a solid 2px outline.
- **Outlines:** All elevated containers must feature an explicit `2px solid #121316` stroke around their perimeter.

## Shapes
This system utilizes high-curvature pill forms contrasted against crisp, geometric structural grids. 

- **Level 3 (Pill-shaped):** Interactive controls, badges, filter chips, and primary buttons use fully rounded pill silhouettes (`roundedness: 3`).
- **Surface Cards & Modals:** Large surface containers use `rounded-xl` (1.5rem to 2rem) corner geometry paired with the mandatory `2px solid #121316` ink outline.
- **Badges & Micro-tags:** Always rendered as full capsules (`border-radius: 9999px`) with `1.5px solid #121316` outlines.

## Components

### Buttons
- **Primary Pill:** Background in `#D4F34A`, text in `#121316`, `2px solid #121316` border, `box-shadow: 3px 3px 0px #121316`, fully rounded (`border-radius: 9999px`). Font: Space Grotesk Bold. Hover adds a slight tilt (`transform: rotate(-1deg)`), active state shifts `translate(3px, 3px)` with shadow cleared.
- **Secondary Pill:** Background in `#C8B6FF`, text in `#121316`, `2px solid #121316`, `3px 3px 0px #121316` drop shadow.
- **Danger / Bunk Pill:** Background in `#FF5376`, text in `#FBF9F1` or `#121316`, matching brutalist shadow and border.

### Attendance Cards
- Container built on `#FFFFFF` with `2px solid #121316` border, `rounded-xl` corners, and `4px 4px 0px #121316` shadow.
- Upper region displays course code in JetBrains Mono tag, large-scale attendance percentage in Space Grotesk Bold, and a contextual badge ("Safe Zone" in `#D4F34A`, "Bunk Risk" in `#FF5376`).
- Bottom row features dual rapid-action buttons: "+ Attended" and "+ Bunked".

### Chips & Badges
- Pill-shaped status indicators with a `1.5px solid #121316` border.
- Text rendered in JetBrains Mono, uppercase, letter-spaced +0.05em.
- Color mappings: Safe (`#D4F34A`), Danger (`#FF5376`), Info (`#C8B6FF`).

### Input Fields
- Background in `#FFFFFF` with `2px solid #121316` stroke, `rounded-lg` (0.75rem to 1rem) or full pill.
- Placeholder text in `#121316` at 50% opacity.
- Focused state applies `box-shadow: 3px 3px 0px #7048E8` with the border remaining solid `#121316`.

### Checkboxes & Toggle Switches
- **Checkboxes:** Square with `rounded-sm` (4px), `2px solid #121316` border, and `2px 2px 0px #121316` shadow. Checked state fills `#D4F34A` with a heavy black vector tick.
- **Toggles:** Pill-shaped track with a `2px solid #121316` border; thumb is a solid `#121316` circle that slides between states with an audible spring transition.

### Custom Component: The "Bunk-O-Meter"
- A segmented progress bar wrapped in a `2px solid #121316` shell with a pill border-radius.
- Segments transition from `#D4F34A` (above 75%) to `#FF5376` (below threshold), separated by thick vertical black divider ticks.