import * as React from "react";

export interface ProfileChoiceCardProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  /** Profile name under the avatar. */
  name: string;
  /** Selected state — green border, mint fill, green underline bar. */
  selected?: boolean;
  /** Avatar content (icon/emoji); a photo URL wins if provided. */
  icon?: React.ReactNode;
  photoSrc?: string;
}

/**
 * Selectable elder-profile card from the guardian "Select Profile" screen —
 * round avatar, name, and a small underline bar that turns green when selected.
 */
export function ProfileChoiceCard({ name, selected = false, icon, photoSrc, className, ...rest }: ProfileChoiceCardProps) {
  return (
    <button
      type="button"
      className={["cc-profile-choice", selected ? "cc-profile-choice--selected" : "", className ?? ""]
        .filter(Boolean)
        .join(" ")}
      {...rest}
    >
      <span className="cc-profile-choice__avatar">
        {photoSrc ? <img src={photoSrc} alt={name} /> : icon ?? name.charAt(0)}
      </span>
      <span className="cc-profile-choice__name">{name}</span>
      <span className="cc-profile-choice__bar" />
    </button>
  );
}
