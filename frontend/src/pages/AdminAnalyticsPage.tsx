import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Button } from '../components';
import apiClient from '../api/client';

interface UserDistribution {
  adopters: number;
  fosters: number;
  rescueOrgs: number;
  vets: number;
  admins: number;
}

interface PetStatistics {
  available: number;
  pendingRescue: number;
  pendingVet: number;
  inProgress: number;
  adopted: number;
  draft: number;
  onHold: number;
  withdrawn: number;
}

interface Analytics {
  totalUsers: number;
  totalPets: number;
  totalAdoptions: number;
  pendingApprovals: number;
  userDistribution: UserDistribution;
  petStatistics: PetStatistics;
}

export function AdminAnalyticsPage() {
  const [analytics, setAnalytics] = useState<Analytics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchAnalytics();
  }, []);

  const fetchAnalytics = async () => {
    try {
      const response = await apiClient.get<Analytics>('/admin/analytics');
      setAnalytics(response.data);
    } catch {
      setError('Failed to load analytics data');
    } finally {
      setLoading(false);
    }
  };

  const exportToCSV = () => {
    if (!analytics) return;

    const rows = [
      ['Metric', 'Value'],
      ['Total Users', analytics.totalUsers.toString()],
      ['Total Pets', analytics.totalPets.toString()],
      ['Total Adoptions', analytics.totalAdoptions.toString()],
      ['Pending Approvals', analytics.pendingApprovals.toString()],
      [''],
      ['User Distribution', ''],
      ['Adopters', analytics.userDistribution.adopters.toString()],
      ['Fosters', analytics.userDistribution.fosters.toString()],
      ['Rescue Orgs', analytics.userDistribution.rescueOrgs.toString()],
      ['Vets', analytics.userDistribution.vets.toString()],
      ['Admins', analytics.userDistribution.admins.toString()],
      [''],
      ['Pet Statistics', ''],
      ['Available', analytics.petStatistics.available.toString()],
      ['Pending Rescue', analytics.petStatistics.pendingRescue.toString()],
      ['Pending Vet', analytics.petStatistics.pendingVet.toString()],
      ['In Progress', analytics.petStatistics.inProgress.toString()],
      ['Adopted', analytics.petStatistics.adopted.toString()],
      ['Draft', analytics.petStatistics.draft.toString()],
      ['On Hold', analytics.petStatistics.onHold.toString()],
      ['Withdrawn', analytics.petStatistics.withdrawn.toString()],
    ];

    const csvContent = rows.map((row) => row.join(',')).join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `forever-home-analytics-${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
  };

  if (loading) {
    return (
      <div className="container-app py-8">
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary-500 border-t-transparent" />
        </div>
      </div>
    );
  }

  if (error || !analytics) {
    return (
      <div className="container-app py-8">
        <div className="text-center py-12">
          <div className="text-6xl mb-4">📊</div>
          <h1 className="text-2xl font-bold text-gray-900 mb-2">Analytics Error</h1>
          <p className="text-gray-600 mb-8">{error || 'Failed to load analytics'}</p>
          <Link to="/admin/dashboard" className="text-primary-500 hover:underline">
            ← Back to Dashboard
          </Link>
        </div>
      </div>
    );
  }

  // Calculate percentages for visualizations
  const userTotal =
    analytics.userDistribution.adopters +
    analytics.userDistribution.fosters +
    analytics.userDistribution.rescueOrgs +
    analytics.userDistribution.vets +
    analytics.userDistribution.admins;

  const petTotal =
    analytics.petStatistics.available +
    analytics.petStatistics.pendingRescue +
    analytics.petStatistics.pendingVet +
    analytics.petStatistics.inProgress +
    analytics.petStatistics.adopted +
    analytics.petStatistics.draft +
    analytics.petStatistics.onHold +
    analytics.petStatistics.withdrawn;

  const userRoles = [
    { name: 'Adopters', value: analytics.userDistribution.adopters, color: 'bg-primary-500' },
    { name: 'Fosters', value: analytics.userDistribution.fosters, color: 'bg-success-500' },
    { name: 'Rescue Orgs', value: analytics.userDistribution.rescueOrgs, color: 'bg-secondary-500' },
    { name: 'Vets', value: analytics.userDistribution.vets, color: 'bg-warning-500' },
    { name: 'Admins', value: analytics.userDistribution.admins, color: 'bg-error-500' },
  ];

  const petStatuses = [
    { name: 'Available', value: analytics.petStatistics.available, color: 'bg-success-500' },
    { name: 'Pending Rescue', value: analytics.petStatistics.pendingRescue, color: 'bg-warning-500' },
    { name: 'Pending Vet', value: analytics.petStatistics.pendingVet, color: 'bg-primary-500' },
    { name: 'In Progress', value: analytics.petStatistics.inProgress, color: 'bg-secondary-500' },
    { name: 'Adopted', value: analytics.petStatistics.adopted, color: 'bg-success-700' },
    { name: 'Draft', value: analytics.petStatistics.draft, color: 'bg-gray-400' },
    { name: 'On Hold', value: analytics.petStatistics.onHold, color: 'bg-gray-500' },
    { name: 'Withdrawn', value: analytics.petStatistics.withdrawn, color: 'bg-error-500' },
  ];

  return (
    <div className="container-app py-8">
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <nav className="text-sm text-gray-500 mb-2">
            <Link to="/admin/dashboard" className="hover:text-primary-500">Dashboard</Link>
            <span className="mx-2">/</span>
            <span className="text-gray-900">Analytics</span>
          </nav>
          <h1 className="text-3xl font-bold text-gray-900">Platform Analytics</h1>
        </div>
        <div className="flex gap-3">
          <Button variant="outline" onClick={exportToCSV}>
            <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            Export CSV
          </Button>
          <a
            href="http://localhost:3000/d/forever-home-admin-analytics"
            target="_blank"
            rel="noopener noreferrer"
            className="btn btn-primary flex items-center gap-2"
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
              <path d="M2 11a1 1 0 011-1h2a1 1 0 011 1v5a1 1 0 01-1 1H3a1 1 0 01-1-1v-5zM8 7a1 1 0 011-1h2a1 1 0 011 1v9a1 1 0 01-1 1H9a1 1 0 01-1-1V7zM14 4a1 1 0 011-1h2a1 1 0 011 1v12a1 1 0 01-1 1h-2a1 1 0 01-1-1V4z" />
            </svg>
            View in Grafana
          </a>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-6 mb-8">
        <div className="card p-6">
          <div className="text-4xl font-bold text-primary-600">{analytics.totalUsers}</div>
          <div className="text-gray-600">Total Users</div>
        </div>
        <div className="card p-6">
          <div className="text-4xl font-bold text-secondary-600">{analytics.totalPets}</div>
          <div className="text-gray-600">Total Pets</div>
        </div>
        <div className="card p-6">
          <div className="text-4xl font-bold text-success-600">{analytics.totalAdoptions}</div>
          <div className="text-gray-600">Adoptions</div>
        </div>
        <div className="card p-6">
          <div className="text-4xl font-bold text-warning-600">{analytics.pendingApprovals}</div>
          <div className="text-gray-600">Pending Approvals</div>
        </div>
      </div>

      <div className="grid md:grid-cols-2 gap-8">
        {/* User Distribution */}
        <div className="card p-6">
          <h2 className="text-xl font-bold text-gray-900 mb-4">User Distribution</h2>
          <div className="space-y-4">
            {userRoles.map((role) => (
              <div key={role.name}>
                <div className="flex justify-between text-sm mb-1">
                  <span className="text-gray-700">{role.name}</span>
                  <span className="text-gray-500">
                    {role.value} ({userTotal > 0 ? Math.round((role.value / userTotal) * 100) : 0}%)
                  </span>
                </div>
                <div className="h-3 bg-gray-200 rounded-full overflow-hidden">
                  <div
                    className={`h-full ${role.color} transition-all duration-500`}
                    style={{ width: `${userTotal > 0 ? (role.value / userTotal) * 100 : 0}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Pet Status Distribution */}
        <div className="card p-6">
          <h2 className="text-xl font-bold text-gray-900 mb-4">Pet Status Distribution</h2>
          <div className="space-y-4">
            {petStatuses.filter((s) => s.value > 0).map((status) => (
              <div key={status.name}>
                <div className="flex justify-between text-sm mb-1">
                  <span className="text-gray-700">{status.name}</span>
                  <span className="text-gray-500">
                    {status.value} ({petTotal > 0 ? Math.round((status.value / petTotal) * 100) : 0}%)
                  </span>
                </div>
                <div className="h-3 bg-gray-200 rounded-full overflow-hidden">
                  <div
                    className={`h-full ${status.color} transition-all duration-500`}
                    style={{ width: `${petTotal > 0 ? (status.value / petTotal) * 100 : 0}%` }}
                  />
                </div>
              </div>
            ))}
            {petStatuses.every((s) => s.value === 0) && (
              <p className="text-gray-500 text-center py-4">No pets registered yet</p>
            )}
          </div>
        </div>
      </div>

      {/* Key Metrics Table */}
      <div className="card p-6 mt-8">
        <h2 className="text-xl font-bold text-gray-900 mb-4">Detailed Metrics</h2>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-200">
                <th className="text-left py-3 px-4 font-semibold text-gray-700">Metric</th>
                <th className="text-right py-3 px-4 font-semibold text-gray-700">Count</th>
                <th className="text-right py-3 px-4 font-semibold text-gray-700">Percentage</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              <tr className="bg-gray-50">
                <td colSpan={3} className="py-2 px-4 font-semibold text-gray-900">Users</td>
              </tr>
              {userRoles.map((role) => (
                <tr key={role.name}>
                  <td className="py-2 px-4 text-gray-600">{role.name}</td>
                  <td className="py-2 px-4 text-right text-gray-900">{role.value}</td>
                  <td className="py-2 px-4 text-right text-gray-500">
                    {userTotal > 0 ? Math.round((role.value / userTotal) * 100) : 0}%
                  </td>
                </tr>
              ))}
              <tr className="bg-gray-50">
                <td colSpan={3} className="py-2 px-4 font-semibold text-gray-900">Pets by Status</td>
              </tr>
              {petStatuses.map((status) => (
                <tr key={status.name}>
                  <td className="py-2 px-4 text-gray-600">{status.name}</td>
                  <td className="py-2 px-4 text-right text-gray-900">{status.value}</td>
                  <td className="py-2 px-4 text-right text-gray-500">
                    {petTotal > 0 ? Math.round((status.value / petTotal) * 100) : 0}%
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
