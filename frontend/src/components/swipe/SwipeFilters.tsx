import type { Species, PetSize, PetSex } from '../../types';

interface SwipeFiltersProps {
  species: Species | '';
  size: PetSize | '';
  sex: PetSex | '';
  onChange: (filters: { species: Species | ''; size: PetSize | ''; sex: PetSex | '' }) => void;
}

interface FilterChipProps {
  label: string;
  active: boolean;
  onClick: () => void;
}

function FilterChip({ label, active, onClick }: FilterChipProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap transition-all ${
        active
          ? 'bg-white text-gray-900 shadow-lg'
          : 'bg-white/10 text-white/80 hover:bg-white/20'
      }`}
    >
      {label}
    </button>
  );
}

export function SwipeFilters({ species, size, sex, onChange }: SwipeFiltersProps) {
  const toggleSpecies = (value: Species | '') => {
    onChange({ species: species === value ? '' : value, size, sex });
  };

  const toggleSize = (value: PetSize | '') => {
    onChange({ species, size: size === value ? '' : value, sex });
  };

  const toggleSex = (value: PetSex | '') => {
    onChange({ species, size, sex: sex === value ? '' : value });
  };

  return (
    <div className="flex gap-2 overflow-x-auto scrollbar-hide pb-1">
      <FilterChip
        label="Dogs"
        active={species === 'DOG'}
        onClick={() => toggleSpecies('DOG')}
      />
      <FilterChip
        label="Cats"
        active={species === 'CAT'}
        onClick={() => toggleSpecies('CAT')}
      />
      <div className="w-px bg-white/20 mx-1 self-stretch" />
      <FilterChip
        label="Small"
        active={size === 'SMALL'}
        onClick={() => toggleSize('SMALL')}
      />
      <FilterChip
        label="Medium"
        active={size === 'MEDIUM'}
        onClick={() => toggleSize('MEDIUM')}
      />
      <FilterChip
        label="Large"
        active={size === 'LARGE'}
        onClick={() => toggleSize('LARGE')}
      />
      <div className="w-px bg-white/20 mx-1 self-stretch" />
      <FilterChip
        label="Male"
        active={sex === 'MALE'}
        onClick={() => toggleSex('MALE')}
      />
      <FilterChip
        label="Female"
        active={sex === 'FEMALE'}
        onClick={() => toggleSex('FEMALE')}
      />
    </div>
  );
}
