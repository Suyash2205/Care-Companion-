import * as React from "react";

export interface ChipProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  /** `neutral` for info chips (qty, timing); `success`/`danger` for Taken / Not taken toggles. */
  tone?: "neutral" | "success" | "danger";
  /** Filled state for toggle chips. */
  selected?: boolean;
  children?: React.ReactNode;
}

/**
 * Assist / toggle chip from the elder medicine flow — "Qty 1", "After Lunch",
 * and the Taken / Not taken confirmation pair (`tone` + `selected`).
 */
export function Chip({ tone = "neutral", selected = false, className, children, ...rest }: ChipProps) {
  const classes = [
    "cc-chip",
    tone !== "neutral" ? `cc-chip--${tone}` : "",
    tone !== "neutral" ? "cc-chip--selectable" : "",
    selected ? "cc-chip--selected" : "",
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
