import '@testing-library/jest-dom';
import { afterEach, vi } from 'vitest';
import { cleanup } from '@testing-library/react';

// Clean up DOM after each test to prevent state leaks
afterEach(() => {
  cleanup();
  localStorage.clear();
  vi.clearAllMocks();
});
