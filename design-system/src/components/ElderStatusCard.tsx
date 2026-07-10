import * as React from "react";
import { StatusPill } from "./StatusPill";

export interface ElderStatusCardProps extends React.HTMLAttributes<HTMLDivElement> {
  /** Elder's display name. */
  name: string;
  /** Status line, e.g. "At Home | 15 mins ago". */
  status?: string;
  /** Shows the solid-green SAFE badge on the right. */
  safe?: boolean;
  /** Photo URL; falls back to an initial letter on the translucent circle. */
  photoSrc?: string;
}

/**
 * Guardian dashboard hero card — green gradient with the elder's avatar,
 * name, last-seen status, and a SAFE badge.
 */
export function ElderStatusCard({
  name,
  status = "At Home | 15 mins ago",
  safe = true,
  photoSrc,
  className,
  ...rest
}: ElderStatusCardProps) {
  return (
    <div className={["cc-elder-status", className ?? ""].filter(Boolean).join(" ")} {...rest}>
      <span className="cc-elder-status__avatar">
        {photoSrc ? <img src={photoSrc} alt={name} /> : name.charAt(0)}
      </span>
      <div className="cc-elder-status__body">
        <p className="cc-elder-status__name">{name}</p>
        <p className="cc-elder-status__meta">{status}</p>
      </div>
      {safe ? <StatusPill variant="badge">SAFE</StatusPill> : <StatusPill variant="alert" small>ALERT</StatusPill>}
    </div>
  );
}
