package org.openpmca.framework;

/**
 * An immutable observation result of a specific capability, provider, or runtime data field.
 *
 * @param <T> the type of data observed
 */
public final class Probe<T> {
    private final Availability availability;
    private final T value;
    private final String reason;

    private Probe(Availability availability, T value, String reason) {
        if (availability == null) {
            throw new IllegalArgumentException("availability must not be null");
        }
        if (availability == Availability.AVAILABLE && value == null) {
            throw new IllegalArgumentException("available probe must have a non-null value");
        }
        this.availability = availability;
        this.value = (availability == Availability.AVAILABLE) ? value : null;
        this.reason = reason;
    }

    /**
     * Creates an AVAILABLE probe carrying an observed non-null value and a diagnostic reason.
     */
    static <T> Probe<T> available(T value, String reason) {
        if (value == null) {
            return unknown("NO_VALUE");
        }
        return new Probe<T>(Availability.AVAILABLE, value, reason);
    }

    /**
     * Creates an AVAILABLE probe with default VALUE_READ reason.
     */
    static <T> Probe<T> available(T value) {
        return available(value, "VALUE_READ");
    }

    /**
     * Creates an UNAVAILABLE probe indicating the symbol or feature is absent.
     */
    static <T> Probe<T> unavailable(String reason) {
        return new Probe<T>(Availability.UNAVAILABLE, null, reason);
    }

    /**
     * Creates an UNKNOWN probe indicating the read failed, was blocked, or produced empty data.
     */
    static <T> Probe<T> unknown(String reason) {
        return new Probe<T>(Availability.UNKNOWN, null, reason);
    }

    public Availability getAvailability() {
        return availability;
    }

    public T getValue() {
        return value;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Probe)) return false;
        Probe<?> other = (Probe<?>) obj;
        if (this.availability != other.availability) return false;
        if (this.value != null ? !this.value.equals(other.value) : other.value != null) return false;
        return this.reason != null ? this.reason.equals(other.reason) : other.reason == null;
    }

    @Override
    public int hashCode() {
        int result = availability.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (reason != null ? reason.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        if (availability == Availability.AVAILABLE) {
            return "Probe[AVAILABLE, value=" + value + ", reason=" + reason + "]";
        }
        return "Probe[" + availability + ", reason=" + reason + "]";
    }
}
