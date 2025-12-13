import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Button } from '../components';
import apiClient from '../api/client';

interface Vet {
  id: string;
  clinicName: string;
  licenseNumber: string;
  phone: string | null;
  website: string | null;
  description: string | null;
  city: string | null;
  state: string | null;
  verified: boolean;
}

interface VetApprovalRequest {
  id: string;
  vetId: string;
  vetClinicName: string;
  vetLicenseNumber: string | null;
  vetPhone: string | null;
  vetCity: string | null;
  vetState: string | null;
  status: string;
  message: string | null;
  requestedAt: string;
}

export function RescueVetManagement() {
  const [pendingVets, setPendingVets] = useState<Vet[]>([]);
  const [approvedVets, setApprovedVets] = useState<Vet[]>([]);
  const [approvalRequests, setApprovalRequests] = useState<VetApprovalRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [rejectReason, setRejectReason] = useState('');
  const [rejectingRequestId, setRejectingRequestId] = useState<string | null>(null);

  useEffect(() => {
    fetchVets();
  }, []);

  const fetchVets = async () => {
    try {
      const [pendingRes, approvedRes, requestsRes] = await Promise.all([
        apiClient.get<Vet[]>('/rescue-org/vets/pending'),
        apiClient.get<Vet[]>('/rescue-org/vets/approved'),
        apiClient.get<VetApprovalRequest[]>('/rescue-org/approval-requests'),
      ]);
      setPendingVets(pendingRes.data);
      setApprovedVets(approvedRes.data);
      setApprovalRequests(requestsRes.data);
    } catch {
      setError('Failed to load vets');
    } finally {
      setLoading(false);
    }
  };

  const handleApproveVet = async (vet: Vet) => {
    setActionLoading(vet.id);
    setError('');
    try {
      await apiClient.post(`/rescue-org/vets/${vet.id}/approve`);
      setSuccessMessage(`${vet.clinicName} has been approved`);
      // Move from pending to approved
      setPendingVets((prev) => prev.filter((v) => v.id !== vet.id));
      setApprovedVets((prev) => [...prev, vet]);
      setTimeout(() => setSuccessMessage(''), 5000);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to approve vet';
      setError(errorMessage);
    } finally {
      setActionLoading(null);
    }
  };

  const handleRevokeVet = async (vet: Vet) => {
    setActionLoading(vet.id);
    setError('');
    try {
      await apiClient.delete(`/rescue-org/vets/${vet.id}/approve`);
      setSuccessMessage(`${vet.clinicName}'s approval has been revoked`);
      // Move from approved to pending
      setApprovedVets((prev) => prev.filter((v) => v.id !== vet.id));
      setPendingVets((prev) => [...prev, vet]);
      setTimeout(() => setSuccessMessage(''), 5000);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to revoke approval';
      setError(errorMessage);
    } finally {
      setActionLoading(null);
    }
  };

  const handleApproveRequest = async (request: VetApprovalRequest) => {
    setActionLoading(request.id);
    setError('');
    try {
      await apiClient.post(`/rescue-org/approval-requests/${request.id}/approve`);
      setSuccessMessage(`${request.vetClinicName} has been approved`);
      // Remove from requests and refresh approved list
      setApprovalRequests((prev) => prev.filter((r) => r.id !== request.id));
      const approvedRes = await apiClient.get<Vet[]>('/rescue-org/vets/approved');
      setApprovedVets(approvedRes.data);
      setTimeout(() => setSuccessMessage(''), 5000);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to approve request';
      setError(errorMessage);
    } finally {
      setActionLoading(null);
    }
  };

  const handleRejectRequest = async (request: VetApprovalRequest) => {
    setActionLoading(request.id);
    setError('');
    try {
      await apiClient.post(`/rescue-org/approval-requests/${request.id}/reject`, {
        reason: rejectReason || null,
      });
      setSuccessMessage(`Request from ${request.vetClinicName} has been rejected`);
      setApprovalRequests((prev) => prev.filter((r) => r.id !== request.id));
      setRejectingRequestId(null);
      setRejectReason('');
      setTimeout(() => setSuccessMessage(''), 5000);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to reject request';
      setError(errorMessage);
    } finally {
      setActionLoading(null);
    }
  };

  const formatLocation = (vet: Vet) => {
    if (vet.city && vet.state) return `${vet.city}, ${vet.state}`;
    return vet.city || vet.state || 'Location not specified';
  };

  const formatRequestLocation = (request: VetApprovalRequest) => {
    if (request.vetCity && request.vetState) return `${request.vetCity}, ${request.vetState}`;
    return request.vetCity || request.vetState || 'Location not specified';
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  return (
    <div className="container-app py-8">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Manage Vets</h1>
          <p className="text-gray-600">Approve vets to verify pets for your organization</p>
        </div>
        <Link to="/rescue">
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
          {/* Approval Requests */}
          {approvalRequests.length > 0 && (
            <section>
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-xl font-semibold text-gray-900">Approval Requests</h2>
                <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-warning-100 text-warning-800">
                  {approvalRequests.length} pending
                </span>
              </div>
              <div className="card divide-y divide-secondary-200">
                {approvalRequests.map((request) => (
                  <div key={request.id} className="p-4">
                    <div className="flex items-start justify-between">
                      <div className="flex items-center gap-4">
                        <div className="w-12 h-12 bg-warning-100 rounded-full flex items-center justify-center">
                          <svg className="w-6 h-6 text-warning-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                          </svg>
                        </div>
                        <div>
                          <p className="font-medium text-gray-900">{request.vetClinicName}</p>
                          <p className="text-sm text-gray-500">{formatRequestLocation(request)}</p>
                          {request.vetLicenseNumber && (
                            <p className="text-xs text-gray-400">License: {request.vetLicenseNumber}</p>
                          )}
                          <p className="text-xs text-gray-400">Requested {formatDate(request.requestedAt)}</p>
                        </div>
                      </div>
                      <div className="flex gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setRejectingRequestId(request.id)}
                          loading={actionLoading === request.id}
                        >
                          Reject
                        </Button>
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={() => handleApproveRequest(request)}
                          loading={actionLoading === request.id}
                        >
                          Approve
                        </Button>
                      </div>
                    </div>
                    {request.message && (
                      <div className="mt-3 ml-16 text-sm text-gray-600 bg-gray-50 p-3 rounded">
                        <span className="font-medium">Message:</span> {request.message}
                      </div>
                    )}
                    {rejectingRequestId === request.id && (
                      <div className="mt-3 ml-16 space-y-2">
                        <textarea
                          value={rejectReason}
                          onChange={(e) => setRejectReason(e.target.value)}
                          className="input min-h-16 w-full"
                          placeholder="Reason for rejection (optional)..."
                        />
                        <div className="flex gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => {
                              setRejectingRequestId(null);
                              setRejectReason('');
                            }}
                          >
                            Cancel
                          </Button>
                          <Button
                            variant="danger"
                            size="sm"
                            onClick={() => handleRejectRequest(request)}
                            loading={actionLoading === request.id}
                          >
                            Confirm Rejection
                          </Button>
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </section>
          )}

          {/* Approved Vets */}
          <section>
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-semibold text-gray-900">Approved Vets</h2>
              <span className="text-sm text-gray-500">{approvedVets.length} approved</span>
            </div>
            {approvedVets.length > 0 ? (
              <div className="card divide-y divide-secondary-200">
                {approvedVets.map((vet) => (
                  <div key={vet.id} className="p-4 flex items-center justify-between">
                    <div className="flex items-center gap-4">
                      <div className="w-12 h-12 bg-success-100 rounded-full flex items-center justify-center">
                        <svg className="w-6 h-6 text-success-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                      </div>
                      <div>
                        <p className="font-medium text-gray-900">{vet.clinicName}</p>
                        <p className="text-sm text-gray-500">{formatLocation(vet)}</p>
                        <p className="text-xs text-gray-400">License: {vet.licenseNumber}</p>
                      </div>
                    </div>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleRevokeVet(vet)}
                      loading={actionLoading === vet.id}
                    >
                      Revoke
                    </Button>
                  </div>
                ))}
              </div>
            ) : (
              <div className="card p-8 text-center text-gray-500">
                No vets approved yet. Approve vets from the pending list below.
              </div>
            )}
          </section>

          {/* Pending Vets */}
          <section>
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-semibold text-gray-900">Available Vets</h2>
              <span className="text-sm text-gray-500">{pendingVets.length} available to approve</span>
            </div>
            {pendingVets.length > 0 ? (
              <div className="card divide-y divide-secondary-200">
                {pendingVets.map((vet) => (
                  <div key={vet.id} className="p-4 flex items-center justify-between">
                    <div className="flex items-center gap-4">
                      <div className="w-12 h-12 bg-secondary-100 rounded-full flex items-center justify-center">
                        <svg className="w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                        </svg>
                      </div>
                      <div>
                        <p className="font-medium text-gray-900">{vet.clinicName}</p>
                        <p className="text-sm text-gray-500">{formatLocation(vet)}</p>
                        <p className="text-xs text-gray-400">License: {vet.licenseNumber}</p>
                        {vet.verified && (
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-primary-100 text-primary-700">
                            Verified
                          </span>
                        )}
                      </div>
                    </div>
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => handleApproveVet(vet)}
                      loading={actionLoading === vet.id}
                    >
                      Approve
                    </Button>
                  </div>
                ))}
              </div>
            ) : (
              <div className="card p-8 text-center text-gray-500">
                No vets available to approve at this time
              </div>
            )}
          </section>
        </div>
      )}
    </div>
  );
}
