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
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'approvals' | 'users'>('approvals');

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [approvalsRes, usersRes] = await Promise.all([
        apiClient.get<Approval[]>('/admin/approvals'),
        apiClient.get<UserItem[]>('/admin/users'),
      ]);
      setApprovals(approvalsRes.data);
      setUsers(usersRes.data);
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
      <div className="mb-8 flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Admin Dashboard</h1>
          <p className="text-gray-600">Welcome back, {user?.name}</p>
        </div>
        <a
          href="http://localhost:3000/d/forever-home-admin-analytics"
          target="_blank"
          rel="noopener noreferrer"
          className="btn btn-outline flex items-center gap-2"
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
            <path d="M2 11a1 1 0 011-1h2a1 1 0 011 1v5a1 1 0 01-1 1H3a1 1 0 01-1-1v-5zM8 7a1 1 0 011-1h2a1 1 0 011 1v9a1 1 0 01-1 1H9a1 1 0 01-1-1V7zM14 4a1 1 0 011-1h2a1 1 0 011 1v12a1 1 0 01-1 1h-2a1 1 0 01-1-1V4z" />
          </svg>
          View Analytics in Grafana
        </a>
      </div>

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

        </>
      )}
    </div>
  );
}
