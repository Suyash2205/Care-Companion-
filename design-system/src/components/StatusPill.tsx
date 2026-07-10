import * as React from "react";

export interface StatusPillProps extends React.HTMLAttributes<HTMLSpanElement> {
  /**
   * `status` is the large "At home" pill under the elder avatar; `badge` is the
   * compact solid-green SAFE tag; `alert` is its red counterpart.
   */
  variant?: "status" | "badge" | "alert";
  /** Compact sizing for inline use inside cards. */
  small?: boolean;
  children?: React.ReactNode;
}

/** Rounded status pill with a leading dot — "At home", "SAFE", or an alert state. */
export function StatusPill({ variant = "status", small = false, className, children, ...rest }: StatusPillProps) {
  const classes = [
    "cc-status-pill",
    variant === "badge" ? "cc-status-pill--badge" : "",
    variant === "alert" ? "cc-status-pill--alert" : "",
    small || variant === "badge" ? "cc-status-pill--sm" : "",
    className ?? "",
  ]
    .filter(Boolean)
    .join(" ");
  return (
    <span className={classes} {...rest}>
      <span className="cc-status-pill__dot" />
      {children}
    </span>
  );
}
