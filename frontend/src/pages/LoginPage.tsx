import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Button, Input, PasswordInput } from '../components';
import apiClient from '../api/client';

interface TestAccount {
  email: string;
  name: string;
  role: string;
}

interface TestAccountsResponse {
  enabled: boolean;
  password: string;
  accounts: TestAccount[];
}

const isTestModeEnabled = import.meta.env.VITE_TEST_MODE === 'true';

export function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [testAccounts, setTestAccounts] = useState<TestAccountsResponse | null>(null);
  const [selectedAccount, setSelectedAccount] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (isTestModeEnabled) {
      apiClient.get<TestAccountsResponse>('/dev/test-accounts')
        .then(response => {
          setTestAccounts(response.data);
        })
        .catch(() => {
          // Test mode not available on backend
          setTestAccounts(null);
        });
    }
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await login({ email, password });
      navigate('/');
    } catch (err) {
      setError('Invalid email or password');
    } finally {
      setLoading(false);
    }
  };

  const handleTestAccountSelect = (value: string) => {
    setSelectedAccount(value);
    if (value && testAccounts) {
      const account = testAccounts.accounts.find(a => a.email === value);
      if (account) {
        setEmail(account.email);
        setPassword(testAccounts.password);
      }
    }
  };

  const handleQuickLogin = async () => {
    if (!selectedAccount || !testAccounts) return;

    setError('');
    setLoading(true);

    try {
      await login({ email: selectedAccount, password: testAccounts.password });
      navigate('/');
    } catch (err) {
      setError('Failed to login with test account');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center py-12 px-4">
      <div className="max-w-md w-full">
        <div className="text-center mb-8">
          <Link to="/" className="inline-flex items-center gap-2 mb-6">
            <span className="text-3xl">🏠</span>
            <span className="font-serif text-2xl font-semibold text-primary-500">Forever Home</span>
          </Link>
          <h1 className="text-2xl font-bold text-gray-900">Welcome back</h1>
          <p className="text-gray-600 mt-2">Sign in to your account</p>
        </div>

        {testAccounts && (
          <div className="card p-4 mb-4 bg-amber-50 border-amber-200">
            <div className="flex items-center gap-2 mb-3">
              <span className="text-amber-600 font-medium text-sm">Test Mode</span>
              <span className="text-xs text-amber-500 bg-amber-100 px-2 py-0.5 rounded">DEV ONLY</span>
            </div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Quick Login
            </label>
            <select
              value={selectedAccount}
              onChange={(e) => handleTestAccountSelect(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 mb-3"
            >
              <option value="">Select a test account...</option>
              {testAccounts.accounts.map((account) => (
                <option key={account.email} value={account.email}>
                  {account.name} ({account.role})
                </option>
              ))}
            </select>
            <Button
              type="button"
              variant="secondary"
              onClick={handleQuickLogin}
              disabled={!selectedAccount}
              loading={loading && !!selectedAccount}
              className="w-full"
            >
              Login as {selectedAccount ? testAccounts.accounts.find(a => a.email === selectedAccount)?.name : 'Test User'}
            </Button>
          </div>
        )}

        <div className="card p-8">
          {testAccounts && (
            <div className="relative mb-6">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-gray-200" />
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="bg-white px-2 text-gray-500">Or sign in manually</span>
              </div>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            {error && (
              <div className="bg-error-50 border border-error-200 text-error-700 px-4 py-3 rounded">
                {error}
              </div>
            )}

            <Input
              label="Email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              required
              autoComplete="email"
            />

            <div>
              <PasswordInput
                label="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter your password"
                required
                autoComplete="current-password"
              />
              <div className="mt-2 text-right">
                <Link to="/forgot-password" className="text-sm text-primary-500 hover:underline">
                  Forgot password?
                </Link>
              </div>
            </div>

            <Button type="submit" variant="primary" loading={loading && !selectedAccount} className="w-full">
              Sign In
            </Button>
          </form>
        </div>

        <p className="text-center mt-6 text-gray-600">
          Don't have an account?{' '}
          <Link to="/register" className="text-primary-500 font-medium hover:underline">
            Sign up
          </Link>
        </p>
      </div>
    </div>
  );
}
