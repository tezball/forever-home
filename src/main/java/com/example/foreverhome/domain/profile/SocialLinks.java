package com.example.foreverhome.domain.profile;

/**
 * Value object representing social media links for a rescue organization.
 * All fields are optional.
 */
public record SocialLinks(
        String facebook,
        String instagram,
        String twitter,
        String tiktok
) {
    /**
     * Creates an empty SocialLinks with all null values.
     * @return empty SocialLinks instance
     */
    public static SocialLinks empty() {
        return new SocialLinks(null, null, null, null);
    }

    /**
     * Counts how many social links are present (non-null).
     * @return count of non-null links
     */
    public int count() {
        int count = 0;
        if (facebook != null) count++;
        if (instagram != null) count++;
        if (twitter != null) count++;
        if (tiktok != null) count++;
        return count;
    }

    /**
     * Checks if any social links are present.
     * @return true if at least one link is non-null
     */
    public boolean hasAny() {
        return count() > 0;
    }
}
