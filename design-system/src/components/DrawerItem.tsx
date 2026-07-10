import * as React from "react";

export interface DrawerItemProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  /** Icon inside the small rounded tile. */
  icon: React.ReactNode;
  /** Row label. */
  label: string;
  /** Highlights the row green (current destination). */
  selected?: boolean;
}

/**
 * Elder navigation drawer row — small rounded icon tile plus label; the
 * selected row is tinted CareGreen.
 */
export function DrawerItem({ icon, label, selected = false, className, ...rest }: DrawerItemProps) {
  return (
    <button
      type="button"
      className={["cc-drawer-item", selected ? "cc-drawer-item--selected" : "", className ?? ""]
        .filter(Boolean)
        .join(" ")}
      {...rest}
    >
      <span className="cc-drawer-item__tile">{icon}</span>
      {label}
    </button>
  );
}
