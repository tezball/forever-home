package com.example.foreverhome.domain.pet;

import java.util.Arrays;
import java.util.List;

/**
 * Represents common breeds for dogs and cats.
 * Each breed is associated with a specific species.
 */
public enum Breed {
    // Dog breeds
    LABRADOR("Labrador", Species.DOG),
    GOLDEN_RETRIEVER("Golden Retriever", Species.DOG),
    GERMAN_SHEPHERD("German Shepherd", Species.DOG),
    FRENCH_BULLDOG("French Bulldog", Species.DOG),
    BULLDOG("Bulldog", Species.DOG),
    BEAGLE("Beagle", Species.DOG),
    POODLE("Poodle", Species.DOG),
    POODLE_MIX("Poodle Mix", Species.DOG),
    ROTTWEILER("Rottweiler", Species.DOG),
    YORKSHIRE_TERRIER("Yorkshire Terrier", Species.DOG),
    BOXER("Boxer", Species.DOG),
    DACHSHUND("Dachshund", Species.DOG),
    HUSKY("Husky", Species.DOG),
    SIBERIAN_HUSKY("Siberian Husky", Species.DOG),
    GREAT_DANE("Great Dane", Species.DOG),
    DOBERMAN("Doberman", Species.DOG),
    AUSTRALIAN_SHEPHERD("Australian Shepherd", Species.DOG),
    CAVALIER_KING_CHARLES("Cavalier King Charles", Species.DOG),
    SHIH_TZU("Shih Tzu", Species.DOG),
    BOSTON_TERRIER("Boston Terrier", Species.DOG),
    BERNESE_MOUNTAIN_DOG("Bernese Mountain Dog", Species.DOG),
    POMERANIAN("Pomeranian", Species.DOG),
    HAVANESE("Havanese", Species.DOG),
    SHETLAND_SHEEPDOG("Shetland Sheepdog", Species.DOG),
    CORGI("Corgi", Species.DOG),
    PEMBROKE_WELSH_CORGI("Pembroke Welsh Corgi", Species.DOG),
    COCKER_SPANIEL("Cocker Spaniel", Species.DOG),
    BORDER_COLLIE("Border Collie", Species.DOG),
    CHIHUAHUA("Chihuahua", Species.DOG),
    MINIATURE_SCHNAUZER("Miniature Schnauzer", Species.DOG),
    PIT_BULL("Pit Bull", Species.DOG),
    MALTESE("Maltese", Species.DOG),
    JACK_RUSSELL_TERRIER("Jack Russell Terrier", Species.DOG),
    BICHON_FRISE("Bichon Frise", Species.DOG),
    MIXED_DOG("Mixed Breed", Species.DOG),

    // Cat breeds
    TABBY("Tabby", Species.CAT),
    DOMESTIC_SHORTHAIR("Domestic Shorthair", Species.CAT),
    DOMESTIC_LONGHAIR("Domestic Longhair", Species.CAT),
    SIAMESE("Siamese", Species.CAT),
    MAINE_COON("Maine Coon", Species.CAT),
    PERSIAN("Persian", Species.CAT),
    RAGDOLL("Ragdoll", Species.CAT),
    BENGAL("Bengal", Species.CAT),
    BRITISH_SHORTHAIR("British Shorthair", Species.CAT),
    ABYSSINIAN("Abyssinian", Species.CAT),
    BIRMAN("Birman", Species.CAT),
    ORIENTAL_SHORTHAIR("Oriental Shorthair", Species.CAT),
    SPHYNX("Sphynx", Species.CAT),
    DEVON_REX("Devon Rex", Species.CAT),
    SCOTTISH_FOLD("Scottish Fold", Species.CAT),
    RUSSIAN_BLUE("Russian Blue", Species.CAT),
    AMERICAN_SHORTHAIR("American Shorthair", Species.CAT),
    NORWEGIAN_FOREST_CAT("Norwegian Forest Cat", Species.CAT),
    EXOTIC_SHORTHAIR("Exotic Shorthair", Species.CAT),
    BURMESE("Burmese", Species.CAT),
    TONKINESE("Tonkinese", Species.CAT),
    HIMALAYAN("Himalayan", Species.CAT),
    TURKISH_ANGORA("Turkish Angora", Species.CAT),
    RAGAMUFFIN("Ragamuffin", Species.CAT),
    MIXED_CAT("Mixed Breed", Species.CAT);

    private final String displayName;
    private final Species species;

    Breed(String displayName, Species species) {
        this.displayName = displayName;
        this.species = species;
    }

    /**
     * Gets the human-readable display name for this breed.
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the species this breed belongs to.
     * @return the species (DOG or CAT)
     */
    public Species getSpecies() {
        return species;
    }

    /**
     * Gets all breeds for a given species.
     * @param species the species to filter by
     * @return list of breeds for that species
     */
    public static List<Breed> getBySpecies(Species species) {
        return Arrays.stream(values())
            .filter(b -> b.species == species)
            .toList();
    }
}
