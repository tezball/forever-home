import { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { Button } from '../../components';
import apiClient from '../../api/client';

interface Approval {
  id: string;
  type: 'RESCUE_ORG';
  name: string;
  email: string;
  submittedAt: string;
}

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

interface UserItem {
  id: string;
  name: string;
  email: string;
  role: string;
  status: string;
  profileComplete: boolean;
}

export function AdminDashboard() {
  const { user } = useAuth();
  const [approvals, setApprovals] = useState<Approval[]>([]);
  const [users, setUsers] = useState<UserItem[]>([]);
  const [analytics, setAnalytics] = useState<Analytics | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'approvals' | 'users' | 'analytics'>('approvals');

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [approvalsRes, usersRes, analyticsRes] = await Promise.all([
        apiClient.get<Approval[]>('/admin/approvals'),
        apiClient.get<UserItem[]>('/admin/users'),
        apiClient.get<Analytics>('/admin/analytics'),
      ]);
      setApprovals(approvalsRes.data);
      setUsers(usersRes.data);
      setAnalytics(analyticsRes.data);
    } catch (err) {
      console.error('Failed to fetch admin data:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (type: string, id: string) => {
    try {
      await apiClient.put(`/admin/approvals/${type}/${id}/approve`);
      setApprovals(approvals.filter((a) => a.id !== id));
      fetchData();
    } catch {
      // Handle error
    }
  };

  const handleReject = async (type: string, id: string) => {
    try {
      await apiClient.put(`/admin/approvals/${type}/${id}/reject`);
      setApprovals(approvals.filter((a) => a.id !== id));
      fetchData();
    } catch {
      // Handle error
    }
  };

  const handleSuspendUser = async (userId: string) => {
    try {
      await apiClient.put(`/admin/users/${userId}/suspend`);
      setUsers(users.map((u) => (u.id === userId ? { ...u, status: 'SUSPENDED' } : u)));
    } catch {
      // Handle error
    }
  };

  const handleReactivateUser = async (userId: string) => {
    try {
      await apiClient.put(`/admin/users/${userId}/reactivate`);
      setUsers(users.map((u) => (u.id === userId ? { ...u, status: 'ACTIVE' } : u)));
    } catch {
      // Handle error
    }
  };

  return (
    <div className="container-app py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Admin Dashboard</h1>
        <p className="text-gray-600">Welcome back, {user?.name}</p>
      </div>

      {/* Analytics Summary */}
      {analytics && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
          <div className="card p-4 text-center">
            <p className="text-3xl font-bold text-primary-500">{analytics.totalUsers}</p>
            <p className="text-sm text-gray-600">Total Users</p>
          </div>
          <div className="card p-4 text-center">
            <p className="text-3xl font-bold text-info-500">{analytics.totalPets}</p>
            <p className="text-sm text-gray-600">Total Pets</p>
          </div>
          <div className="card p-4 text-center">
            <p className="text-3xl font-bold text-success-500">{analytics.totalAdoptions}</p>
            <p className="text-sm text-gray-600">Adoptions</p>
          </div>
          <div className="card p-4 text-center">
            <p className="text-3xl font-bold text-warning-600">{analytics.pendingApprovals}</p>
            <p className="text-sm text-gray-600">Pending</p>
          </div>
        </div>
      )}

      {/* Tabs */}
      <div className="border-b border-secondary-200 mb-6">
        <nav className="flex gap-8">
          <button
            onClick={() => setActiveTab('approvals')}
            className={`pb-4 font-medium border-b-2 transition-colors ${
              activeTab === 'approvals'
                ? 'border-primary-500 text-primary-500'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            Rescue Org Approvals ({approvals.length})
          </button>
          <button
            onClick={() => setActiveTab('users')}
            className={`pb-4 font-medium border-b-2 transition-colors ${
              activeTab === 'users'
                ? 'border-primary-500 text-primary-500'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            User Management
          </button>
          <button
            onClick={() => setActiveTab('analytics')}
            className={`pb-4 font-medium border-b-2 transition-colors ${
              activeTab === 'analytics'
                ? 'border-primary-500 text-primary-500'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            Analytics
          </button>
        </nav>
      </div>

      {loading ? (
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary-500 border-t-transparent" />
        </div>
      ) : (
        <>
          {/* Approvals Tab */}
          {activeTab === 'approvals' && (
            <div>
              {approvals.length > 0 ? (
                <div className="card divide-y divide-secondary-200">
                  {approvals.map((approval) => (
                    <div key={approval.id} className="p-4 flex items-center justify-between">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="status-badge status-available">
                            Rescue Org
                          </span>
                          <span className="font-medium text-gray-900">{approval.name}</span>
                        </div>
                        <p className="text-sm text-gray-500">{approval.email}</p>
                        <p className="text-xs text-gray-400">
                          Submitted {new Date(approval.submittedAt).toLocaleDateString()}
                        </p>
                      </div>
                      <div className="flex gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleReject(approval.type, approval.id)}
                        >
                          Reject
                        </Button>
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={() => handleApprove(approval.type, approval.id)}
                        >
                          Approve
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="card p-12 text-center text-gray-500">
                  No pending approvals
                </div>
              )}
            </div>
          )}

          {/* Users Tab */}
          {activeTab === 'users' && (
            <div>
              {users.length > 0 ? (
                <div className="card overflow-hidden">
                  <table className="w-full">
                    <thead className="bg-secondary-100">
                      <tr>
                        <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">Name</th>
                        <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">Email</th>
                        <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">Role</th>
                        <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">Status</th>
                        <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-secondary-200">
                      {users.map((u) => (
                        <tr key={u.id}>
                          <td className="px-4 py-3 text-gray-900">{u.name}</td>
                          <td className="px-4 py-3 text-gray-600">{u.email}</td>
                          <td className="px-4 py-3">
                            <span className="text-sm capitalize">{u.role.toLowerCase().replace('_', ' ')}</span>
                          </td>
                          <td className="px-4 py-3">
                            <span className={`status-badge ${u.status === 'ACTIVE' ? 'status-available' : u.status === 'PENDING' ? 'status-pending' : 'status-withdrawn'}`}>
                              {u.status}
                            </span>
                          </td>
                          <td className="px-4 py-3">
                            {u.status === 'ACTIVE' && (
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => handleSuspendUser(u.id)}
                              >
                                Suspend
                              </Button>
                            )}
                            {u.status === 'SUSPENDED' && (
                              <Button
                                variant="primary"
                                size="sm"
                                onClick={() => handleReactivateUser(u.id)}
                              >
                                Reactivate
                              </Button>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="card p-12 text-center text-gray-500">
                  No users to display
                </div>
              )}
            </div>
          )}

          {/* Analytics Tab */}
          {activeTab === 'analytics' && analytics && (
            <div className="grid md:grid-cols-2 gap-6">
              <div className="card p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">User Distribution</h3>
                <div className="space-y-3">
                  <div className="flex justify-between">
                    <span className="text-gray-600">Adopters</span>
                    <span className="font-medium">{analytics.userDistribution.adopters}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Fosters</span>
                    <span className="font-medium">{analytics.userDistribution.fosters}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Rescue Organizations</span>
                    <span className="font-medium">{analytics.userDistribution.rescueOrgs}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Veterinarians</span>
                    <span className="font-medium">{analytics.userDistribution.vets}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Admins</span>
                    <span className="font-medium">{analytics.userDistribution.admins}</span>
                  </div>
                </div>
              </div>
              <div className="card p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Pet Statistics</h3>
                <div className="space-y-4">
                  <div className="flex justify-between items-start">
                    <div>
                      <span className="text-gray-900 font-medium">Available</span>
                      <p className="text-xs text-gray-500">Passed all verifications, publicly listed for adoption</p>
                    </div>
                    <span className="font-medium text-success-500">{analytics.petStatistics.available}</span>
                  </div>
                  <div className="flex justify-between items-start">
                    <div>
                      <span className="text-gray-900 font-medium">Pending Rescue Review</span>
                      <p className="text-xs text-gray-500">Submitted by foster, awaiting rescue organization acceptance</p>
                    </div>
                    <span className="font-medium text-warning-600">{analytics.petStatistics.pendingRescue}</span>
                  </div>
                  <div className="flex justify-between items-start">
                    <div>
                      <span className="text-gray-900 font-medium">Pending Vet Review</span>
                      <p className="text-xs text-gray-500">Accepted by rescue, awaiting veterinary verification</p>
                    </div>
                    <span className="font-medium text-warning-600">{analytics.petStatistics.pendingVet}</span>
                  </div>
                  <div className="flex justify-between items-start">
                    <div>
                      <span className="text-gray-900 font-medium">In Progress</span>
                      <p className="text-xs text-gray-500">Adoption application approved, process underway</p>
                    </div>
                    <span className="font-medium text-info-500">{analytics.petStatistics.inProgress}</span>
                  </div>
                  <div className="flex justify-between items-start">
                    <div>
                      <span className="text-gray-900 font-medium">Adopted</span>
                      <p className="text-xs text-gray-500">Successfully adopted, found their forever home</p>
                    </div>
                    <span className="font-medium text-primary-500">{analytics.petStatistics.adopted}</span>
                  </div>
                  <div className="flex justify-between items-start">
                    <div>
                      <span className="text-gray-900 font-medium">Draft</span>
                      <p className="text-xs text-gray-500">Profile being created by foster, not yet submitted</p>
                    </div>
                    <span className="font-medium text-gray-400">{analytics.petStatistics.draft}</span>
                  </div>
                  <div className="flex justify-between items-start">
                    <div>
                      <span className="text-gray-900 font-medium">On Hold</span>
                      <p className="text-xs text-gray-500">Temporarily unavailable (medical, behavioral, or admin review)</p>
                    </div>
                    <span className="font-medium text-gray-500">{analytics.petStatistics.onHold}</span>
                  </div>
                  <div className="flex justify-between items-start">
                    <div>
                      <span className="text-gray-900 font-medium">Withdrawn</span>
                      <p className="text-xs text-gray-500">Removed from adoption process by foster</p>
                    </div>
                    <span className="font-medium text-gray-400">{analytics.petStatistics.withdrawn}</span>
                  </div>
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
