import * as React from "react";

export interface TextFieldProps extends React.InputHTMLAttributes<HTMLInputElement> {
  /** Floating label rendered on the field's top border. */
  label: string;
  /** Optional leading icon inside the field. */
  leadingIcon?: React.ReactNode;
}

/**
 * Outlined input from guardian forms — white fill, 14px radius, label on the
 * border, green focus ring, optional leading icon.
 */
export function TextField({ label, leadingIcon, className, ...rest }: TextFieldProps) {
  return (
    <label
      className={["cc-field", leadingIcon ? "cc-field--with-icon" : "", className ?? ""]
        .filter(Boolean)
        .join(" ")}
    >
      <input className="cc-field__input" {...rest} />
      <span className="cc-field__label">{label}</span>
      {leadingIcon ? <span className="cc-field__icon">{leadingIcon}</span> : null}
    </label>
  );
}
