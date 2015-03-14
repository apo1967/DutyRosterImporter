package dutyroster.importer.domain;

/**
 * Tags a shift from a docx duty roster. Shifts are
 * <ul>
 * <li>{@link #EARLY_SHIFT}</li>
 * <li>{@link #LATE_SHIFT}</li>
 * <li>{@link #NIGHT_SHIFT}</li>
 * </ul>
 *
 * @author apohl
 */
public enum Shift {

    /**
     * Early shift. Label: "FD" (german: Fruehdienst).
     */
    EARLY_SHIFT("FD"),

    /**
     * Late shift. Lable: "SD" (german: Spaetdienst).
     */
    LATE_SHIFT("SD"),

    /**
     * Night shift. Lable: "ND" (german: Nachtdienst).
     */
    NIGHT_SHIFT("ND");

    private String label;

    private Shift(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Shift parseLabel(String label) {
        for (Shift shift : Shift.values()) {
            if (shift.getLabel().equals(label)) {
                return shift;
            }
        }
        throw new IllegalArgumentException("no such shift label [" + label + "]");
    }
}
