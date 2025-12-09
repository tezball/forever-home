import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { Button, Modal } from '../components';
import apiClient from '../api/client';

interface Flag {
  id: string;
  contentType: string;
  contentId: string;
  reporterId?: string;
  reason: string;
  description?: string;
  status: string;
  reviewedBy?: string;
  reviewedAt?: string;
  resolutionNotes?: string;
  createdAt: string;
}

interface FlagSearchResponse {
  flags: Flag[];
  page: number;
  size: number;
  totalCount: number;
  totalPages: number;
}

interface AuditLog {
  id: string;
  action: string;
  entityType?: string;
  entityId?: string;
  actorId?: string;
  details?: string;
  createdAt: string;
}

const REASONS: Record<string, string> = {
  INAPPROPRIATE_CONTENT: 'Inappropriate Content',
  SPAM: 'Spam',
  MISLEADING_INFO: 'Misleading Information',
  FRAUD: 'Fraud',
  ABUSE: 'Abuse',
  OTHER: 'Other',
};

const STATUSES: Record<string, { label: string; color: string }> = {
  PENDING: { label: 'Pending', color: 'status-pending' },
  APPROVED: { label: 'Approved', color: 'status-available' },
  DISMISSED: { label: 'Dismissed', color: 'status-withdrawn' },
};

export function AdminModerationPage() {
  const [activeTab, setActiveTab] = useState<'flags' | 'audit'>('flags');
  const [flags, setFlags] = useState<Flag[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('PENDING');
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [totalCount, setTotalCount] = useState(0);

  // Modal state
  const [resolveModalOpen, setResolveModalOpen] = useState(false);
  const [selectedFlag, setSelectedFlag] = useState<Flag | null>(null);
  const [resolveAction, setResolveAction] = useState<'approve' | 'dismiss'>('dismiss');
  const [resolveNotes, setResolveNotes] = useState('');
  const [resolving, setResolving] = useState(false);

  const fetchFlags = useCallback(async () => {
    try {
      const params = new URLSearchParams();
      if (statusFilter) params.append('status', statusFilter);
      params.append('page', currentPage.toString());
      params.append('size', '20');

      const res = await apiClient.get<FlagSearchResponse>(`/admin/flags?${params.toString()}`);
      setFlags(res.data.flags);
      setTotalPages(res.data.totalPages);
      setTotalCount(res.data.totalCount);
    } catch (err) {
      console.error('Failed to fetch flags:', err);
    }
  }, [statusFilter, currentPage]);

  const fetchAuditLogs = async () => {
    try {
      const res = await apiClient.get<AuditLog[]>('/admin/audit-logs?page=1&size=100');
      setAuditLogs(res.data);
    } catch (err) {
      console.error('Failed to fetch audit logs:', err);
    }
  };

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      await Promise.all([fetchFlags(), fetchAuditLogs()]);
      setLoading(false);
    };
    loadData();
  }, [fetchFlags]);

  useEffect(() => {
    setCurrentPage(1);
  }, [statusFilter]);

  const openResolveModal = (flag: Flag, action: 'approve' | 'dismiss') => {
    setSelectedFlag(flag);
    setResolveAction(action);
    setResolveNotes('');
    setResolveModalOpen(true);
  };

  const handleResolve = async () => {
    if (!selectedFlag) return;

    setResolving(true);
    try {
      await apiClient.put(`/admin/flags/${selectedFlag.id}/${resolveAction}`, {
        notes: resolveNotes,
      });
      setResolveModalOpen(false);
      setSelectedFlag(null);
      fetchFlags();
      fetchAuditLogs();
    } catch (err) {
      console.error('Failed to resolve flag:', err);
    } finally {
      setResolving(false);
    }
  };

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleString();
  };

  const formatAction = (action: string) => {
    return action.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
  };

  return (
    <div className="container-app py-8">
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <nav className="text-sm text-gray-500 mb-2">
            <Link to="/admin/dashboard" className="hover:text-primary-500">Dashboard</Link>
            <span className="mx-2">/</span>
            <span className="text-gray-900">Content Moderation</span>
          </nav>
          <h1 className="text-3xl font-bold text-gray-900">Content Moderation</h1>
        </div>
      </div>

      {/* Tabs */}
      <div className="border-b border-secondary-200 mb-6">
        <nav className="flex gap-8">
          <button
            onClick={() => setActiveTab('flags')}
            className={`pb-4 font-medium border-b-2 transition-colors ${
              activeTab === 'flags'
                ? 'border-primary-500 text-primary-500'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            Content Flags ({totalCount})
          </button>
          <button
            onClick={() => setActiveTab('audit')}
            className={`pb-4 font-medium border-b-2 transition-colors ${
              activeTab === 'audit'
                ? 'border-primary-500 text-primary-500'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            Audit Log
          </button>
        </nav>
      </div>

      {loading ? (
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary-500 border-t-transparent" />
        </div>
      ) : (
        <>
          {/* Flags Tab */}
          {activeTab === 'flags' && (
            <div className="space-y-4">
              {/* Status Filter */}
              <div className="flex gap-2">
                {['PENDING', 'APPROVED', 'DISMISSED', ''].map((status) => (
                  <button
                    key={status || 'all'}
                    onClick={() => setStatusFilter(status)}
                    className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                      statusFilter === status
                        ? 'bg-primary-500 text-white'
                        : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                    }`}
                  >
                    {status || 'All'}
                  </button>
                ))}
              </div>

              {/* Flags Table */}
              {flags.length > 0 ? (
                <div className="card overflow-hidden">
                  <table className="w-full">
                    <thead className="bg-secondary-100">
                      <tr>
                        <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">Content</th>
                        <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">Reason</th>
                        <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">Status</th>
                        <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">Reported</th>
                        <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-secondary-200">
                      {flags.map((flag) => (
                        <tr key={flag.id} className="hover:bg-gray-50">
                          <td className="px-4 py-3">
                            <div className="font-medium text-gray-900">{flag.contentType}</div>
                            <div className="text-xs text-gray-500 font-mono">{flag.contentId.slice(0, 8)}...</div>
                          </td>
                          <td className="px-4 py-3">
                            <div className="text-gray-900">{REASONS[flag.reason] || flag.reason}</div>
                            {flag.description && (
                              <div className="text-xs text-gray-500 truncate max-w-[200px]">{flag.description}</div>
                            )}
                          </td>
                          <td className="px-4 py-3">
                            <span className={`status-badge ${STATUSES[flag.status]?.color || 'status-pending'}`}>
                              {STATUSES[flag.status]?.label || flag.status}
                            </span>
                          </td>
                          <td className="px-4 py-3 text-sm text-gray-500">
                            {formatDate(flag.createdAt)}
                          </td>
                          <td className="px-4 py-3">
                            {flag.status === 'PENDING' && (
                              <div className="flex gap-2">
                                <Button
                                  variant="outline"
                                  size="sm"
                                  onClick={() => openResolveModal(flag, 'dismiss')}
                                >
                                  Dismiss
                                </Button>
                                <Button
                                  variant="primary"
                                  size="sm"
                                  onClick={() => openResolveModal(flag, 'approve')}
                                >
                                  Take Action
                                </Button>
                              </div>
                            )}
                            {flag.status !== 'PENDING' && flag.resolutionNotes && (
                              <span className="text-xs text-gray-500">{flag.resolutionNotes}</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>

                  {/* Pagination */}
                  {totalPages > 1 && (
                    <div className="px-4 py-3 bg-gray-50 border-t border-gray-200 flex items-center justify-between">
                      <div className="text-sm text-gray-600">
                        Page {currentPage} of {totalPages}
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
                  No content flags found
                </div>
              )}
            </div>
          )}

          {/* Audit Log Tab */}
          {activeTab === 'audit' && (
            <div className="card overflow-hidden">
              {auditLogs.length > 0 ? (
                <table className="w-full">
                  <thead className="bg-secondary-100">
                    <tr>
                      <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">Action</th>
                      <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">Entity</th>
                      <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">Details</th>
                      <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">Time</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-secondary-200">
                    {auditLogs.map((log) => (
                      <tr key={log.id} className="hover:bg-gray-50">
                        <td className="px-4 py-3">
                          <span className="font-medium text-gray-900">{formatAction(log.action)}</span>
                        </td>
                        <td className="px-4 py-3">
                          {log.entityType && (
                            <div className="text-gray-900">{log.entityType}</div>
                          )}
                          {log.entityId && (
                            <div className="text-xs text-gray-500 font-mono">{log.entityId.slice(0, 8)}...</div>
                          )}
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-600 max-w-[300px] truncate">
                          {log.details || '-'}
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-500">
                          {formatDate(log.createdAt)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : (
                <div className="p-12 text-center text-gray-500">
                  No audit logs yet
                </div>
              )}
            </div>
          )}
        </>
      )}

      {/* Resolve Modal */}
      <Modal
        isOpen={resolveModalOpen}
        onClose={() => {
          setResolveModalOpen(false);
          setSelectedFlag(null);
        }}
        title={resolveAction === 'approve' ? 'Take Action on Content' : 'Dismiss Flag'}
      >
        <div className="space-y-4">
          {selectedFlag && (
            <>
              <div className="p-4 bg-gray-50 rounded-lg">
                <div className="text-sm text-gray-500 mb-1">Flagged Content</div>
                <div className="font-medium">{selectedFlag.contentType}</div>
                <div className="text-xs text-gray-500 font-mono">{selectedFlag.contentId}</div>
              </div>

              <div className="p-4 bg-gray-50 rounded-lg">
                <div className="text-sm text-gray-500 mb-1">Reason</div>
                <div className="font-medium">{REASONS[selectedFlag.reason] || selectedFlag.reason}</div>
                {selectedFlag.description && (
                  <div className="text-sm text-gray-600 mt-1">{selectedFlag.description}</div>
                )}
              </div>

              {resolveAction === 'approve' && (
                <div className="p-4 bg-warning-50 border border-warning-200 rounded-lg">
                  <div className="flex gap-3">
                    <svg className="w-5 h-5 text-warning-600 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                    </svg>
                    <div className="text-sm text-warning-700">
                      Taking action will mark this flag as approved and may require additional steps to moderate the content.
                    </div>
                  </div>
                </div>
              )}

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Resolution Notes
                </label>
                <textarea
                  value={resolveNotes}
                  onChange={(e) => setResolveNotes(e.target.value)}
                  className="input-field min-h-[80px]"
                  placeholder="Add notes about your decision..."
                />
              </div>

              <div className="flex gap-3 pt-2">
                <Button
                  variant="outline"
                  onClick={() => {
                    setResolveModalOpen(false);
                    setSelectedFlag(null);
                  }}
                  className="flex-1"
                >
                  Cancel
                </Button>
                <Button
                  variant={resolveAction === 'approve' ? 'primary' : 'outline'}
                  onClick={handleResolve}
                  loading={resolving}
                  className="flex-1"
                >
                  {resolveAction === 'approve' ? 'Take Action' : 'Dismiss Flag'}
                </Button>
              </div>
            </>
          )}
        </div>
      </Modal>
    </div>
  );
}
