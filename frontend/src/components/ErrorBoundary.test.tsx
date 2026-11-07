import React, { useState } from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ErrorBoundary } from './ErrorBoundary';

const ThrowError: React.FC<{ shouldThrow: boolean; message?: string }> = ({ shouldThrow, message }) => {
  if (shouldThrow) {
    throw new Error(message || 'Explosion in component');
  }
  return <div>Component rendered successfully</div>;
};

describe('ErrorBoundary', () => {
  it('renders children normally when no error occurs', () => {
    render(
      <ErrorBoundary>
        <div>Normal content</div>
      </ErrorBoundary>
    );

    expect(screen.getByText('Normal content')).toBeInTheDocument();
  });

  it('renders fallback UI and error message when an error is thrown', () => {
    // Suppress console.error in test output for intentional throw
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    render(
      <ErrorBoundary>
        <ThrowError shouldThrow={true} message="Test critical error" />
      </ErrorBoundary>
    );

    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
    expect(screen.getByText(/Test critical error/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument();

    consoleSpy.mockRestore();
  });

  it('resets error state when clicking "Try Again"', async () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    const user = userEvent.setup();

    const Wrapper: React.FC = () => {
      const [hasError, setHasError] = useState(true);
      return (
        <ErrorBoundary onReset={() => setHasError(false)}>
          <ThrowError shouldThrow={hasError} message="Temporary error" />
        </ErrorBoundary>
      );
    };

    render(<Wrapper />);

    expect(screen.getByText('Something went wrong')).toBeInTheDocument();

    const tryAgainBtn = screen.getByRole('button', { name: /try again/i });
    await user.click(tryAgainBtn);

    expect(screen.getByText('Component rendered successfully')).toBeInTheDocument();

    consoleSpy.mockRestore();
  });

  it('renders custom fallback prop when provided', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    render(
      <ErrorBoundary fallback={<div data-testid="custom-fallback">Custom Error Message</div>}>
        <ThrowError shouldThrow={true} />
      </ErrorBoundary>
    );

    expect(screen.getByTestId('custom-fallback')).toBeInTheDocument();
    expect(screen.getByText('Custom Error Message')).toBeInTheDocument();

    consoleSpy.mockRestore();
  });
});

