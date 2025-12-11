import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { Button, Modal } from '../../components';
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

interface UserSearchResponse {
  users: UserItem[];
  page: number;
  size: number;
  totalCount: number;
  totalPages: number;
}

const ROLES = ['ADOPTER', 'FOSTER', 'RESCUE_ORG', 'VET', 'ADMIN'] as const;
const STATUSES = ['ACTIVE', 'PENDING', 'SUSPENDED'] as const;

export function AdminDashboard() {
  const { user } = useAuth();
  const [approvals, setApprovals] = useState<Approval[]>([]);
  const [users, setUsers] = useState<UserItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'approvals' | 'users'>('approvals');

  // Search and filter state
  const [searchQuery, setSearchQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalCount, setTotalCount] = useState(0);
  const pageSize = 20;

  // Reset password modal state
  const [resetModalOpen, setResetModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<UserItem | null>(null);
  const [resetLoading, setResetLoading] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [error, setError] = useState('');

  // Approval action state
  const [processingApprovalId, setProcessingApprovalId] = useState<string | null>(null);

  const fetchApprovals = async () => {
    try {
      const res = await apiClient.get<Approval[]>('/admin/approvals');
      setApprovals(res.data);
    } catch (err) {
      console.error('Failed to fetch approvals:', err);
    }
  };

  const fetchUsers = useCallback(async () => {
    try {
      const params = new URLSearchParams();
      if (searchQuery) params.append('query', searchQuery);
      if (roleFilter) params.append('role', roleFilter);
      if (statusFilter) params.append('status', statusFilter);
      params.append('page', currentPage.toString());
      params.append('size', pageSize.toString());

      const res = await apiClient.get<UserSearchResponse>(`/admin/users/search?${params.toString()}`);
      setUsers(res.data.users);
      setTotalPages(res.data.totalPages);
      setTotalCount(res.data.totalCount);
    } catch (err) {
      console.error('Failed to fetch users:', err);
    }
  }, [searchQuery, roleFilter, statusFilter, currentPage]);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      await Promise.all([fetchApprovals(), fetchUsers()]);
      setLoading(false);
    };
    loadData();
  }, [fetchUsers]);

  // Reset to page 1 when filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [searchQuery, roleFilter, statusFilter]);

  const handleApprove = async (type: string, id: string) => {
    setProcessingApprovalId(id);
    setError('');
    try {
      await apiClient.put(`/admin/approvals/${type}/${id}/approve`);
      setSuccessMessage('Rescue organization approved successfully');
      await fetchApprovals();
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        const response = (err as { response?: { data?: { message?: string } } }).response;
        setError(response?.data?.message || 'Failed to approve rescue organization');
      } else {
        setError('Failed to approve rescue organization');
      }
    } finally {
      setProcessingApprovalId(null);
    }
  };

  const handleReject = async (type: string, id: string) => {
    setProcessingApprovalId(id);
    setError('');
    try {
      await apiClient.put(`/admin/approvals/${type}/${id}/reject`);
      setSuccessMessage('Rescue organization rejected');
      await fetchApprovals();
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        const response = (err as { response?: { data?: { message?: string } } }).response;
        setError(response?.data?.message || 'Failed to reject rescue organization');
      } else {
        setError('Failed to reject rescue organization');
      }
    } finally {
      setProcessingApprovalId(null);
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

  const openResetModal = (userItem: UserItem) => {
    setSelectedUser(userItem);
    setResetModalOpen(true);
    setError('');
  };

  const handleResetPassword = async () => {
    if (!selectedUser) return;

    setResetLoading(true);
    setError('');

    try {
      await apiClient.post(`/admin/users/${selectedUser.id}/reset-password`);
      setSuccessMessage(`Password reset email sent to ${selectedUser.email}`);
      setResetModalOpen(false);
      setSelectedUser(null);
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        const response = (err as { response?: { data?: { message?: string } } }).response;
        setError(response?.data?.message || 'Failed to reset password');
      } else {
        setError('Failed to reset password');
      }
    } finally {
      setResetLoading(false);
    }
  };

  const clearFilters = () => {
    setSearchQuery('');
    setRoleFilter('');
    setStatusFilter('');
    setCurrentPage(1);
  };

  return (
    <div className="container-app py-8">
      <div className="mb-8 flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Admin Dashboard</h1>
          <p className="text-gray-600">Welcome back, {user?.name}</p>
        </div>
        <div className="flex gap-3">
          <Link
            to="/admin/moderation"
            className="btn btn-outline flex items-center gap-2"
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M3 6a3 3 0 013-3h10a1 1 0 01.8 1.6L14.25 8l2.55 3.4A1 1 0 0116 13H6a1 1 0 00-1 1v3a1 1 0 11-2 0V6z" clipRule="evenodd" />
            </svg>
            Content Moderation
          </Link>
          <Link
            to="/admin/analytics"
            className="btn btn-outline flex items-center gap-2"
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
              <path d="M2 11a1 1 0 011-1h2a1 1 0 011 1v5a1 1 0 01-1 1H3a1 1 0 01-1-1v-5zM8 7a1 1 0 011-1h2a1 1 0 011 1v9a1 1 0 01-1 1H9a1 1 0 01-1-1V7zM14 4a1 1 0 011-1h2a1 1 0 011 1v12a1 1 0 01-1 1h-2a1 1 0 01-1-1V4z" />
            </svg>
            Analytics
          </Link>
          <a
            href="http://localhost:3000/d/forever-home-admin-analytics"
            target="_blank"
            rel="noopener noreferrer"
            className="btn btn-outline flex items-center gap-2"
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M4.083 9h1.946c.089-1.546.383-2.97.837-4.118A6.004 6.004 0 004.083 9zM10 2a8 8 0 100 16 8 8 0 000-16zm0 2c-.076 0-.232.032-.465.262-.238.234-.497.623-.737 1.182-.389.907-.673 2.142-.766 3.556h3.936c-.093-1.414-.377-2.649-.766-3.556-.24-.56-.5-.948-.737-1.182C10.232 4.032 10.076 4 10 4zm3.971 5c-.089-1.546-.383-2.97-.837-4.118A6.004 6.004 0 0115.917 9h-1.946zm-2.003 2H8.032c.093 1.414.377 2.649.766 3.556.24.56.5.948.737 1.182.233.23.389.262.465.262.076 0 .232-.032.465-.262.238-.234.498-.623.737-1.182.389-.907.673-2.142.766-3.556zm1.166 4.118c.454-1.147.748-2.572.837-4.118h1.946a6.004 6.004 0 01-2.783 4.118zm-6.268 0C6.412 13.97 6.118 12.546 6.03 11H4.083a6.004 6.004 0 002.783 4.118z" clipRule="evenodd" />
            </svg>
            Grafana
          </a>
        </div>
      </div>

      {/* Success Message */}
      {successMessage && (
        <div className="mb-6 bg-success-50 border border-success-200 text-success-700 px-4 py-3 rounded flex justify-between items-center">
          <span>{successMessage}</span>
          <button onClick={() => setSuccessMessage('')} className="text-success-700 hover:text-success-900">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      )}

      {/* Error Message */}
      {error && (
        <div className="mb-6 bg-error-50 border border-error-200 text-error-700 px-4 py-3 rounded flex justify-between items-center">
          <span>{error}</span>
          <button onClick={() => setError('')} className="text-error-700 hover:text-error-900">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
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
            User Management ({totalCount})
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
                          disabled={processingApprovalId === approval.id}
                        >
                          {processingApprovalId === approval.id ? 'Processing...' : 'Reject'}
                        </Button>
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={() => handleApprove(approval.type, approval.id)}
                          disabled={processingApprovalId === approval.id}
                        >
                          {processingApprovalId === approval.id ? 'Processing...' : 'Approve'}
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
            <div className="space-y-4">
              {/* Search and Filters */}
              <div className="card p-4">
                <div className="flex flex-wrap gap-4 items-end">
                  {/* Search */}
                  <div className="flex-1 min-w-[200px]">
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Search
                    </label>
                    <input
                      type="text"
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      placeholder="Search by name or email..."
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                    />
                  </div>

                  {/* Role Filter */}
                  <div className="w-40">
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Role
                    </label>
                    <select
                      value={roleFilter}
                      onChange={(e) => setRoleFilter(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                    >
                      <option value="">All Roles</option>
                      {ROLES.map((role) => (
                        <option key={role} value={role}>
                          {role.replace('_', ' ')}
                        </option>
                      ))}
                    </select>
                  </div>

                  {/* Status Filter */}
                  <div className="w-40">
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Status
                    </label>
                    <select
                      value={statusFilter}
                      onChange={(e) => setStatusFilter(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                    >
                      <option value="">All Statuses</option>
                      {STATUSES.map((status) => (
                        <option key={status} value={status}>
                          {status}
                        </option>
                      ))}
                    </select>
                  </div>

                  {/* Clear Filters */}
                  {(searchQuery || roleFilter || statusFilter) && (
                    <Button variant="outline" size="sm" onClick={clearFilters}>
                      Clear Filters
                    </Button>
                  )}
                </div>
              </div>

              {/* Users Table */}
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
                        <tr key={u.id} className="hover:bg-gray-50">
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
                            <div className="flex gap-2">
                              {u.status === 'ACTIVE' && (
                                <>
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    onClick={() => openResetModal(u)}
                                  >
                                    Reset Password
                                  </Button>
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    onClick={() => handleSuspendUser(u.id)}
                                  >
                                    Suspend
                                  </Button>
                                </>
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
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>

                  {/* Pagination */}
                  {totalPages > 1 && (
                    <div className="px-4 py-3 bg-gray-50 border-t border-gray-200 flex items-center justify-between">
                      <div className="text-sm text-gray-600">
                        Showing {((currentPage - 1) * pageSize) + 1} to {Math.min(currentPage * pageSize, totalCount)} of {totalCount} users
                      </div>
                      <div className="flex gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                          disabled={currentPage === 1}
                        >
                          Previous
                        </Button>
                        <span className="px-3 py-1 text-sm text-gray-700">
                          Page {currentPage} of {totalPages}
                        </span>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                          disabled={currentPage === totalPages}
                        >
                          Next
                        </Button>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <div className="card p-12 text-center text-gray-500">
                  No users match your filters
                </div>
              )}
            </div>
          )}

        </>
      )}

      {/* Reset Password Modal */}
      <Modal
        isOpen={resetModalOpen}
        onClose={() => {
          setResetModalOpen(false);
          setSelectedUser(null);
          setError('');
        }}
        title="Reset User Password"
      >
        <div className="space-y-4">
          {selectedUser && (
            <>
              <div className="p-4 bg-gray-50 rounded-lg">
                <p className="font-medium text-gray-900">{selectedUser.name}</p>
                <p className="text-sm text-gray-600">{selectedUser.email}</p>
                <p className="text-sm text-gray-500 capitalize">
                  {selectedUser.role.toLowerCase().replace('_', ' ')}
                </p>
              </div>

              <div className="bg-warning-50 border border-warning-200 rounded-lg p-4">
                <div className="flex gap-3">
                  <svg className="w-5 h-5 text-warning-600 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                  </svg>
                  <div>
                    <h4 className="font-medium text-warning-800">Password Reset</h4>
                    <p className="text-sm text-warning-700 mt-1">
                      This will generate a new temporary password and send it to the user's email address.
                      The user will be required to change their password on next login.
                    </p>
                  </div>
                </div>
              </div>

              {error && (
                <div className="bg-error-50 border border-error-200 text-error-700 px-4 py-3 rounded">
                  {error}
                </div>
              )}

              <div className="flex gap-3 pt-2">
                <Button
                  variant="outline"
                  onClick={() => {
                    setResetModalOpen(false);
                    setSelectedUser(null);
                    setError('');
                  }}
                  className="flex-1"
                >
                  Cancel
                </Button>
                <Button
                  variant="primary"
                  onClick={handleResetPassword}
                  loading={resetLoading}
                  className="flex-1"
                >
                  Reset Password
                </Button>
              </div>
            </>
          )}
        </div>
      </Modal>
    </div>
  );
}
