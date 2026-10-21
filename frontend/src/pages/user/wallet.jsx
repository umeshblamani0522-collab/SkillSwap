import React, { useEffect, useState } from 'react'
import { FaCoins, FaArrowUp, FaArrowDown, FaWallet } from 'react-icons/fa'
import { creditService } from '../../services/credits'
import LoadingSpinner from '../../components/common/loading-spinner'
import EmptyState from '../../components/common/empty-state'

export default function Wallet() {
  const [wallet, setWallet] = useState(null)
  const [transactions, setTransactions] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const load = async () => {
      const [w, t] = await Promise.all([creditService.wallet(), creditService.transactions(0, 50)])
      setWallet(w)
      setTransactions(t.content)
      setLoading(false)
    }
    load()
  }, [])

  if (loading) return <LoadingSpinner />

  return (
    <div className="container-fluid p-0">
      <div className="se-dash-header animate-slide-up mb-4 d-flex justify-content-between align-items-center flex-wrap gap-3">
        <div>
          <h2 className="mb-1 fw-bold">Skill <span className="se-gradient-text">Wallet</span></h2>
          <p className="text-muted mb-0">Track your earned and spent skill credits.</p>
        </div>
        <div className="se-icon-box warning rounded-circle d-none d-sm-flex" style={{ width: '64px', height: '64px', fontSize: '2rem' }}>
          <FaWallet />
        </div>
      </div>

      <div className="row g-4 mb-5">
        <div className="col-md-4">
          <div className="se-card se-card-hover p-4 text-center h-100 animate-slide-up delay-100 border-0 shadow-sm d-flex flex-column align-items-center justify-content-center">
            <div className="se-icon-box warning rounded-circle mb-3" style={{ width: '56px', height: '56px', fontSize: '1.5rem' }}>
              <FaCoins />
            </div>
            <div className="fs-1 fw-bold text-dark mb-1">{wallet?.balance ?? 0}</div>
            <div className="text-muted fw-medium text-uppercase tracking-wide small">Current Balance</div>
          </div>
        </div>
        <div className="col-md-4">
          <div className="se-card se-card-hover p-4 text-center h-100 animate-slide-up delay-200 border-0 shadow-sm d-flex flex-column align-items-center justify-content-center">
            <div className="se-icon-box success rounded-circle mb-3" style={{ width: '56px', height: '56px', fontSize: '1.5rem' }}>
              <FaArrowUp />
            </div>
            <div className="fs-1 fw-bold text-dark mb-1">{wallet?.totalEarned ?? 0}</div>
            <div className="text-muted fw-medium text-uppercase tracking-wide small">Total Earned</div>
            <div className="text-success small mt-1">From Teaching</div>
          </div>
        </div>
        <div className="col-md-4">
          <div className="se-card se-card-hover p-4 text-center h-100 animate-slide-up delay-300 border-0 shadow-sm d-flex flex-column align-items-center justify-content-center">
            <div className="se-icon-box" style={{ background: 'hsl(348, 83%, 95%)', color: 'var(--se-danger)', width: '56px', height: '56px', fontSize: '1.5rem', borderRadius: '50%', marginBottom: '1rem', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <FaArrowDown />
            </div>
            <div className="fs-1 fw-bold text-dark mb-1">{wallet?.totalSpent ?? 0}</div>
            <div className="text-muted fw-medium text-uppercase tracking-wide small">Total Spent</div>
            <div className="text-danger small mt-1">From Learning</div>
          </div>
        </div>
      </div>

      <div className="se-card p-4 animate-slide-up delay-300 border-0 shadow-sm">
        <h5 className="mb-4 fw-bold">Transaction History</h5>
        {transactions.length === 0 ? (
          <div className="bg-light rounded-4 border border-light py-5">
             <EmptyState title="No transactions yet" subtitle="Complete a session to start earning or spending credits." />
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th className="py-3 px-4 border-0 rounded-start-3 text-muted fw-semibold">Date</th>
                  <th className="py-3 px-4 border-0 text-muted fw-semibold">Type</th>
                  <th className="py-3 px-4 border-0 text-muted fw-semibold">Description</th>
                  <th className="py-3 px-4 border-0 text-end text-muted fw-semibold">Amount</th>
                  <th className="py-3 px-4 border-0 rounded-end-3 text-end text-muted fw-semibold">Balance After</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((t) => (
                  <tr key={t.id} className="border-bottom">
                    <td className="py-3 px-4 text-secondary">{new Date(t.createdAt).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })}</td>
                    <td className="py-3 px-4">
                      <span className="badge bg-light text-secondary border border-secondary border-opacity-25 rounded-pill px-3 py-2 fw-medium text-uppercase" style={{ fontSize: '0.75rem' }}>
                        {t.type.replace('_', ' ')}
                      </span>
                    </td>
                    <td className="py-3 px-4 fw-medium text-dark">{t.description}</td>
                    <td className="py-3 px-4 text-end">
                      <span className={`badge rounded-pill px-3 py-2 fs-6 ${Number(t.amount) >= 0 ? 'bg-success bg-opacity-10 text-success' : 'bg-danger bg-opacity-10 text-danger'}`}>
                        {Number(t.amount) >= 0 ? '+' : ''}{t.amount}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-end fw-bold text-dark fs-5">{t.balanceAfter}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
