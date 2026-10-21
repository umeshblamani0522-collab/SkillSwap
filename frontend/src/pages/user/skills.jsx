import React, { useEffect, useState } from 'react'
import { FaPlus, FaTrash, FaBookOpen, FaChalkboardTeacher } from 'react-icons/fa'
import { useAuth } from '../../context/auth'
import { skillService } from '../../services/skills'
import { useToast } from '../../components/common/toast'
import LoadingSpinner from '../../components/common/loading-spinner'

const LEVELS = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT']

export default function MySkills() {
  const { user, refreshProfile } = useAuth()
  const { push } = useToast()
  const [allSkills, setAllSkills] = useState([])
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState({ skillId: '', skillType: 'TEACH', level: 'INTERMEDIATE', experienceDescription: '' })

  useEffect(() => {
    const load = async () => {
      const res = await skillService.search('', 0, 200)
      setAllSkills(res.content)
      setLoading(false)
    }
    load()
  }, [])

  const handleAdd = async (e) => {
    e.preventDefault()
    if (!form.skillId) return
    try {
      await skillService.addUserSkill({ ...form, skillId: Number(form.skillId) })
      await refreshProfile()
      setForm({ ...form, skillId: '' })
      push('Skill added')
    } catch (err) {
      push(err.response?.data?.message || 'Could not add skill', 'danger')
    }
  }

  const handleDelete = async (id) => {
    try {
      await skillService.deleteUserSkill(id)
      await refreshProfile()
      push('Skill removed')
    } catch (err) {
      push(err.response?.data?.message || 'Could not remove skill', 'danger')
    }
  }

  if (loading) return <LoadingSpinner />

  return (
    <div className="container-fluid p-0">
      <div className="se-dash-header animate-slide-up mb-4">
        <h2 className="mb-1 fw-bold">Manage Your <span className="se-gradient-text">Skills</span></h2>
        <p className="text-muted mb-0">Add skills you can teach and skills you want to learn to get matched with others.</p>
      </div>

      <div className="se-card se-card-hover p-4 mb-5 animate-slide-up delay-100 border-0 shadow-sm">
        <h5 className="mb-4 fw-bold d-flex align-items-center gap-2">
          <span className="se-icon-box primary rounded-circle" style={{ width: '32px', height: '32px', fontSize: '1rem' }}><FaPlus /></span>
          Add a New Skill
        </h5>
        <form className="row g-3 align-items-end" onSubmit={handleAdd}>
          <div className="col-md-4">
            <label className="form-label small fw-medium text-muted">Skill</label>
            <select className="form-select bg-light border-0 shadow-none py-2" value={form.skillId} onChange={(e) => setForm({ ...form, skillId: e.target.value })} required>
              <option value="">Select a skill...</option>
              {allSkills.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </div>
          <div className="col-md-2">
            <label className="form-label small fw-medium text-muted">Type</label>
            <select className="form-select bg-light border-0 shadow-none py-2" value={form.skillType} onChange={(e) => setForm({ ...form, skillType: e.target.value })}>
              <option value="TEACH">I can teach</option>
              <option value="LEARN">I want to learn</option>
            </select>
          </div>
          <div className="col-md-2">
            <label className="form-label small fw-medium text-muted">Level</label>
            <select className="form-select bg-light border-0 shadow-none py-2" value={form.level} onChange={(e) => setForm({ ...form, level: e.target.value })}>
              {LEVELS.map((l) => <option key={l} value={l}>{l}</option>)}
            </select>
          </div>
          <div className="col-md-3">
            <label className="form-label small fw-medium text-muted">Notes (optional)</label>
            <input className="form-control bg-light border-0 shadow-none py-2" value={form.experienceDescription}
              onChange={(e) => setForm({ ...form, experienceDescription: e.target.value })} placeholder="E.g., 3 years experience" />
          </div>
          <div className="col-md-1">
            <button className="btn se-btn-primary w-100 py-2 rounded-3 shadow-sm d-flex justify-content-center align-items-center"><FaPlus /></button>
          </div>
        </form>
      </div>

      <div className="row g-4">
        <div className="col-md-6">
          <div className="se-card p-4 h-100 animate-slide-up delay-200 border-0 shadow-sm">
            <div className="d-flex align-items-center gap-3 mb-4">
              <div className="se-icon-box" style={{ background: 'var(--se-primary-light)', color: 'var(--se-primary)' }}>
                <FaChalkboardTeacher />
              </div>
              <h5 className="mb-0 fw-bold">Skills I Teach</h5>
            </div>
            {(user?.teachSkills || []).length === 0 ? (
              <div className="text-center py-5 bg-light rounded-4 border border-light">
                <p className="text-muted mb-0">No teaching skills added yet.</p>
              </div>
            ) : (
              <div className="d-flex flex-column">
                {user.teachSkills.map((s) => (
                  <div key={s.id} className="se-list-item d-flex justify-content-between align-items-center bg-light border-0 mb-3 rounded-4 shadow-sm">
                    <div>
                      <div className="fw-semibold fs-5 mb-1 text-primary">{s.skillName}</div>
                      <div className="d-flex align-items-center gap-2">
                        <span className="badge bg-white text-dark shadow-sm border border-light">{s.level}</span>
                        <span className="text-muted small">{s.categoryName}</span>
                      </div>
                    </div>
                    <button className="btn btn-sm btn-outline-danger rounded-circle d-flex align-items-center justify-content-center border-0 bg-white shadow-sm" style={{ width: '36px', height: '36px', transition: 'all 0.2s' }} onClick={() => handleDelete(s.id)} title="Remove Skill">
                      <FaTrash />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="col-md-6">
          <div className="se-card p-4 h-100 animate-slide-up delay-300 border-0 shadow-sm">
            <div className="d-flex align-items-center gap-3 mb-4">
              <div className="se-icon-box" style={{ background: 'hsl(142, 71%, 95%)', color: 'var(--se-success)' }}>
                <FaBookOpen />
              </div>
              <h5 className="mb-0 fw-bold">Skills I Want to Learn</h5>
            </div>
            {(user?.learnSkills || []).length === 0 ? (
              <div className="text-center py-5 bg-light rounded-4 border border-light">
                <p className="text-muted mb-0">No learning interests added yet.</p>
              </div>
            ) : (
              <div className="d-flex flex-column">
                {user.learnSkills.map((s) => (
                  <div key={s.id} className="se-list-item d-flex justify-content-between align-items-center bg-light border-0 mb-3 rounded-4 shadow-sm">
                    <div>
                      <div className="fw-semibold fs-5 mb-1 text-success">{s.skillName}</div>
                      <div className="d-flex align-items-center gap-2">
                        <span className="text-muted small">{s.categoryName}</span>
                      </div>
                    </div>
                    <button className="btn btn-sm btn-outline-danger rounded-circle d-flex align-items-center justify-content-center border-0 bg-white shadow-sm" style={{ width: '36px', height: '36px', transition: 'all 0.2s' }} onClick={() => handleDelete(s.id)} title="Remove Skill">
                      <FaTrash />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
