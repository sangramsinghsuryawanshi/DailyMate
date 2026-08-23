/**
 * DailyMate Design System Tokens
 * Source of truth mapped to CSS Custom Properties (--dm-*).
 */

export const tokens = {
  colors: {
    // Brand & Primary
    primary: 'var(--dm-color-primary, #1f8f74)',
    primaryHover: 'var(--dm-color-primary-hover, #17735d)',
    primaryDeep: 'var(--dm-color-primary-deep, #126a56)',
    primarySubtle: 'var(--dm-color-primary-subtle, #eaf6f2)',

    // Secondary & Accent
    secondary: 'var(--dm-color-secondary, #eaf6f2)',
    accent: 'var(--dm-color-accent, #f6b84b)',
    accentHover: 'var(--dm-color-accent-hover, #e0a338)',

    // Background & Surfaces
    background: 'var(--dm-color-background, #f8fafc)',
    backgroundElevated: 'var(--dm-color-background-elevated, #edf5f1)',
    surface: 'var(--dm-color-surface, #ffffff)',
    surfaceSoft: 'var(--dm-color-surface-soft, #f8fafc)',
    surfaceHover: 'var(--dm-color-surface-hover, #f1f5f9)',

    // Text & Content
    text: 'var(--dm-color-text, #0f172a)',
    textMuted: 'var(--dm-color-text-muted, #475569)',
    textSoft: 'var(--dm-color-text-soft, #64748b)',
    textInverse: 'var(--dm-color-text-inverse, #ffffff)',

    // Borders & Dividers
    border: 'var(--dm-color-border, #e2e8f0)',
    borderHover: 'var(--dm-color-border-hover, #cbd5e1)',
    borderFocus: 'var(--dm-color-border-focus, #1f8f74)',

    // Semantic States
    success: 'var(--dm-color-success, #10b981)',
    successSubtle: 'var(--dm-color-success-subtle, #ecfdf5)',
    warning: 'var(--dm-color-warning, #f59e0b)',
    warningSubtle: 'var(--dm-color-warning-subtle, #fffbeb)',
    danger: 'var(--dm-color-danger, #ef4444)',
    dangerSubtle: 'var(--dm-color-danger-subtle, #fef2f2)',
    info: 'var(--dm-color-info, #3b82f6)',
    infoSubtle: 'var(--dm-color-info-subtle, #eff6ff)',

    // Domain Accent Palette
    domainExpense: 'var(--dm-domain-expense, #10b981)',
    domainHealth: 'var(--dm-domain-health, #06b6d4)',
    domainEmergency: 'var(--dm-domain-emergency, #ef4444)',
    domainCommunity: 'var(--dm-domain-community, #6366f1)',
    domainMarketplace: 'var(--dm-domain-marketplace, #f59e0b)',
    domainAI: 'var(--dm-domain-ai, #8b5cf6)',
  },

  typography: {
    fontFamily: {
      sans: 'Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
      mono: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
      tabular: 'Inter, "SF Pro", -apple-system, sans-serif',
    },
    fontSize: {
      xs: '0.75rem',    // 12px
      sm: '0.875rem',   // 14px
      base: '1rem',      // 16px
      lg: '1.125rem',   // 18px
      xl: '1.25rem',    // 20px
      '2xl': '1.5rem',  // 24px
      '3xl': '1.875rem',// 30px
      '4xl': '2.25rem', // 36px
    },
    fontWeight: {
      normal: '400',
      medium: '500',
      semibold: '600',
      bold: '700',
      extrabold: '800',
    },
    lineHeight: {
      tight: '1.25',
      normal: '1.5',
      relaxed: '1.625',
    },
  },

  spacing: {
    1: '0.25rem', // 4px
    2: '0.5rem',  // 8px
    3: '0.75rem', // 12px
    4: '1rem',    // 16px
    5: '1.25rem', // 20px
    6: '1.5rem',  // 24px
    8: '2rem',    // 32px
    10: '2.5rem', // 40px
    12: '3rem',   // 48px
    16: '4rem',   // 64px
  },

  radius: {
    none: '0',
    xs: '4px',
    sm: '8px',
    md: '12px',
    lg: '16px',
    xl: '24px',
    pill: '9999px',
  },

  shadows: {
    none: 'none',
    sm: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
    md: '0 4px 6px -1px rgba(0, 0, 0, 0.08), 0 2px 4px -2px rgba(0, 0, 0, 0.04)',
    lg: '0 10px 15px -3px rgba(0, 0, 0, 0.08), 0 4px 6px -4px rgba(0, 0, 0, 0.03)',
    xl: '0 20px 25px -5px rgba(0, 0, 0, 0.08), 0 8px 10px -6px rgba(0, 0, 0, 0.03)',
    focus: '0 0 0 3px rgba(31, 143, 116, 0.25)',
  },

  breakpoints: {
    mobile: '320px',
    mobileLarge: '430px',
    tablet: '768px',
    desktop: '1024px',
    wide: '1280px',
    ultrawide: '1440px',
  },

  containers: {
    form: '540px',
    content: '780px',
    dashboard: '1200px',
    wide: '1400px',
  },

  motion: {
    fast: '150ms cubic-bezier(0.4, 0, 0.2, 1)',
    normal: '250ms cubic-bezier(0.4, 0, 0.2, 1)',
    slow: '350ms cubic-bezier(0.4, 0, 0.2, 1)',
  },
}

export default tokens
