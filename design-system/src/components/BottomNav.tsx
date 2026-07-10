import * as React from "react";

export interface BottomNavItem {
  /** Stable key, also the label under the icon. */
  label: string;
  /** Icon (SVG or emoji). */
  icon: React.ReactNode;
}

export interface BottomNavProps extends Omit<React.HTMLAttributes<HTMLElement>, "onSelect"> {
  /** Tabs in display order (the app uses Home / Alerts / Settings). */
  items: BottomNavItem[];
  /** Label of the active tab. */
  active?: string;
  /** Called with the tapped tab's label. */
  onSelect?: (label: string) => void;
}

/**
 * Guardian bottom navigation bar — white with a top shadow; the active tab
 * gets a rounded green tint and bold label.
 */
export function BottomNav({ items, active, onSelect, className, ...rest }: BottomNavProps) {
  return (
    <nav className={["cc-bottom-nav", className ?? ""].filter(Boolean).join(" ")} {...rest}>
      {items.map((item) => (
        <button
          key={item.label}
          type="button"
          className={[
            "cc-bottom-nav__item",
            item.label === active ? "cc-bottom-nav__item--active" : "",
          ]
            .filter(Boolean)
            .join(" ")}
          onClick={() => onSelect?.(item.label)}
        >
          <span className="cc-bottom-nav__icon">{item.icon}</span>
          {item.label}
        </button>
      ))}
    </nav>
  );
}
