import * as React from "react";

export interface SectionHeaderProps extends React.HTMLAttributes<HTMLDivElement> {
  children?: React.ReactNode;
}

/** Small uppercase section label used between groups on guardian screens. */
export function SectionHeader({ className, children, ...rest }: SectionHeaderProps) {
  return (
    <div className={["cc-section-header", className ?? ""].filter(Boolean).join(" ")} {...rest}>
      {children}
    </div>
  );
}
