import * as React from "react";

export interface SosButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  /** Big label in the middle. Defaults to "SOS". */
  label?: string;
  /** Small helper line at the bottom edge. */
  hint?: string;
  /** Optional icon rendered before the label (defaults to a warning triangle). */
  icon?: React.ReactNode;
}

const WarningIcon = (
  <svg className="cc-sos__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
    <line x1="12" y1="9" x2="12" y2="13" />
    <line x1="12" y1="17" x2="12.01" y2="17" />
  </svg>
);

/**
 * Full-width emergency SOS button from the elder home screen — red gradient,
 * oversized type, press-and-hold hint pinned to the bottom edge.
 */
export function SosButton({
  label = "SOS",
  hint = "Press and hold for emergency",
  icon = WarningIcon,
  className,
  ...rest
}: SosButtonProps) {
  return (
    <button type="button" className={["cc-sos", className ?? ""].filter(Boolean).join(" ")} {...rest}>
      <span className="cc-sos__main">
        {icon}
        {label}
      </span>
      <span className="cc-sos__hint">{hint}</span>
    </button>
  );
}
