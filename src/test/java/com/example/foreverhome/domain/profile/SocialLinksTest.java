package com.example.foreverhome.domain.profile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SocialLinks value object")
class SocialLinksTest {

    @Nested
    @DisplayName("given complete social links")
    class GivenCompleteSocialLinks {

        @Test
        @DisplayName("should store all social media links")
        void shouldStoreAllSocialMediaLinks() {
            SocialLinks links = new SocialLinks(
                    "https://facebook.com/rescue",
                    "rescue_org",
                    "@rescue_org",
                    "@rescue_org"
            );

            assertThat(links.facebook()).isEqualTo("https://facebook.com/rescue");
            assertThat(links.instagram()).isEqualTo("rescue_org");
            assertThat(links.twitter()).isEqualTo("@rescue_org");
            assertThat(links.tiktok()).isEqualTo("@rescue_org");
        }
    }

    @Nested
    @DisplayName("given partial social links")
    class GivenPartialSocialLinks {

        @Test
        @DisplayName("should allow null values for unused platforms")
        void shouldAllowNullValuesForUnusedPlatforms() {
            SocialLinks links = new SocialLinks(
                    "https://facebook.com/rescue",
                    "rescue_org",
                    null,
                    null
            );

            assertThat(links.facebook()).isEqualTo("https://facebook.com/rescue");
            assertThat(links.instagram()).isEqualTo("rescue_org");
            assertThat(links.twitter()).isNull();
            assertThat(links.tiktok()).isNull();
        }

        @Test
        @DisplayName("should count non-null links")
        void shouldCountNonNullLinks() {
            SocialLinks links = new SocialLinks(
                    "https://facebook.com/rescue",
                    "rescue_org",
                    null,
                    null
            );

            assertThat(links.count()).isEqualTo(2);
        }

        @Test
        @DisplayName("should check if any links are present")
        void shouldCheckIfAnyLinksArePresent() {
            SocialLinks withLinks = new SocialLinks("https://facebook.com/rescue", null, null, null);
            SocialLinks noLinks = new SocialLinks(null, null, null, null);

            assertThat(withLinks.hasAny()).isTrue();
            assertThat(noLinks.hasAny()).isFalse();
        }
    }

    @Nested
    @DisplayName("when creating empty social links")
    class WhenCreatingEmptySocialLinks {

        @Test
        @DisplayName("empty() factory method should create links with all null values")
        void emptyFactoryMethodShouldCreateLinksWithAllNullValues() {
            SocialLinks empty = SocialLinks.empty();

            assertThat(empty.facebook()).isNull();
            assertThat(empty.instagram()).isNull();
            assertThat(empty.twitter()).isNull();
            assertThat(empty.tiktok()).isNull();
            assertThat(empty.hasAny()).isFalse();
        }
    }

    @Nested
    @DisplayName("when checking equality")
    class WhenCheckingEquality {

        @Test
        @DisplayName("two social links with same values should be equal")
        void twoSocialLinksWithSameValuesShouldBeEqual() {
            SocialLinks links1 = new SocialLinks("fb", "ig", "tw", "tt");
            SocialLinks links2 = new SocialLinks("fb", "ig", "tw", "tt");

            assertThat(links1).isEqualTo(links2);
            assertThat(links1.hashCode()).isEqualTo(links2.hashCode());
        }
    }
}
