package net.nicomar.nomoregrids.core;

import java.util.Objects;
import java.util.regex.Pattern;

/** Platform-independent textual registry ID, never an external enum ordinal. */
public record ResourceId(String value) {
    private static final Pattern VALID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

    public ResourceId {
        Objects.requireNonNull(value);
        if (value.length() > 256 || !VALID.matcher(value).matches()) throw new IllegalArgumentException("Invalid resource ID");
    }
}
