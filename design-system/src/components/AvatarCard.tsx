import * as React from "react";
import { StatusPill } from "./StatusPill";

export interface AvatarCardProps extends React.HTMLAttributes<HTMLDivElement> {
  /** Elder's first name, rendered extra bold at 34px. */
  name: string;
  /** Status pill text under the name, e.g. "Status: At home". */
  status?: string;
  /** Photo URL for the oval avatar; falls back to a large initial on a blue radial gradient. */
  photoSrc?: string;
}

/**
 * The elder home screen's centerpiece: a large white card with an oval
 * blue-gradient avatar, the elder's name in oversized type, and a status pill.
 */
export function AvatarCard({ name, status = "Status: At home", photoSrc, className, ...rest }: AvatarCardProps) {
  return (
    <div className={["cc-avatar-card", className ?? ""].filter(Boolean).join(" ")} {...rest}>
      <span className="cc-avatar-card__oval">
        {photoSrc ? <img src={photoSrc} alt={name} /> : name.charAt(0)}
      </span>
      <p className="cc-avatar-card__name">{name}</p>
      <StatusPill>{status}</StatusPill>
    </div>
  );
}
