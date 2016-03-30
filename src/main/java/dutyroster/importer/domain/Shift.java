package dutyroster.importer.domain;

import org.apache.commons.lang3.StringUtils;

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
    EARLY_SHIFT("FD", "TD"),

    /**
     * Late shift. Lable: "SD" (german: Spaetdienst).
     */
    LATE_SHIFT("SD", null),

    /**
     * Night shift. Lable: "ND" (german: Nachtdienst).
     */
    NIGHT_SHIFT("ND", null);

    private String label;

    private String altLabel;

    private Shift(String label, String altLabel) {
        this.label = label;
        this.altLabel = altLabel;
    }

    public String getLabel() {
        return label;
    }

    public String getAltLabel() {
        return altLabel;
    }

    public static Shift parseLabel(String label) {
        for (Shift shift : Shift.values()) {
            if (StringUtils.endsWith(label, shift.getLabel()) || StringUtils.equals(label, shift.getAltLabel())) {
                return shift;
            }
        }
        throw new IllegalArgumentException("no such shift label [" + label + "]");
    }
}
