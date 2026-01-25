// Inline SVG icons. No icon-font or image dependency, so they port to any renderer.
const svgProps = {
  width: 24,
  height: 24,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 2,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
  focusable: false,
};

export function IconBack() {
  return (
    <svg {...svgProps}>
      <polyline points="15 18 9 12 15 6" />
    </svg>
  );
}

export function IconPin() {
  return (
    <svg {...svgProps} width={28} height={28}>
      <path d="M12 21s-7-6.2-7-11a7 7 0 1 1 14 0c0 4.8-7 11-7 11z" />
      <circle cx="12" cy="10" r="2.5" />
    </svg>
  );
}

export function IconSearch() {
  return (
    <svg {...svgProps} width={28} height={28}>
      <circle cx="11" cy="11" r="6.5" />
      <line x1="16" y1="16" x2="21" y2="21" />
    </svg>
  );
}

export function IconHome() {
  return (
    <svg {...svgProps}>
      <path d="M3 10.5 12 3l9 7.5" />
      <path d="M5 9.5V20h14V9.5" />
      <path d="M10 20v-5h4v5" />
    </svg>
  );
}

export function IconMap() {
  return (
    <svg {...svgProps}>
      <polygon points="9 4 3 6 3 20 9 18 15 20 21 18 21 4 15 6 9 4" />
      <line x1="9" y1="4" x2="9" y2="18" />
      <line x1="15" y1="6" x2="15" y2="20" />
    </svg>
  );
}

export function IconMessage() {
  return (
    <svg {...svgProps}>
      <path d="M21 12a8 8 0 0 1-11.6 7.1L4 20.5l1.4-4.3A8 8 0 1 1 21 12z" />
    </svg>
  );
}

export function IconUser() {
  return (
    <svg {...svgProps}>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21a8 8 0 0 1 16 0" />
    </svg>
  );
}
