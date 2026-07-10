import * as React from "react";

export interface ActionCardProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  /** Icon (SVG or emoji) shown inside the pastel tile. */
  icon: React.ReactNode;
  /** Big label under the tile — elder-sized 22px type. */
  label: string;
  /** Pastel accent of the icon tile. */
  tone?: "blue" | "purple" | "green" | "amber" | "red" | "teal" | "neutral";
  /** Optional small hint line, e.g. "Tap to open". */
  hint?: string;
}

/**
 * Large tappable navigation card from the elder flow — white card, pastel
 * icon tile, oversized label. Used for Contacts, Entertainment, footer nav.
 */
export function ActionCard({ icon, label, tone = "blue", hint, className, ...rest }: ActionCardProps) {
  return (
    <button
      type="button"
      className={["cc-action-card", `cc-action-card--tone-${tone}`, className ?? ""].filter(Boolean).join(" ")}
      {...rest}
    >
      <span className="cc-action-card__tile">{icon}</span>
      <span className="cc-action-card__label">{label}</span>
      {hint ? <span className="cc-action-card__hint">{hint}</span> : null}
    </button>
  );
}
