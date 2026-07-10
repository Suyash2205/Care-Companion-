import * as React from "react";

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  children?: React.ReactNode;
}

/** White rounded surface card (20px radius, soft shadow) — the app's default container. */
export function Card({ className, children, ...rest }: CardProps) {
  return (
    <div className={["cc-card", className ?? ""].filter(Boolean).join(" ")} {...rest}>
      {children}
    </div>
  );
}
