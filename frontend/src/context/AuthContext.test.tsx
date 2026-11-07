import type React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AuthProvider, useAuth } from './AuthContext';
import { authApi } from '../services/api';
import type { User, AuthResponse } from '../types';

vi.mock('../services/api', () => ({
  authApi: {
    getCurrentUser: vi.fn(),
    login: vi.fn(),
    register: vi.fn(),
  },
}));

const mockUser: User = {
  id: 1,
  firstName: 'John',
  lastName: 'Doe',
  email: 'john@example.com',
  role: 'DEVELOPER',
  active: true,
  createdAt: '2026-01-01T00:00:00Z',
};

const TestConsumer: React.FC = () => {
  const { user, token, loading, login, logout } = useAuth();

  if (loading) return <div>Loading Auth...</div>;

  return (
    <div>
      <div data-testid="user-email">{user ? user.email : 'No user'}</div>
      <div data-testid="auth-token">{token || 'No token'}</div>
      <button onClick={() => login('john@example.com', 'password123')}>Login Action</button>
      <button onClick={logout}>Logout Action</button>
    </div>
  );
};

describe('AuthContext', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('initializes with null user and token when localStorage is empty', async () => {
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    expect(await screen.findByTestId('user-email')).toHaveTextContent('No user');
    expect(screen.getByTestId('auth-token')).toHaveTextContent('No token');
  });

  it('initializes user from localStorage and validates via getCurrentUser', async () => {
    localStorage.setItem('token', 'stored-token-xyz');
    localStorage.setItem('user', JSON.stringify(mockUser));
    vi.mocked(authApi.getCurrentUser).mockResolvedValue(mockUser);

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    expect(await screen.findByTestId('user-email')).toHaveTextContent('john@example.com');
    expect(screen.getByTestId('auth-token')).toHaveTextContent('stored-token-xyz');
    expect(authApi.getCurrentUser).toHaveBeenCalledTimes(1);
  });

  it('updates state and localStorage on successful login', async () => {
    const user = userEvent.setup();
    const authResponse: AuthResponse = {
      token: 'jwt-new-token',
      tokenType: 'Bearer',
      user: mockUser,
    };
    vi.mocked(authApi.login).mockResolvedValue(authResponse);

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    expect(await screen.findByTestId('user-email')).toHaveTextContent('No user');

    await user.click(screen.getByText('Login Action'));

    expect(await screen.findByTestId('user-email')).toHaveTextContent('john@example.com');
    expect(screen.getByTestId('auth-token')).toHaveTextContent('jwt-new-token');
    expect(localStorage.getItem('token')).toBe('jwt-new-token');
    expect(JSON.parse(localStorage.getItem('user')!)).toEqual(mockUser);
  });

  it('clears state and localStorage on logout', async () => {
    const user = userEvent.setup();
    localStorage.setItem('token', 'active-token');
    localStorage.setItem('user', JSON.stringify(mockUser));
    vi.mocked(authApi.getCurrentUser).mockResolvedValue(mockUser);

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    expect(await screen.findByTestId('user-email')).toHaveTextContent('john@example.com');

    await user.click(screen.getByText('Logout Action'));

    expect(screen.getByTestId('user-email')).toHaveTextContent('No user');
    expect(screen.getByTestId('auth-token')).toHaveTextContent('No token');
    expect(localStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('user')).toBeNull();
  });

  it('handles auth:unauthorized event by resetting auth state without reload', async () => {
    localStorage.setItem('token', 'expired-token');
    localStorage.setItem('user', JSON.stringify(mockUser));
    vi.mocked(authApi.getCurrentUser).mockResolvedValue(mockUser);

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    expect(await screen.findByTestId('user-email')).toHaveTextContent('john@example.com');

    // Dispatch 401 unauthorized event
    act(() => {
      window.dispatchEvent(new CustomEvent('auth:unauthorized'));
    });

    expect(screen.getByTestId('user-email')).toHaveTextContent('No user');
    expect(screen.getByTestId('auth-token')).toHaveTextContent('No token');
    expect(localStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('user')).toBeNull();
  });
});
