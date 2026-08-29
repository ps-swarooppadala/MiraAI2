---
name: Mira
colors:
  surface: '#f1fcf7'
  surface-dim: '#d1ddd8'
  surface-bright: '#f1fcf7'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#ebf6f1'
  surface-container: '#e5f0eb'
  surface-container-high: '#dfebe6'
  surface-container-highest: '#dae5e0'
  on-surface: '#141e1b'
  on-surface-variant: '#414845'
  inverse-surface: '#28332f'
  inverse-on-surface: '#e8f3ee'
  outline: '#717974'
  outline-variant: '#c0c8c3'
  surface-tint: '#3c6657'
  primary: '#00241a'
  on-primary: '#ffffff'
  primary-container: '#0e3b2e'
  on-primary-container: '#7aa694'
  inverse-primary: '#a3d0be'
  secondary: '#5e5e5c'
  on-secondary: '#ffffff'
  secondary-container: '#e1dfdc'
  on-secondary-container: '#636360'
  tertiary: '#735c00'
  on-tertiary: '#ffffff'
  tertiary-container: '#cba72f'
  on-tertiary-container: '#4e3d00'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#bfecd9'
  primary-fixed-dim: '#a3d0be'
  on-primary-fixed: '#002117'
  on-primary-fixed-variant: '#244e40'
  secondary-fixed: '#e4e2de'
  secondary-fixed-dim: '#c8c6c3'
  on-secondary-fixed: '#1b1c1a'
  on-secondary-fixed-variant: '#474744'
  tertiary-fixed: '#ffe088'
  tertiary-fixed-dim: '#e9c349'
  on-tertiary-fixed: '#241a00'
  on-tertiary-fixed-variant: '#574500'
  background: '#f1fcf7'
  on-background: '#141e1b'
  surface-variant: '#dae5e0'
typography:
  hero:
    fontFamily: Playfair Display
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Playfair Display
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Playfair Display
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
  headline-md:
    fontFamily: Playfair Display
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  body-lg:
    fontFamily: DM Sans
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: DM Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-md:
    fontFamily: DM Sans
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.05em
  label-sm:
    fontFamily: DM Sans
    fontSize: 12px
    fontWeight: '700'
    lineHeight: 16px
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  unit: 8px
  container-padding: 24px
  gutter: 16px
  stack-sm: 12px
  stack-md: 24px
  stack-lg: 40px
---

## Brand & Style

The design system is anchored in "Quiet Confidence." It serves as a premium wellness sanctuary that feels personal, on-device, and deeply thoughtful. The aesthetic merges **Modern Minimalism** with **Tactile Softness**, avoiding the sterile nature of traditional AI interfaces in favor of a warm, human-centric environment.

The target audience seeks mental clarity and premium coaching. The UI should evoke an emotional response of immediate calm, safety, and sophisticated guidance. This is achieved through generous negative space, a restricted and naturalistic color palette, and a focus on high-quality typography that feels both editorial and functional.

## Colors

The palette is inspired by nature and high-end physical stationery. 

- **Primary (#0E3B2E):** Used for primary actions, heavy headings, and grounding the brand. It represents the "Forest" and stability.
- **Background (#FDFBF7):** A warm off-white that reduces eye strain and feels more premium than pure white.
- **Accent (#D4AF37):** Used sparingly for "Hero" moments, progress indicators, or premium feature highlights.
- **Surface:** For light mode, surfaces are pure white with soft shadows to differentiate from the cream background.
- **Dark Mode (Secondary):** When transitioning to dark mode, use **#1A2421** as the base background to maintain the deep green undertone rather than a neutral gray.

## Typography

This system uses a high-contrast pairing to balance authority with utility.

- **Playfair Display:** Used for "Hero" moments, quotes from the AI coach, and primary screen headings. It provides a literary, sophisticated feel.
- **DM Sans:** Used for all functional UI elements, labels, and long-form body text. It is geometric and clean, ensuring the app feels modern and high-tech despite its classic color palette.
- **Hierarchy:** Maintain large margins around text blocks. Avoid tight line heights; the "breathability" of the text is essential for a wellness context.

## Layout & Spacing

The layout philosophy follows a **Fluid Grid** with fixed maximum widths for desktop to maintain intimacy. 

- **Margins:** A standard 24px side margin is used on mobile to provide a "frame" for the content.
- **Rhythm:** Vertical spacing should be generous. Group related items with 12px or 16px, but separate major sections with at least 40px to prevent the UI from feeling cluttered.
- **Safe Areas:** Ensure content never touches the edges of the screen; the AI "Coach" interface should always feel centered and balanced.

## Elevation & Depth

This system utilizes **Tonal Layers** and **Ambient Shadows** to create a sense of soft depth.

- **Shadows:** Avoid pure black shadows. Use the Primary color (#0E3B2E) at 5-8% opacity with a large blur radius (20px to 40px) and a subtle Y-offset. This creates a "floating" effect rather than a "stuck on" effect.
- **Surfaces:** Use white (#FFFFFF) cards on the cream background (#FDFBF7) to create a subtle hierarchy without needing borders.
- **Transitions:** Depth should feel fluid. When an element is focused, its shadow should slightly expand, simulating the element lifting off the surface toward the user.

## Shapes

The shape language is defined by **Extreme Rounding**.

- **Containers:** All cards and primary containers use a 24px (`rounded-xl` equivalent) radius to evoke a sense of softness and approachability.
- **Interactive Elements:** Buttons and input fields should be pill-shaped to contrast against the more structured content cards.
- **Consistency:** Never use sharp corners. Even icons and progress bars should utilize rounded end-caps.

## Components

### Buttons
- **Primary:** Pill-shaped, Primary Green background, White text. High-elevation shadow on hover.
- **Secondary:** Pill-shaped, Soft Gold (#E6BE8A) background or outline, Primary Green text.
- **Ghost:** No background, Primary Green text with an underline or just the label font style. Use for low-priority actions.

### Cards
- White background, 24px corner radius, and a very soft ambient green-tinted shadow. No borders. Inner padding should be a minimum of 20px.

### Coach Speaking Indicator
- **Bubble:** A soft, off-white or cream bubble with a slight gradient.
- **Indicator:** Instead of a traditional "typing" dots, use a subtle, breathing pulse animation using the Soft Gold color. The indicator should be minimalist—a single glowing ring or a thin oscillating line.

### Input Fields
- Pill-shaped with a 1px border in a very light tint of the Primary color (#F2EEE6). On focus, the border transitions to Soft Gold.

### Chips & Tags
- Used for mood selection or topics. Small pill shapes with #F2EEE6 backgrounds and Primary Green text. Selected state uses Primary Green background with White text.

### Iconography
- All icons must be thin-line (1px or 1.5px stroke) with a "hand-drawn" slight imperfection or rounded terminals. Avoid filled icons unless used for a persistent active state in the bottom navigation.