-- Update test rescue organization addresses to Irish locations
-- Only updates records that still have the placeholder "Pet City" address

UPDATE rescue_organizations
SET address_street = '12 Grafton Street',
    address_city = 'Dublin',
    address_state = 'Dublin',
    address_postal_code = 'D02 VK60',
    address_country = 'Ireland',
    phone = '+353 1 5550100'
WHERE name = 'Happy Tails Rescue'
  AND address_city = 'Pet City';

UPDATE rescue_organizations
SET address_street = '45 Shop Street',
    address_city = 'Galway',
    address_state = 'Galway',
    address_postal_code = 'H91 E2K3',
    address_country = 'Ireland',
    phone = '+353 91 5550101'
WHERE name = 'Second Chance Animal Shelter'
  AND address_city = 'Pet City';

UPDATE rescue_organizations
SET address_street = '78 Patrick Street',
    address_city = 'Cork',
    address_state = 'Cork',
    address_postal_code = 'T12 W8HK',
    address_country = 'Ireland',
    phone = '+353 21 5550102'
WHERE name = 'Paws & Claws Rescue'
  AND address_city = 'Pet City';

UPDATE rescue_organizations
SET address_street = '23 Main Street',
    address_city = 'Limerick',
    address_state = 'Limerick',
    address_postal_code = 'V94 T9PX',
    address_country = 'Ireland',
    phone = '+353 61 5550103'
WHERE name = 'Forever Friends Animal Rescue'
  AND address_city = 'Pet City';
