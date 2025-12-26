import { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { Button, Modal } from '../../components';
import apiClient from '../../api/client';

interface RescueOrg {
  id: string;
  userId: string;
  name: string;
  phone: string | null;
  website: string | null;
  description: string | null;
  logoUrl: string | null;
  contactName: string | null;
  contactEmail: string | null;
  verified: boolean;
  location: string | null;
}

interface UserStats {
  total: number;
  fosters: number;
  adopters: number;
  vets: number;
  rescueOrgs: number;
  verifiedVets: number;
}

interface RescueStats {
  total: number;
  verified: number;
  pending: number;
}

interface PetStats {
  total: number;
  available: number;
  pendingVet: number;
  pendingRescue: number;
  inProgress: number;
  adopted: number;
  draft: number;
}

interface AdminStats {
  users: UserStats;
  rescues: RescueStats;
  pets: PetStats;
  totalAdoptions: number;
  generatedAt: string;
}

export function AdminDashboard() {
  const { user } = useAuth();
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [pendingRescues, setPendingRescues] = useState<RescueOrg[]>([]);
  const [allRescues, setAllRescues] = useState<RescueOrg[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [viewMode, setViewMode] = useState<'pending' | 'all'>('pending');
  const [selectedRescue, setSelectedRescue] = useState<RescueOrg | null>(null);
  const [detailModalOpen, setDetailModalOpen] = useState(false);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [statsRes, pendingRes, allRes] = await Promise.all([
        apiClient.get<AdminStats>('/super-admin/stats'),
        apiClient.get<RescueOrg[]>('/super-admin/rescues/pending'),
        apiClient.get<RescueOrg[]>('/super-admin/rescues'),
      ]);
      setStats(statsRes.data);
      setPendingRescues(pendingRes.data);
      setAllRescues(allRes.data);
    } catch (err) {
      console.error('Failed to fetch admin data:', err);
      setError('Failed to load admin data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleApproveRescue = async (rescue: RescueOrg) => {
    setActionLoading(true);
    setError('');
    try {
      await apiClient.post(`/super-admin/rescues/${rescue.id}/approve`);
      setSuccessMessage(`${rescue.name} has been approved!`);
      // Update local state
      setPendingRescues((prev) => prev.filter((r) => r.id !== rescue.id));
      setAllRescues((prev) =>
        prev.map((r) => (r.id === rescue.id ? { ...r, verified: true } : r))
      );
      // Refresh stats
      const statsRes = await apiClient.get<AdminStats>('/super-admin/stats');
      setStats(statsRes.data);
      setDetailModalOpen(false);
      setTimeout(() => setSuccessMessage(''), 5000);
    } catch (err) {
      console.error('Failed to approve rescue:', err);
      setError('Failed to approve rescue organization.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleRejectRescue = async (rescue: RescueOrg) => {
    setActionLoading(true);
    setError('');
    try {
      await apiClient.post(`/super-admin/rescues/${rescue.id}/reject`);
      setSuccessMessage(`${rescue.name} verification has been revoked.`);
      // Update local state
      setAllRescues((prev) =>
        prev.map((r) => (r.id === rescue.id ? { ...r, verified: false } : r))
      );
      setPendingRescues((prev) => {
        if (!prev.find((r) => r.id === rescue.id)) {
          return [...prev, { ...rescue, verified: false }];
        }
        return prev;
      });
      // Refresh stats
      const statsRes = await apiClient.get<AdminStats>('/super-admin/stats');
      setStats(statsRes.data);
      setDetailModalOpen(false);
      setTimeout(() => setSuccessMessage(''), 5000);
    } catch (err) {
      console.error('Failed to reject rescue:', err);
      setError('Failed to reject rescue organization.');
    } finally {
      setActionLoading(false);
    }
  };

  const openDetailModal = (rescue: RescueOrg) => {
    setSelectedRescue(rescue);
    setDetailModalOpen(true);
  };

  const displayedRescues = viewMode === 'pending' ? pendingRescues : allRescues;

  if (loading) {
    return (
      <div className="container-app py-8">
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary-500 border-t-transparent" />
        </div>
      </div>
    );
  }

  return (
    <div className="container-app py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Admin Dashboard</h1>
        <p className="text-gray-600">Welcome back, {user?.name}</p>
      </div>

      {/* Success Message */}
      {successMessage && (
        <div className="bg-success-50 border border-success-200 text-success-700 px-4 py-3 rounded mb-8">
          {successMessage}
        </div>
      )}

      {/* Error Message */}
      {error && (
        <div className="bg-error-50 border border-error-200 text-error-700 px-4 py-3 rounded mb-8">
          {error}
        </div>
      )}

      {/* Action Required Banner */}
      {pendingRescues.length > 0 && (
        <div className="card p-4 mb-8 border-2 border-warning-200 bg-warning-50">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-8 h-8 bg-warning-500 rounded-full flex items-center justify-center">
              <svg className="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
            <h2 className="text-lg font-semibold text-warning-800">Action Required</h2>
            <span className="px-2 py-0.5 text-xs font-bold bg-warning-500 text-white rounded-full">
              {pendingRescues.length}
            </span>
          </div>
          <p className="text-warning-700">
            {pendingRescues.length} rescue organization{pendingRescues.length !== 1 ? 's' : ''} pending approval
          </p>
        </div>
      )}

      {/* Platform Statistics */}
      {stats && (
        <section className="mb-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Platform Statistics</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {/* User Stats */}
            <div className="card p-4">
              <div className="text-3xl font-bold text-primary-500">{stats.users.total}</div>
              <div className="text-sm text-gray-600">Total Users</div>
            </div>
            <div className="card p-4">
              <div className="text-3xl font-bold text-blue-500">{stats.users.fosters}</div>
              <div className="text-sm text-gray-600">Fosters</div>
            </div>
            <div className="card p-4">
              <div className="text-3xl font-bold text-green-500">{stats.users.adopters}</div>
              <div className="text-sm text-gray-600">Adopters</div>
            </div>
            <div className="card p-4">
              <div className="text-3xl font-bold text-purple-500">{stats.users.vets}</div>
              <div className="text-sm text-gray-600">Vets ({stats.users.verifiedVets} verified)</div>
            </div>
          </div>

          {/* Pet Stats */}
          <h3 className="text-lg font-medium text-gray-800 mt-6 mb-3">Pet Statistics</h3>
          <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-4">
            <div className="card p-3">
              <div className="text-2xl font-bold text-gray-700">{stats.pets.total}</div>
              <div className="text-xs text-gray-500">Total</div>
            </div>
            <div className="card p-3">
              <div className="text-2xl font-bold text-success-500">{stats.pets.available}</div>
              <div className="text-xs text-gray-500">Available</div>
            </div>
            <div className="card p-3">
              <div className="text-2xl font-bold text-blue-500">{stats.pets.pendingVet}</div>
              <div className="text-xs text-gray-500">Pending Vet</div>
            </div>
            <div className="card p-3">
              <div className="text-2xl font-bold text-orange-500">{stats.pets.pendingRescue}</div>
              <div className="text-xs text-gray-500">Pending Rescue</div>
            </div>
            <div className="card p-3">
              <div className="text-2xl font-bold text-purple-500">{stats.pets.inProgress}</div>
              <div className="text-xs text-gray-500">In Progress</div>
            </div>
            <div className="card p-3">
              <div className="text-2xl font-bold text-primary-500">{stats.pets.adopted}</div>
              <div className="text-xs text-gray-500">Adopted</div>
            </div>
            <div className="card p-3">
              <div className="text-2xl font-bold text-gray-400">{stats.pets.draft}</div>
              <div className="text-xs text-gray-500">Draft</div>
            </div>
          </div>

          {/* Rescue Stats */}
          <h3 className="text-lg font-medium text-gray-800 mt-6 mb-3">Rescue Organization Statistics</h3>
          <div className="grid grid-cols-3 gap-4">
            <div className="card p-4">
              <div className="text-3xl font-bold text-gray-700">{stats.rescues.total}</div>
              <div className="text-sm text-gray-600">Total Rescues</div>
            </div>
            <div className="card p-4">
              <div className="text-3xl font-bold text-success-500">{stats.rescues.verified}</div>
              <div className="text-sm text-gray-600">Verified</div>
            </div>
            <div className="card p-4">
              <div className="text-3xl font-bold text-warning-500">{stats.rescues.pending}</div>
              <div className="text-sm text-gray-600">Pending</div>
            </div>
          </div>

          {/* Adoption Stats */}
          <h3 className="text-lg font-medium text-gray-800 mt-6 mb-3">Adoption Statistics</h3>
          <div className="card p-4 inline-block">
            <div className="text-3xl font-bold text-primary-500">{stats.totalAdoptions}</div>
            <div className="text-sm text-gray-600">Total Adoptions Completed</div>
          </div>
        </section>
      )}

      {/* Rescue Organizations Management */}
      <section>
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-semibold text-gray-900">Rescue Organizations</h2>
          <div className="flex gap-2">
            <button
              onClick={() => setViewMode('pending')}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                viewMode === 'pending'
                  ? 'bg-primary-500 text-white'
                  : 'bg-secondary-100 text-gray-700 hover:bg-secondary-200'
              }`}
            >
              Pending ({pendingRescues.length})
            </button>
            <button
              onClick={() => setViewMode('all')}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                viewMode === 'all'
                  ? 'bg-primary-500 text-white'
                  : 'bg-secondary-100 text-gray-700 hover:bg-secondary-200'
              }`}
            >
              All ({allRescues.length})
            </button>
          </div>
        </div>

        {displayedRescues.length > 0 ? (
          <div className="card divide-y divide-secondary-200">
            {displayedRescues.map((rescue) => (
              <div key={rescue.id} className="p-4 flex items-center justify-between">
                <div className="flex items-center gap-4">
                  {rescue.logoUrl ? (
                    <img
                      src={rescue.logoUrl}
                      alt={rescue.name}
                      className="w-12 h-12 rounded-lg object-cover"
                    />
                  ) : (
                    <div className="w-12 h-12 rounded-lg bg-secondary-200 flex items-center justify-center">
                      <svg className="w-6 h-6 text-secondary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                      </svg>
                    </div>
                  )}
                  <div>
                    <div className="font-medium text-gray-900">{rescue.name}</div>
                    <div className="text-sm text-gray-500">
                      {rescue.location || 'No location'}
                      {rescue.contactEmail && ` | ${rescue.contactEmail}`}
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <span
                    className={`px-2 py-1 text-xs font-medium rounded-full ${
                      rescue.verified
                        ? 'bg-success-100 text-success-700'
                        : 'bg-warning-100 text-warning-700'
                    }`}
                  >
                    {rescue.verified ? 'Verified' : 'Pending'}
                  </span>
                  <Button variant="outline" size="sm" onClick={() => openDetailModal(rescue)}>
                    View
                  </Button>
                  {!rescue.verified && (
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => handleApproveRescue(rescue)}
                      loading={actionLoading}
                    >
                      Approve
                    </Button>
                  )}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="card p-8 text-center text-gray-500">
            {viewMode === 'pending'
              ? 'No rescue organizations pending approval'
              : 'No rescue organizations found'}
          </div>
        )}
      </section>

      {/* Rescue Detail Modal */}
      <Modal
        isOpen={detailModalOpen}
        onClose={() => setDetailModalOpen(false)}
        title={selectedRescue?.name || 'Rescue Organization'}
      >
        {selectedRescue && (
          <div className="space-y-4">
            {selectedRescue.logoUrl && (
              <div className="flex justify-center">
                <img
                  src={selectedRescue.logoUrl}
                  alt={selectedRescue.name}
                  className="w-24 h-24 rounded-lg object-cover"
                />
              </div>
            )}

            <div className="space-y-3">
              {selectedRescue.description && (
                <div>
                  <h4 className="font-medium text-gray-900 mb-1">Description</h4>
                  <p className="text-gray-600">{selectedRescue.description}</p>
                </div>
              )}

              <div className="grid grid-cols-2 gap-4">
                {selectedRescue.location && (
                  <div>
                    <h4 className="text-sm font-medium text-gray-500">Location</h4>
                    <p className="text-gray-900">{selectedRescue.location}</p>
                  </div>
                )}
                {selectedRescue.phone && (
                  <div>
                    <h4 className="text-sm font-medium text-gray-500">Phone</h4>
                    <p className="text-gray-900">{selectedRescue.phone}</p>
                  </div>
                )}
                {selectedRescue.contactName && (
                  <div>
                    <h4 className="text-sm font-medium text-gray-500">Contact Person</h4>
                    <p className="text-gray-900">{selectedRescue.contactName}</p>
                  </div>
                )}
                {selectedRescue.contactEmail && (
                  <div>
                    <h4 className="text-sm font-medium text-gray-500">Email</h4>
                    <p className="text-gray-900">{selectedRescue.contactEmail}</p>
                  </div>
                )}
                {selectedRescue.website && (
                  <div className="col-span-2">
                    <h4 className="text-sm font-medium text-gray-500">Website</h4>
                    <a
                      href={selectedRescue.website}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-primary-500 hover:underline"
                    >
                      {selectedRescue.website}
                    </a>
                  </div>
                )}
              </div>

              <div className="pt-4 border-t border-gray-200">
                <div className="flex items-center justify-between">
                  <span className="text-sm text-gray-500">Verification Status</span>
                  <span
                    className={`px-2 py-1 text-xs font-medium rounded-full ${
                      selectedRescue.verified
                        ? 'bg-success-100 text-success-700'
                        : 'bg-warning-100 text-warning-700'
                    }`}
                  >
                    {selectedRescue.verified ? 'Verified' : 'Pending Verification'}
                  </span>
                </div>
              </div>
            </div>

            <div className="flex gap-4 pt-4">
              {selectedRescue.verified ? (
                <Button
                  variant="outline"
                  onClick={() => handleRejectRescue(selectedRescue)}
                  loading={actionLoading}
                  className="flex-1"
                >
                  Revoke Verification
                </Button>
              ) : (
                <>
                  <Button
                    variant="outline"
                    onClick={() => setDetailModalOpen(false)}
                    className="flex-1"
                  >
                    Cancel
                  </Button>
                  <Button
                    variant="primary"
                    onClick={() => handleApproveRescue(selectedRescue)}
                    loading={actionLoading}
                    className="flex-1"
                  >
                    Approve
                  </Button>
                </>
              )}
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
