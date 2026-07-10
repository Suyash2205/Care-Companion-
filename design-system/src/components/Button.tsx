import * as React from "react";

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  /** Visual style. `primary` is the green gradient used for main guardian actions. */
  variant?: "primary" | "solid" | "danger" | "info" | "outline" | "ghost";
  /** `md` for guardian screens, `lg`/`xl` for elder-facing oversized buttons. */
  size?: "md" | "lg" | "xl";
  /** Stretch to the container width (the app's primary buttons are full-width). */
  fullWidth?: boolean;
  children?: React.ReactNode;
}

/**
 * CareCompanion button. Guardian flows use the full-width green-gradient
 * `primary` variant; elder flows use `lg`/`xl` sizes for readability.
 * `danger` carries the red SOS gradient, `info` the blue "Start taking" action.
 */
export function Button({
  variant = "primary",
  size = "md",
  fullWidth = false,
  className,
  children,
  ...rest
}: ButtonProps) {
  const classes = [
    "cc-btn",
    variant !== "primary" ? `cc-btn--${variant}` : "",
    size !== "md" ? `cc-btn--${size}` : "",
    fullWidth ? "cc-btn--full" : "",
    className ?? "",
  ]
    .filter(Boolean)
    .join(" ");
  return (
    <button type="button" className={classes} {...rest}>
      {children}
    </button>
  );
}
