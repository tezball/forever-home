# Forever home

The forever home website aims to find the forever-home of our beloved pets.

* user can create an account and add their pet to find a new home
* The site is not to sell the pet, but to find a new home by someone adopting it.
* A user must still work with a rescue organization to adopt a pet.
* A vet needs to sign off on the pet, also, the pet must have been
  * Neutered
  * Vaccinated
  * Good/Known Health

## Pages

### Home
The home page will highlight the goal of the site.

### Pet Profile

This page will show the profile of a pet. 
Details:
* Name
* Age
* Breed
* Description
* Size
* Microchip
* Images

### Sign up

User Types:
* Admin: root user
* Rescue Organization: Can CRUD a single rescue organization and all pets belonging to it
* Vet: Can CRUD a single vet. Can sign off on a dog
* Foster: Can register a pet for adoption
* Adopter: Can inherit a pet from a foster through a rescue when the pet has been signed off by a vet.


### Rescue Organization

* List of pets belonging to the organization
* rescue details
  * location
  * phone number
  * website
  * description
  * logo
  * contact name
  * contact email
  * social media links


### Vet 

* List of pets signed off by the vet
* vet details
  * location
  * phone number
  * website
  * description
  * logo

### Pet Browsing

* List of pets
* Filter by breed
* Filter by size

