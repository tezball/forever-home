import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Button, Modal } from '../components';
import apiClient from '../api/client';

interface RescueOrg {
  id: string;
  name: string;
  city: string | null;
  state: string | null;
  phone: string | null;
}

interface ApprovalRequest {
  id: string;
  rescueOrgId: string;
  rescueOrgName: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN';
  message: string | null;
  requestedAt: string;
  reviewedAt: string | null;
  rejectionReason: string | null;
}

export function VetRequestApprovalPage() {
  const [availableOrgs, setAvailableOrgs] = useState<RescueOrg[]>([]);
  const [myRequests, setMyRequests] = useState<ApprovalRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Request modal state
  const [requestModalOpen, setRequestModalOpen] = useState(false);
  const [selectedOrg, setSelectedOrg] = useState<RescueOrg | null>(null);
  const [requestMessage, setRequestMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [orgsRes, requestsRes] = await Promise.all([
        apiClient.get<RescueOrg[]>('/vet/rescue-orgs/available'),
        apiClient.get<ApprovalRequest[]>('/vet/approval-requests'),
      ]);
      setAvailableOrgs(orgsRes.data);
      setMyRequests(requestsRes.data);
    } catch {
      setError('Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  const openRequestModal = (org: RescueOrg) => {
    setSelectedOrg(org);
    setRequestMessage('');
    setRequestModalOpen(true);
  };

  const handleSubmitRequest = async () => {
    if (!selectedOrg) return;

    setSubmitting(true);
    setError('');
    try {
      const response = await apiClient.post<ApprovalRequest>(
        `/vet/rescue-orgs/${selectedOrg.id}/request`,
        { message: requestMessage || null }
      );
      setRequestModalOpen(false);
      setSuccessMessage(`Approval request sent to ${selectedOrg.name}`);
      // Add to my requests and remove from available
      setMyRequests((prev) => [...prev, response.data]);
      setAvailableOrgs((prev) => prev.filter((org) => org.id !== selectedOrg.id));
      setTimeout(() => setSuccessMessage(''), 5000);
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        const response = (err as { response?: { data?: { message?: string } } }).response;
        setError(response?.data?.message || 'Failed to submit request');
      } else {
        setError('Failed to submit request');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleWithdrawRequest = async (request: ApprovalRequest) => {
    setActionLoading(request.id);
    setError('');
    try {
      await apiClient.delete(`/vet/approval-requests/${request.id}`);
      setSuccessMessage(`Request to ${request.rescueOrgName} has been withdrawn`);
      // Update request status locally
      setMyRequests((prev) =>
        prev.map((r) => (r.id === request.id ? { ...r, status: 'WITHDRAWN' as const } : r))
      );
      // Add org back to available
      const orgRes = await apiClient.get<RescueOrg[]>('/vet/rescue-orgs/available');
      setAvailableOrgs(orgRes.data);
      setTimeout(() => setSuccessMessage(''), 5000);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to withdraw request';
      setError(errorMessage);
    } finally {
      setActionLoading(null);
    }
  };

  const formatLocation = (org: RescueOrg) => {
    if (org.city && org.state) return `${org.city}, ${org.state}`;
    return org.city || org.state || 'Location not specified';
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  const getStatusBadge = (status: ApprovalRequest['status']) => {
    switch (status) {
      case 'PENDING':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-warning-100 text-warning-800">
            Pending
          </span>
        );
      case 'APPROVED':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-success-100 text-success-800">
            Approved
          </span>
        );
      case 'REJECTED':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-error-100 text-error-800">
            Rejected
          </span>
        );
      case 'WITHDRAWN':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
            Withdrawn
          </span>
        );
    }
  };

  // Filter requests by status for display
  const pendingRequests = myRequests.filter((r) => r.status === 'PENDING');
  const completedRequests = myRequests.filter((r) => r.status !== 'PENDING');

  return (
    <div className="container-app py-8">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Request Rescue Approvals</h1>
          <p className="text-gray-600">Request approval from rescue organizations to verify their pets</p>
        </div>
        <Link to="/vet/dashboard">
          <Button variant="outline">Back to Dashboard</Button>
        </Link>
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

      {loading ? (
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-4 border-primary-500 border-t-transparent" />
        </div>
      ) : (
        <div className="space-y-8">
          {/* Pending Requests */}
          {pendingRequests.length > 0 && (
            <section>
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-xl font-semibold text-gray-900">Pending Requests</h2>
                <span className="text-sm text-gray-500">{pendingRequests.length} pending</span>
              </div>
              <div className="card divide-y divide-secondary-200">
                {pendingRequests.map((request) => (
                  <div key={request.id} className="p-4">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-4">
                        <div className="w-12 h-12 bg-warning-100 rounded-full flex items-center justify-center">
                          <svg className="w-6 h-6 text-warning-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                          </svg>
                        </div>
                        <div>
                          <p className="font-medium text-gray-900">{request.rescueOrgName}</p>
                          <p className="text-sm text-gray-500">Requested {formatDate(request.requestedAt)}</p>
                        </div>
                      </div>
                      <div className="flex items-center gap-3">
                        {getStatusBadge(request.status)}
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleWithdrawRequest(request)}
                          loading={actionLoading === request.id}
                        >
                          Withdraw
                        </Button>
                      </div>
                    </div>
                    {request.message && (
                      <div className="mt-3 ml-16 text-sm text-gray-600 bg-gray-50 p-3 rounded">
                        <span className="font-medium">Your message:</span> {request.message}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </section>
          )}

          {/* Available Organizations */}
          <section>
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-semibold text-gray-900">Available Organizations</h2>
              <span className="text-sm text-gray-500">{availableOrgs.length} available</span>
            </div>
            {availableOrgs.length > 0 ? (
              <div className="card divide-y divide-secondary-200">
                {availableOrgs.map((org) => (
                  <div key={org.id} className="p-4 flex items-center justify-between">
                    <div className="flex items-center gap-4">
                      <div className="w-12 h-12 bg-primary-100 rounded-full flex items-center justify-center">
                        <svg className="w-6 h-6 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                        </svg>
                      </div>
                      <div>
                        <p className="font-medium text-gray-900">{org.name}</p>
                        <p className="text-sm text-gray-500">{formatLocation(org)}</p>
                      </div>
                    </div>
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => openRequestModal(org)}
                    >
                      Request Approval
                    </Button>
                  </div>
                ))}
              </div>
            ) : (
              <div className="card p-8 text-center text-gray-500">
                No organizations available to request approval from at this time.
                {myRequests.length > 0 && (
                  <span className="block mt-2 text-sm">
                    You may already have pending or approved requests with all available organizations.
                  </span>
                )}
              </div>
            )}
          </section>

          {/* Request History */}
          {completedRequests.length > 0 && (
            <section>
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-xl font-semibold text-gray-900">Request History</h2>
                <span className="text-sm text-gray-500">{completedRequests.length} requests</span>
              </div>
              <div className="card divide-y divide-secondary-200">
                {completedRequests.map((request) => (
                  <div key={request.id} className="p-4">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-4">
                        <div className={`w-12 h-12 rounded-full flex items-center justify-center ${
                          request.status === 'APPROVED' ? 'bg-success-100' :
                          request.status === 'REJECTED' ? 'bg-error-100' : 'bg-gray-100'
                        }`}>
                          {request.status === 'APPROVED' ? (
                            <svg className="w-6 h-6 text-success-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                          ) : request.status === 'REJECTED' ? (
                            <svg className="w-6 h-6 text-error-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                          ) : (
                            <svg className="w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                            </svg>
                          )}
                        </div>
                        <div>
                          <p className="font-medium text-gray-900">{request.rescueOrgName}</p>
                          <p className="text-sm text-gray-500">
                            Requested {formatDate(request.requestedAt)}
                            {request.reviewedAt && ` - Reviewed ${formatDate(request.reviewedAt)}`}
                          </p>
                        </div>
                      </div>
                      {getStatusBadge(request.status)}
                    </div>
                    {request.rejectionReason && (
                      <div className="mt-3 ml-16 text-sm text-error-600 bg-error-50 p-3 rounded">
                        <span className="font-medium">Reason:</span> {request.rejectionReason}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </section>
          )}
        </div>
      )}

      {/* Request Modal */}
      <Modal
        isOpen={requestModalOpen}
        onClose={() => setRequestModalOpen(false)}
        title={`Request Approval from ${selectedOrg?.name}`}
      >
        <div className="space-y-4">
          <p className="text-gray-600">
            Send a request to {selectedOrg?.name} to be approved as a veterinarian for their organization.
            Once approved, you'll be able to verify pets from this rescue.
          </p>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Message (optional)
            </label>
            <textarea
              value={requestMessage}
              onChange={(e) => setRequestMessage(e.target.value)}
              className="input min-h-24"
              placeholder="Introduce yourself or explain why you'd like to work with this organization..."
            />
          </div>

          <div className="flex gap-4 pt-4">
            <Button
              variant="outline"
              onClick={() => setRequestModalOpen(false)}
              className="flex-1"
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={handleSubmitRequest}
              loading={submitting}
              className="flex-1"
            >
              Send Request
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
