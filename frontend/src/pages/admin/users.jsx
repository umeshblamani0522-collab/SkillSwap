
import React, { useEffect, useState } from 'react'
import { adminService } from '../../services/admin'
import { useToast } from '../../components/common/toast'
import LoadingSpinner from '../../components/common/loading-spinner'

export default function UserManagement() {
  const { push } = useToast()
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)

  const load = async () => {
    setLoading(true)

    try {
      const res = await adminService.users(0, 100)
      setUsers(res.content || [])
    } catch (err) {
      push(
        err.response?.data?.message || 'Failed to load users',
        'danger'
      )
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  // Suspend or restore a user account.
  const toggleSuspend = async (u) => {
    try {
      if (u.suspended) {
        await adminService.unsuspend(
          u.id,
          'Reinstated by admin'
        )
      } else {
        await adminService.suspend(
          u.id,
          'Suspended by admin via console'
        )
      }

      push(
        `User ${u.suspended ? 'unsuspended' : 'suspended'}`
      )

      await load()
    } catch (err) {
      push(
        err.response?.data?.message || 'Action failed',
        'danger'
      )
    }
  }

  // Permanently delete a user account without blocking the email.
  const handleDelete = async (u) => {
    const confirmed = window.confirm(
      `Permanently delete the account for ${u.username} (${u.email})? ` +
      'This action cannot be undone. The email will remain available ' +
      'for future registration.'
    )

    if (!confirmed) return

    try {
      await adminService.deleteUser(u.id)

      push('User account permanently deleted')
      await load()
    } catch (err) {
      push(
        err.response?.data?.message || 'Account deletion failed',
        'danger'
      )
    }
  }

  if (loading) {
    return <LoadingSpinner />
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-4">
        <div>
          <h2 className="mb-1">User Management</h2>
          <p className="text-muted mb-0">
            View users and manage their accounts.
          </p>
        </div>

        <span className="badge bg-primary">
          Total users: {users.length}
        </span>
      </div>

      <div className="se-card">
        <div className="table-responsive">
          <table className="table mb-0 align-middle">
            <thead>
              <tr>
                <th>ID</th>
                <th>Username</th>
                <th>Email</th>
                <th>Roles</th>
                <th>Status</th>
                <th>Joined</th>
                <th className="text-end">Actions</th>
              </tr>
            </thead>

            <tbody>
              {users.length === 0 ? (
                <tr>
                  <td
                    colSpan="7"
                    className="text-center py-4 text-muted"
                  >
                    No users found.
                  </td>
                </tr>
              ) : (
                users.map((u) => (
                  <tr key={u.id}>
                    <td>{u.id}</td>

                    <td>{u.username}</td>

                    <td>{u.email}</td>

                    <td>
                      {(u.roles || []).join(', ') || '—'}
                    </td>

                    <td>
                      {u.suspended ? (
                        <span className="badge bg-danger">
                          Suspended
                        </span>
                      ) : u.enabled === false ? (
                        <span className="badge bg-secondary">
                          Disabled
                        </span>
                      ) : (
                        <span className="badge bg-success">
                          Active
                        </span>
                      )}
                    </td>

                    <td>
                      {u.createdAt
                        ? new Date(
                            u.createdAt
                          ).toLocaleDateString()
                        : '—'}
                    </td>

                    <td>
                      <div className="d-flex justify-content-end flex-wrap gap-2">
                        <button
                          type="button"
                          className={`btn btn-sm ${
                            u.suspended
                              ? 'btn-outline-success'
                              : 'btn-outline-danger'
                          }`}
                          onClick={() => toggleSuspend(u)}
                        >
                          {u.suspended
                            ? 'Unsuspend'
                            : 'Suspend'}
                        </button>

                        <button
                          type="button"
                          className="btn btn-sm btn-outline-dark"
                          onClick={() => handleDelete(u)}
                        >
                          Delete Account
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
