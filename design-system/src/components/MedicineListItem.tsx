import * as React from "react";

export interface MedicineListItemProps extends React.HTMLAttributes<HTMLDivElement> {
  /** Medicine name, optionally numbered by the caller ("1. Lorazepam 3mg"). */
  name: string;
  /** Quantity label, e.g. "Qty 1". */
  qty?: string;
  /** Icon or emoji inside the pastel tile. */
  icon?: React.ReactNode;
  /** Pastel accent of the icon tile. */
  tone?: "amber" | "blue" | "purple" | "green";
}

/**
 * Row from the elder "Today's medicines" list — pastel pill icon tile,
 * medicine name, and quantity on the right.
 */
export function MedicineListItem({ name, qty, icon = "💊", tone = "amber", className, ...rest }: MedicineListItemProps) {
  return (
    <div
      className={["cc-med-item", `cc-med-item--tone-${tone}`, className ?? ""].filter(Boolean).join(" ")}
      {...rest}
    >
      <span className="cc-med-item__tile">{icon}</span>
      <span className="cc-med-item__name">{name}</span>
      {qty ? <span className="cc-med-item__qty">{qty}</span> : null}
    </div>
  );
}
