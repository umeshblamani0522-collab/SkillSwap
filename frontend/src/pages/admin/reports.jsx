import React, { useEffect, useMemo, useState } from 'react'
import {
  FaFlag,
  FaClock,
  FaCheckCircle,
  FaTimesCircle,
  FaSearch,
  FaCalendarAlt,
  FaFileAlt,
  FaUser,
  FaArrowRight,
} from 'react-icons/fa'

import { reportService } from '../../services/reports'
import { useToast } from '../../components/common/toast'
import LoadingSpinner from '../../components/common/loading-spinner'

const STATUSES = ['OPEN', 'UNDER_REVIEW', 'RESOLVED', 'REJECTED']

const statusConfig = {
  OPEN: {
    label: 'Open Reports',
    shortLabel: 'OPEN',
    subtitle: 'Awaiting review',
    icon: <FaFlag />,
    className: 'open',
  },
  UNDER_REVIEW: {
    label: 'Under Review',
    shortLabel: 'UNDER REVIEW',
    subtitle: 'Currently reviewing',
    icon: <FaClock />,
    className: 'review',
  },
  RESOLVED: {
    label: 'Resolved',
    shortLabel: 'RESOLVED',
    subtitle: 'Issues resolved',
    icon: <FaCheckCircle />,
    className: 'resolved',
  },
  REJECTED: {
    label: 'Rejected',
    shortLabel: 'REJECTED',
    subtitle: 'Not actioned',
    icon: <FaTimesCircle />,
    className: 'rejected',
  },
}

export default function ReportManagement() {
  const { push } = useToast()

  const [status, setStatus] = useState('OPEN')
  const [reports, setReports] = useState([])
  const [counts, setCounts] = useState({
    OPEN: 0,
    UNDER_REVIEW: 0,
    RESOLVED: 0,
    REJECTED: 0,
  })

  const [loading, setLoading] = useState(true)
  const [countsLoading, setCountsLoading] = useState(true)
  const [notes, setNotes] = useState({})
  const [search, setSearch] = useState('')
  const [timeFilter, setTimeFilter] = useState('ALL')

  const loadReports = async (selectedStatus) => {
    setLoading(true)

    try {
      const response = await reportService.byStatus(selectedStatus, 0, 50)
      setReports(response.content || [])
    } catch (err) {
      push(
        err.response?.data?.message ||
          'Unable to load reports.',
        'danger'
      )
      setReports([])
    } finally {
      setLoading(false)
    }
  }

  const loadCounts = async () => {
    setCountsLoading(true)

    try {
      const results = await Promise.all(
        STATUSES.map((item) =>
          reportService.byStatus(item, 0, 1)
        )
      )

      const nextCounts = {}

      STATUSES.forEach((item, index) => {
        nextCounts[item] =
          results[index]?.totalElements ??
          results[index]?.content?.length ??
          0
      })

      setCounts(nextCounts)
    } catch (err) {
      console.error('Could not load report counts', err)
    } finally {
      setCountsLoading(false)
    }
  }

  useEffect(() => {
    loadReports(status)
  }, [status])

  useEffect(() => {
    loadCounts()
  }, [])

  const handleResolve = async (id, newStatus) => {
    try {
      await reportService.resolve(id, {
        status: newStatus,
        resolutionNotes: notes[id] || '',
      })

      push(`Report marked ${newStatus}`)

      await loadReports(status)
      await loadCounts()
    } catch (err) {
      push(
        err.response?.data?.message ||
          'Could not update report',
        'danger'
      )
    }
  }

  const filteredReports = useMemo(() => {
    const query = search.trim().toLowerCase()

    const now = new Date()

    let cutoffDate = null

    if (timeFilter === 'TODAY') {
      cutoffDate = new Date()
      cutoffDate.setHours(0, 0, 0, 0)
    }

    if (timeFilter === '7_DAYS') {
      cutoffDate = new Date()
      cutoffDate.setDate(cutoffDate.getDate() - 7)
    }

    if (timeFilter === '30_DAYS') {
      cutoffDate = new Date()
      cutoffDate.setDate(cutoffDate.getDate() - 30)
    }

    if (timeFilter === '90_DAYS') {
      cutoffDate = new Date()
      cutoffDate.setDate(cutoffDate.getDate() - 90)
    }

    return reports.filter((report) => {
      // Search filter
      const matchesSearch =
        !query ||
        String(report.reporterUsername || '')
          .toLowerCase()
          .includes(query) ||
        String(report.reason || '')
          .toLowerCase()
          .includes(query) ||
        String(report.targetType || '')
          .toLowerCase()
          .includes(query) ||
        String(report.targetId || '')
          .toLowerCase()
          .includes(query) ||
        String(report.description || '')
          .toLowerCase()
          .includes(query)

      // Date filter
      const reportDate = report.createdAt
        ? new Date(report.createdAt)
        : null

      const matchesDate =
        !cutoffDate ||
        (reportDate && reportDate >= cutoffDate && reportDate <= now)

      return matchesSearch && matchesDate
    })
  }, [reports, search, timeFilter])

  const currentConfig = statusConfig[status]

  return (
    <div className="admin-reports-page">

      {/* PAGE HEADER */}
      <section className="reports-hero">
        <div className="reports-hero-content">
          <div className="reports-hero-icon">
            <FaFlag />
          </div>

          <div>
            <h1>Reports</h1>
            <p>
              Review and manage user reports to keep the platform
              safe, fair and positive.
            </p>
          </div>
        </div>

        <div className="reports-hero-decoration">
          <div className="report-paper report-paper-back">
            <FaFileAlt />
          </div>

          <div className="report-paper report-paper-front">
            <FaFlag />
          </div>
        </div>
      </section>

      {/* STAT CARDS */}
      <section className="report-stat-grid">
        {STATUSES.map((item) => {
          const config = statusConfig[item]

          return (
            <button
              key={item}
              type="button"
              className={`report-stat-card ${config.className} ${
                status === item ? 'selected' : ''
              }`}
              onClick={() => setStatus(item)}
            >
              <div className="report-stat-icon">
                {config.icon}
              </div>

              <div className="report-stat-content">
                <span>{config.label}</span>

                <strong>
                  {countsLoading ? '—' : counts[item]}
                </strong>

                <small>{config.subtitle}</small>
              </div>

              <span className="report-stat-badge">
                {countsLoading ? '—' : counts[item]}
              </span>
            </button>
          )
        })}
      </section>

      {/* FILTER ROW */}
      <section className="reports-toolbar">



        <div className="reports-filters">

          <div className="report-search">
            <FaSearch />

            <input
              type="text"
              placeholder="Search by user, reason, type or ID..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>

          <div className="report-time-filter">
            <FaCalendarAlt />

            <select
              value={timeFilter}
              onChange={(e) => setTimeFilter(e.target.value)}
              aria-label="Filter reports by date"
            >
              <option value="ALL">All time</option>
              <option value="TODAY">Today</option>
              <option value="7_DAYS">Last 7 days</option>
              <option value="30_DAYS">Last 30 days</option>
              <option value="90_DAYS">Last 90 days</option>
            </select>
          </div>

        </div>

      </section>

      {/* REPORT CONTENT */}
      <section className="reports-content-card">

        {loading ? (
          <div className="reports-loading">
            <LoadingSpinner label="Loading reports..." />
          </div>
        ) : filteredReports.length === 0 ? (

          <div className="reports-empty-state">

            <div className="reports-empty-illustration">
              <div className="empty-paper">
                <FaFlag />
              </div>

              <span className="spark spark-one">✦</span>
              <span className="spark spark-two">✦</span>
              <span className="spark spark-three">•</span>
            </div>

            <h2>
              No {currentConfig.shortLabel.toLowerCase()} reports
            </h2>

            <p>
              {status === 'OPEN'
                ? 'There are currently no reports awaiting review.'
                : `There are currently no reports marked ${currentConfig.label.toLowerCase()}.`}
            </p>

            {status === 'OPEN' && (
              <span>
                The platform is running smoothly.
              </span>
            )}

          </div>

        ) : (

          <div className="reports-list">

            {filteredReports.map((report) => (
              <article
                className="report-item"
                key={report.id}
              >

                <div className="report-item-top">

                  <div className="report-target">
                    <FaFileAlt />

                    <span>
                      {report.targetType} #{report.targetId}
                    </span>
                  </div>

                  <time>
                    {report.createdAt
                      ? new Date(report.createdAt).toLocaleDateString(
                          'en-GB',
                          {
                            day: '2-digit',
                            month: 'short',
                            year: 'numeric',
                          }
                        )
                      : '—'}
                  </time>

                </div>

                <div className="report-reporter">
                  <FaUser />
                  <strong>Reported by</strong>
                  <span>{report.reporterUsername || 'Unknown user'}</span>
                </div>

                <div className="report-reason">
                  {report.reason}
                </div>

                {report.resolutionNotes && (
                  <div className="report-notes">
                    <strong>Resolution notes:</strong>
                    <span>{report.resolutionNotes}</span>
                  </div>
                )}

                {(status === 'OPEN' ||
                  status === 'UNDER_REVIEW') && (

                  <div className="report-actions">

                    <input
                      type="text"
                      placeholder="Resolution notes..."
                      value={notes[report.id] || ''}
                      onChange={(e) =>
                        setNotes({
                          ...notes,
                          [report.id]: e.target.value,
                        })
                      }
                    />

                    <div className="report-action-buttons">

                      {status === 'OPEN' && (
                        <button
                          type="button"
                          className="review-button"
                          onClick={() =>
                            handleResolve(
                              report.id,
                              'UNDER_REVIEW'
                            )
                          }
                        >
                          Start review
                        </button>
                      )}

                      <button
                        type="button"
                        className="resolve-button"
                        onClick={() =>
                          handleResolve(
                            report.id,
                            'RESOLVED'
                          )
                        }
                      >
                        Resolve
                      </button>

                      <button
                        type="button"
                        className="reject-button"
                        onClick={() =>
                          handleResolve(
                            report.id,
                            'REJECTED'
                          )
                        }
                      >
                        Reject
                      </button>

                    </div>
                  </div>
                )}

              </article>
            ))}

          </div>
        )}

      </section>

      {/* FOOTER SUMMARY */}
      <div className="reports-footer-summary">
        <span>
          Showing {filteredReports.length} report
          {filteredReports.length !== 1 ? 's' : ''}
        </span>

        <span>
          {currentConfig.label}
          <FaArrowRight />
        </span>
      </div>

    </div>
  )
}