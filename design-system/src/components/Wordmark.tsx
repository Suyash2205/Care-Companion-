import * as React from "react";

export interface WordmarkProps extends React.HTMLAttributes<HTMLHeadingElement> {
  /** Override the two-line brand text (defaults to "Care\nCompanion"). */
  children?: React.ReactNode;
}

/** The "Care Companion" brand wordmark — extra-light serif display type on two lines. */
export function Wordmark({ className, children, ...rest }: WordmarkProps) {
  return (
    <h1 className={["cc-wordmark", className ?? ""].filter(Boolean).join(" ")} {...rest}>
      {children ?? "Care\nCompanion"}
    </h1>
  );
}
