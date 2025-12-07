package com.example.foreverhome.domain.pet;

/**
 * Represents the unit for measuring a pet's age.
 * Young pets (puppies/kittens) are measured in months, adults in years.
 */
public enum AgeUnit {
    MONTHS("month", "months"),
    YEARS("year", "years");

    private final String singular;
    private final String plural;

    AgeUnit(String singular, String plural) {
        this.singular = singular;
        this.plural = plural;
    }

    /**
     * Formats an age value with the appropriate unit (singular or plural).
     * @param age the age value
     * @return formatted string like "1 year" or "5 months"
     */
    public String formatAge(int age) {
        return age + " " + (age == 1 ? singular : plural);
    }
}
