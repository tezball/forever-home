import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Button } from '../components';
import type { VetSignOffHistory } from '../types';
import apiClient from '../api/client';

export function VetSignOffHistoryPage() {
  const { user } = useAuth();
  const [signOffs, setSignOffs] = useState<VetSignOffHistory[]>([]);
  const [filteredSignOffs, setFilteredSignOffs] = useState<VetSignOffHistory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [totalCount, setTotalCount] = useState(0);

  // Date filters
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  useEffect(() => {
    fetchSignOffHistory();
  }, []);

  useEffect(() => {
    applyDateFilter();
  }, [signOffs, startDate, endDate]);

  const fetchSignOffHistory = async () => {
    try {
      const [historyResponse, countResponse] = await Promise.all([
        apiClient.get<VetSignOffHistory[]>('/vet/sign-offs'),
        apiClient.get<{ count: number }>('/vet/sign-offs/count'),
      ]);
      setSignOffs(historyResponse.data);
      setTotalCount(countResponse.data.count);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to load sign-off history';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const applyDateFilter = () => {
    let filtered = [...signOffs];

    if (startDate) {
      const start = new Date(startDate);
      start.setHours(0, 0, 0, 0);
      filtered = filtered.filter((s) => new Date(s.signedOffAt) >= start);
    }

    if (endDate) {
      const end = new Date(endDate);
      end.setHours(23, 59, 59, 999);
      filtered = filtered.filter((s) => new Date(s.signedOffAt) <= end);
    }

    setFilteredSignOffs(filtered);
  };

  const clearFilters = () => {
    setStartDate('');
    setEndDate('');
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const exportToCSV = () => {
    const headers = ['Pet Name', 'Species', 'Breed', 'Microchip', 'Health Status', 'Health Notes', 'Date'];
    const rows = filteredSignOffs.map((s) => [
      s.petName,
      s.petSpecies || '',
      s.petBreed || '',
      s.petMicrochipId || '',
      s.healthStatus,
      s.healthNotes || '',
      formatDate(s.signedOffAt),
    ]);

    const csvContent = [
      headers.join(','),
      ...rows.map((row) => row.map((cell) => `"${cell.replace(/"/g, '""')}"`).join(',')),
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `vet-signoffs-${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
  };

  return (
    <div className="container-app py-8">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Sign-Off History</h1>
          <p className="text-gray-600">
            Welcome back, {user?.name} - {totalCount} pets verified
          </p>
        </div>
        <Link to="/vet/dashboard">
          <Button variant="outline">Back to Dashboard</Button>
        </Link>
      </div>

      {/* Date Filters */}
      <div className="card p-6 mb-8">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Filter by Date Range</h2>
        <div className="flex flex-wrap gap-4 items-end">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Start Date</label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="input"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">End Date</label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="input"
            />
          </div>
          <Button variant="ghost" onClick={clearFilters}>
            Clear Filters
          </Button>
          <Button variant="primary" onClick={exportToCSV} disabled={filteredSignOffs.length === 0}>
            Export to CSV
          </Button>
        </div>
        {(startDate || endDate) && (
          <p className="mt-3 text-sm text-gray-600">
            Showing {filteredSignOffs.length} of {signOffs.length} sign-offs
          </p>
        )}
      </div>

      {/* Error */}
      {error && (
        <div className="bg-error-50 border border-error-200 text-error-700 px-4 py-3 rounded mb-8">
          {error}
        </div>
      )}

      {/* Loading */}
      {loading ? (
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary-500 border-t-transparent" />
        </div>
      ) : filteredSignOffs.length === 0 ? (
        <div className="card p-12 text-center">
          <div className="text-6xl mb-4">📋</div>
          <h3 className="text-xl font-semibold text-gray-900 mb-2">
            {signOffs.length === 0 ? 'No sign-offs yet' : 'No sign-offs match your filters'}
          </h3>
          <p className="text-gray-600 mb-6">
            {signOffs.length === 0
              ? 'Your verified pets will appear here once you complete sign-offs.'
              : 'Try adjusting your date range filters.'}
          </p>
          {signOffs.length > 0 ? (
            <Button variant="outline" onClick={clearFilters}>
              Clear Filters
            </Button>
          ) : (
            <Link to="/vet/dashboard">
              <Button variant="primary">Go to Dashboard to Verify Pets</Button>
            </Link>
          )}
        </div>
      ) : (
        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Pet
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Microchip
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Health Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Notes
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Date
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {filteredSignOffs.map((signOff) => (
                  <tr key={signOff.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center">
                        <div className="flex-shrink-0 h-10 w-10">
                          <img
                            className="h-10 w-10 rounded-full object-cover"
                            src={signOff.petImageUrl || `https://placedog.net/40/40?id=${signOff.petId.slice(0, 8)}`}
                            alt={signOff.petName}
                          />
                        </div>
                        <div className="ml-4">
                          <div className="text-sm font-medium text-gray-900">{signOff.petName}</div>
                          <div className="text-sm text-gray-500">
                            {signOff.petBreed || signOff.petSpecies}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {signOff.petMicrochipId || 'N/A'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                        signOff.healthStatus === 'HEALTHY'
                          ? 'bg-success-100 text-success-800'
                          : signOff.healthStatus === 'MINOR_ISSUES'
                          ? 'bg-warning-100 text-warning-800'
                          : 'bg-error-100 text-error-800'
                      }`}>
                        {signOff.healthStatus.replace('_', ' ')}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500 max-w-xs truncate">
                      {signOff.healthNotes || '-'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {formatDate(signOff.signedOffAt)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                      <Link
                        to={`/pets/${signOff.petId}`}
                        className="text-primary-500 hover:text-primary-600"
                      >
                        View Pet
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
