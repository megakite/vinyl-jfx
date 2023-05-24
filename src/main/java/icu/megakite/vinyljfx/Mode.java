package icu.megakite.vinyljfx;

public enum Mode {
    DEFAULT,
    REPEAT_ALL,
    REPEAT_ONE,
    SHUFFLE;

    @Override
    public String toString() {
        switch (this) {
        case DEFAULT:
            return "DEFAULT";
        case SHUFFLE:
            return "SHUFFLE";
        case REPEAT_ALL:
            return "REPEAT_ALL";
        case REPEAT_ONE:
            return "REPEAT_ONE";
        default:
            throw new IllegalStateException();
        }
    }
}
