import * as React from "react";

export interface GradientPageHeaderProps extends React.HTMLAttributes<HTMLDivElement> {
  title: string;
  subtitle?: string;
  /** Gradient family per guardian section. */
  tone?: "primary" | "medicines" | "schedule" | "sos";
  /** Back-arrow handler; the round translucent back button is always shown. */
  onBack?: () => void;
  /** Trailing action buttons (icon buttons) on the right edge. */
  actions?: React.ReactNode;
}

const BackIcon = (
  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M19 12H5" />
    <path d="m12 19-7-7 7-7" />
  </svg>
);

/**
 * Green-gradient page header used across guardian screens: round translucent
 * back button, bold white title, optional subtitle and trailing actions.
 */
export function GradientPageHeader({
  title,
  subtitle,
  tone = "primary",
  onBack,
  actions,
  className,
  ...rest
}: GradientPageHeaderProps) {
  return (
    <div
      className={["cc-page-header", tone !== "primary" ? `cc-page-header--${tone}` : "", className ?? ""]
        .filter(Boolean)
        .join(" ")}
      {...rest}
    >
      <button type="button" className="cc-page-header__back" onClick={onBack} aria-label="Back">
        {BackIcon}
      </button>
      <div className="cc-page-header__titles">
        <p className="cc-page-header__title">{title}</p>
        {subtitle ? <p className="cc-page-header__subtitle">{subtitle}</p> : null}
      </div>
      {actions ? <div className="cc-page-header__actions">{actions}</div> : null}
    </div>
  );
}
