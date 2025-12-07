import { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { Button } from '../../components';
import type { User } from '../../types';
import apiClient from '../../api/client';

interface Approval {
  id: string;
  type: 'VET' | 'RESCUE_ORG';
  name: string;
  email: string;
  submittedAt: string;
}

interface Analytics {
  totalUsers: number;
  totalPets: number;
  totalAdoptions: number;
  pendingApprovals: number;
}

export function AdminDashboard() {
  const { user } = useAuth();
  const [approvals, setApprovals] = useState<Approval[]>([]);
  const [users, setUsers] = useState<User[]>([]);
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
        apiClient.get<User[]>('/admin/users'),
        apiClient.get<Analytics>('/admin/analytics'),
      ]);
      setApprovals(approvalsRes.data);
      setUsers(usersRes.data);
      setAnalytics(analyticsRes.data);
    } catch {
      // Demo data
      setApprovals([
        {
          id: '1',
          type: 'VET',
          name: 'Dr. Sarah Johnson',
          email: 'sarah@vetclinic.com',
          submittedAt: new Date().toISOString(),
        },
        {
          id: '2',
          type: 'RESCUE_ORG',
          name: 'Happy Tails Rescue',
          email: 'contact@happytails.org',
          submittedAt: new Date().toISOString(),
        },
      ]);
      setUsers([]);
      setAnalytics({
        totalUsers: 150,
        totalPets: 45,
        totalAdoptions: 32,
        pendingApprovals: 2,
      });
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (type: string, id: string) => {
    try {
      await apiClient.put(`/admin/approvals/${type}/${id}/approve`);
      setApprovals(approvals.filter((a) => a.id !== id));
    } catch {
      // Handle error
    }
  };

  const handleReject = async (type: string, id: string) => {
    try {
      await apiClient.put(`/admin/approvals/${type}/${id}/reject`);
      setApprovals(approvals.filter((a) => a.id !== id));
    } catch {
      // Handle error
    }
  };

  const handleSuspendUser = async (userId: string) => {
    try {
      await apiClient.put(`/admin/users/${userId}/suspend`);
      setUsers(users.map((u) => (u.id === userId ? { ...u, status: 'SUSPENDED' as const } : u)));
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

      {/* Analytics */}
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
            Pending Approvals ({approvals.length})
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
                          <span className={`status-badge ${approval.type === 'VET' ? 'status-pending' : 'status-available'}`}>
                            {approval.type === 'VET' ? 'Veterinarian' : 'Rescue Org'}
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
                            <span className="text-sm capitalize">{u.role.toLowerCase()}</span>
                          </td>
                          <td className="px-4 py-3">
                            <span className={`status-badge ${u.status === 'ACTIVE' ? 'status-available' : 'status-withdrawn'}`}>
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
                    <span className="font-medium">85</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Fosters</span>
                    <span className="font-medium">35</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Rescue Organizations</span>
                    <span className="font-medium">12</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Veterinarians</span>
                    <span className="font-medium">18</span>
                  </div>
                </div>
              </div>
              <div className="card p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Pet Statistics</h3>
                <div className="space-y-3">
                  <div className="flex justify-between">
                    <span className="text-gray-600">Available</span>
                    <span className="font-medium text-success-500">23</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Pending Review</span>
                    <span className="font-medium text-warning-600">8</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">In Progress</span>
                    <span className="font-medium text-info-500">5</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Adopted This Month</span>
                    <span className="font-medium text-primary-500">9</span>
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
