package net.nicomar.nomoregrids.world;

/** Pure-index rejection. Platform validation adds reach, protection, support and inventory checks. */
public final class IndexException extends IllegalArgumentException {
    public enum Code { INVALID_DATA, STALE_REVISION, OVERLAP, COMPLEXITY_LIMIT }
    private final Code code;

    public IndexException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code code() { return code; }
}
