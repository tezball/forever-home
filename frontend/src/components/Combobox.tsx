import { useState, useRef, useEffect, useId } from 'react';

interface ComboboxOption {
  value: string;
  label: string;
}

interface ComboboxProps {
  label?: string;
  error?: string;
  hint?: string;
  options: ComboboxOption[];
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  required?: boolean;
  disabled?: boolean;
}

export function Combobox({
  label,
  error,
  hint,
  options,
  value,
  onChange,
  placeholder = 'Select or type to filter...',
  required,
  disabled,
}: ComboboxProps) {
  const generatedId = useId();
  const inputId = generatedId;
  const listboxId = `${generatedId}-listbox`;

  const [isOpen, setIsOpen] = useState(false);
  const [inputValue, setInputValue] = useState('');
  const [highlightedIndex, setHighlightedIndex] = useState(-1);

  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLUListElement>(null);

  // Get the display label for the current value
  const selectedOption = options.find((opt) => opt.value === value);

  // Filter options based on input
  const filteredOptions = inputValue
    ? options.filter((opt) => opt.label.toLowerCase().includes(inputValue.toLowerCase()))
    : options;

  // Sync input value with selected option when closed
  useEffect(() => {
    if (!isOpen) {
      setInputValue(selectedOption?.label ?? '');
    }
  }, [isOpen, selectedOption]);

  // Reset highlighted index when filtered options change
  useEffect(() => {
    setHighlightedIndex(-1);
  }, [filteredOptions.length]);

  // Scroll highlighted option into view
  useEffect(() => {
    if (highlightedIndex >= 0 && listRef.current) {
      const highlightedElement = listRef.current.children[highlightedIndex] as HTMLElement;
      highlightedElement?.scrollIntoView({ block: 'nearest' });
    }
  }, [highlightedIndex]);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setInputValue(e.target.value);
    setIsOpen(true);
  };

  const handleInputFocus = () => {
    if (!disabled) {
      setIsOpen(true);
      setInputValue(''); // Clear to show all options
    }
  };

  const handleOptionSelect = (option: ComboboxOption) => {
    onChange(option.value);
    setInputValue(option.label);
    setIsOpen(false);
    inputRef.current?.focus();
  };

  const handleClear = () => {
    onChange('');
    setInputValue('');
    inputRef.current?.focus();
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (disabled) return;

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        if (!isOpen) {
          setIsOpen(true);
        } else {
          setHighlightedIndex((prev) =>
            prev < filteredOptions.length - 1 ? prev + 1 : prev
          );
        }
        break;
      case 'ArrowUp':
        e.preventDefault();
        setHighlightedIndex((prev) => (prev > 0 ? prev - 1 : prev));
        break;
      case 'Enter':
        e.preventDefault();
        if (isOpen && highlightedIndex >= 0 && filteredOptions[highlightedIndex]) {
          handleOptionSelect(filteredOptions[highlightedIndex]);
        }
        break;
      case 'Escape':
        e.preventDefault();
        setIsOpen(false);
        setInputValue(selectedOption?.label ?? '');
        break;
      case 'Tab':
        setIsOpen(false);
        break;
    }
  };

  return (
    <div className="w-full" ref={containerRef}>
      {label && (
        <label htmlFor={inputId} className="block text-sm font-medium text-gray-700 mb-1">
          {label}
          {required && <span className="text-error-500 ml-0.5">*</span>}
        </label>
      )}

      <div className="relative">
        <input
          ref={inputRef}
          id={inputId}
          type="text"
          role="combobox"
          aria-expanded={isOpen}
          aria-haspopup="listbox"
          aria-controls={listboxId}
          aria-activedescendant={
            highlightedIndex >= 0 ? `${listboxId}-option-${highlightedIndex}` : undefined
          }
          aria-invalid={error ? 'true' : undefined}
          aria-describedby={error ? `${inputId}-error` : hint ? `${inputId}-hint` : undefined}
          value={inputValue}
          onChange={handleInputChange}
          onFocus={handleInputFocus}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          disabled={disabled}
          className={`w-full h-12 px-4 py-3 pr-16 border-2 rounded bg-secondary-50 focus:outline-none focus:border-primary-500 transition-colors ${
            error ? 'border-error-500' : 'border-secondary-200'
          } ${disabled ? 'opacity-50 cursor-not-allowed bg-gray-100' : ''}`}
        />

        {/* Clear button */}
        {value && !disabled && (
          <button
            type="button"
            onClick={handleClear}
            className="absolute right-10 top-1/2 -translate-y-1/2 p-1 text-gray-400 hover:text-gray-600"
            aria-label="Clear selection"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        )}

        {/* Dropdown chevron */}
        <button
          type="button"
          onClick={() => !disabled && setIsOpen(!isOpen)}
          className="absolute right-3 top-1/2 -translate-y-1/2 p-1 text-gray-400"
          tabIndex={-1}
          aria-label={isOpen ? 'Close dropdown' : 'Open dropdown'}
        >
          <svg
            className={`w-5 h-5 transition-transform ${isOpen ? 'rotate-180' : ''}`}
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </button>

        {/* Dropdown list */}
        {isOpen && (
          <ul
            ref={listRef}
            id={listboxId}
            role="listbox"
            className="absolute z-50 w-full mt-1 max-h-60 overflow-auto bg-white border-2 border-secondary-200 rounded shadow-lg"
          >
            {filteredOptions.length === 0 ? (
              <li className="px-4 py-3 text-gray-500 text-sm">No matches found</li>
            ) : (
              filteredOptions.map((option, index) => (
                <li
                  key={option.value}
                  id={`${listboxId}-option-${index}`}
                  role="option"
                  aria-selected={option.value === value}
                  onClick={() => handleOptionSelect(option)}
                  onMouseEnter={() => setHighlightedIndex(index)}
                  className={`px-4 py-3 cursor-pointer transition-colors ${
                    index === highlightedIndex
                      ? 'bg-primary-100 text-primary-900'
                      : option.value === value
                      ? 'bg-primary-50'
                      : 'hover:bg-gray-50'
                  }`}
                >
                  {option.label}
                </li>
              ))
            )}
          </ul>
        )}
      </div>

      {hint && !error && (
        <p id={`${inputId}-hint`} className="mt-1 text-sm text-gray-500">
          {hint}
        </p>
      )}
      {error && (
        <p id={`${inputId}-error`} className="mt-1 text-sm text-error-500" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
