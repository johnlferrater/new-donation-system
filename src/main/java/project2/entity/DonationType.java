package project2.entity;

import java.util.Arrays;
import java.util.List;

public enum DonationType {
    CASH("Cash"),
    CHECK("Check"),
    ONLINE("Online"),
    IN_KIND("In-Kind"),
    OTHER("Other");

    private final String label;

    DonationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static List<String> labels() {
        return Arrays.stream(values()).map(DonationType::getLabel).toList();
    }
}
