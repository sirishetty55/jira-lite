import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import { Login } from './Login';
import * as AuthContextModule from '../context/AuthContext';
import * as ToastModule from '../components/Toast';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('Login Component', () => {
  const mockLogin = vi.fn();
  const mockToastError = vi.fn();
  const mockToastSuccess = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();

    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      user: null,
      token: null,
      loading: false,
      login: mockLogin,
      register: vi.fn(),
      logout: vi.fn(),
      refreshUser: vi.fn(),
    });

    vi.spyOn(ToastModule, 'useToast').mockReturnValue({
      toast: vi.fn(),
      success: mockToastSuccess,
      error: mockToastError,
      info: vi.fn(),
    });
  });

  const renderComponent = () => {
    return render(
      <BrowserRouter>
        <Login />
      </BrowserRouter>
    );
  };

  it('renders login form with email, password, and submit button', () => {
    renderComponent();

    expect(screen.getByRole('heading', { name: /welcome back/i })).toBeInTheDocument();
    expect(screen.getByPlaceholderText('you@company.com')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('••••••••')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
    expect(screen.getByText('Quick Demo Accounts')).toBeInTheDocument();
  });

  it('populates fields when clicking quick demo account buttons', async () => {
    const user = userEvent.setup();
    renderComponent();

    const adminDemoBtn = screen.getByRole('button', { name: /admin/i });
    await user.click(adminDemoBtn);

    const emailInput = screen.getByPlaceholderText('you@company.com') as HTMLInputElement;
    const passwordInput = screen.getByPlaceholderText('••••••••') as HTMLInputElement;

    expect(emailInput.value).toBe('admin@jiralite.com');
    expect(passwordInput.value).toBe('Admin@JiraLite2026!');
  });

  it('triggers successful login and navigates to root on valid submission', async () => {
    const user = userEvent.setup();
    mockLogin.mockResolvedValueOnce(undefined);

    renderComponent();

    const emailInput = screen.getByPlaceholderText('you@company.com');
    const passwordInput = screen.getByPlaceholderText('••••••••');
    const submitBtn = screen.getByRole('button', { name: /sign in/i });

    await user.type(emailInput, 'dev@company.com');
    await user.type(passwordInput, 'ValidPassword123');
    await user.click(submitBtn);

    expect(mockLogin).toHaveBeenCalledWith('dev@company.com', 'ValidPassword123');
    expect(mockToastSuccess).toHaveBeenCalledWith('Logged in successfully!');
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  it('displays API error toast when login fails', async () => {
    const user = userEvent.setup();
    const errorResponse = {
      response: {
        data: {
          message: 'Invalid credentials provided',
        },
      },
    };
    mockLogin.mockRejectedValueOnce(errorResponse);

    renderComponent();

    const emailInput = screen.getByPlaceholderText('you@company.com');
    const passwordInput = screen.getByPlaceholderText('••••••••');
    const submitBtn = screen.getByRole('button', { name: /sign in/i });

    await user.type(emailInput, 'wrong@company.com');
    await user.type(passwordInput, 'wrongpass');
    await user.click(submitBtn);

    expect(mockLogin).toHaveBeenCalledWith('wrong@company.com', 'wrongpass');
    expect(mockToastError).toHaveBeenCalledWith('Invalid credentials provided');
    expect(mockNavigate).not.toHaveBeenCalled();
  });
});
