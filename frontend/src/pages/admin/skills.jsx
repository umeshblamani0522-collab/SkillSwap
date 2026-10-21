import React, { useEffect, useState } from 'react'
import { skillService } from '../../services/skills'
import { adminService } from '../../services/admin'
import { useToast } from '../../components/common/toast'
import LoadingSpinner from '../../components/common/loading-spinner'
import EmptyState from '../../components/common/empty-state'

export default function SkillManagement() {
  const { push } = useToast()
  const [skills, setSkills] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)
  const [newSkill, setNewSkill] = useState({ name: '', categoryId: '' })
  const [newCategory, setNewCategory] = useState({ name: '', description: '' })

  const load = async () => {
    setLoading(true)
    setError('')
    try {
      const [s, c] = await Promise.all([skillService.search('', 0, 200), skillService.categories()])
      setSkills(s.content || [])
      setCategories(c || [])
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to load the skill catalogue.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const handleAddSkill = async (e) => {
    e.preventDefault()
    try {
      await adminService.createSkill({ name: newSkill.name.trim(), categoryId: newSkill.categoryId || null })
      push('Skill created successfully')
      setNewSkill({ name: '', categoryId: '' })
      load()
    } catch (err) {
      push(err.response?.data?.message || 'Could not create skill', 'danger')
    }
  }

  const handleAddCategory = async (e) => {
    e.preventDefault()
    try {
      await adminService.createCategory({ name: newCategory.name.trim(), description: newCategory.description.trim() })
      push('Category created successfully')
      setNewCategory({ name: '', description: '' })
      load()
    } catch (err) {
      push(err.response?.data?.message || 'Could not create category', 'danger')
    }
  }

  const toggleSkill = async (skill) => {
    setBusyId(skill.id)
    try {
      if (skill.active) await adminService.deactivateSkill(skill.id)
      else await adminService.activateSkill(skill.id)
      push(skill.active ? 'Skill deactivated' : 'Skill activated')
      await load()
    } catch (err) {
      push(err.response?.data?.message || 'Could not update skill status', 'danger')
    } finally {
      setBusyId(null)
    }
  }

  if (loading) return <LoadingSpinner label="Loading skill catalogue..." />
  if (error) return <div className="se-alert danger">{error}</div>

  return (
    <div>
      <div className="d-flex justify-content-between align-items-end flex-wrap gap-2 mb-4">
        <div>
          <span className="eyebrow muted">Platform catalogue</span>
          <h2 className="mb-1">Skills & categories</h2>
          <p className="text-muted mb-0">Maintain the skills members can add to their profiles and matching preferences.</p>
        </div>
      </div>

      <div className="row g-4 mb-4">
        <div className="col-lg-6">
          <div className="se-card p-4 h-100">
            <h6 className="mb-3">Add skill category</h6>
            <form onSubmit={handleAddCategory} className="d-flex flex-column gap-2">
              <input className="form-control" placeholder="Category name" required
                value={newCategory.name} onChange={(e) => setNewCategory({ ...newCategory, name: e.target.value })} />
              <textarea className="form-control" rows={2} placeholder="Optional description"
                value={newCategory.description} onChange={(e) => setNewCategory({ ...newCategory, description: e.target.value })} />
              <button className="btn btn-primary se-btn-primary align-self-start">Create category</button>
            </form>
          </div>
        </div>
        <div className="col-lg-6">
          <div className="se-card p-4 h-100">
            <h6 className="mb-3">Add skill</h6>
            <form onSubmit={handleAddSkill} className="d-flex flex-column gap-2">
              <input className="form-control" placeholder="Skill name" required
                value={newSkill.name} onChange={(e) => setNewSkill({ ...newSkill, name: e.target.value })} />
              <select className="form-select" value={newSkill.categoryId}
                onChange={(e) => setNewSkill({ ...newSkill, categoryId: e.target.value })}>
                <option value="">No category</option>
                {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
              <button className="btn btn-primary se-btn-primary align-self-start">Create skill</button>
            </form>
          </div>
        </div>
      </div>

      <div className="se-card">
        <div className="card-heading p-4 pb-3">
          <div>
            <span className="card-kicker">Catalogue</span>
            <h2>Available skills</h2>
          </div>
          <span className="text-muted small">{skills.length} skills</span>
        </div>
        {skills.length === 0 ? (
          <div className="p-4 pt-0"><EmptyState title="No skills in the catalogue" /></div>
        ) : (
          <div className="table-responsive">
            <table className="table mb-0 align-middle">
              <thead><tr><th>Skill</th><th>Category</th><th>Status</th><th className="text-end">Action</th></tr></thead>
              <tbody>
                {skills.map((s) => (
                  <tr key={s.id}>
                    <td>{s.name}</td>
                    <td>{s.categoryName || '—'}</td>
                    <td>{s.active ? <span className="badge bg-success">Active</span> : <span className="badge bg-secondary">Inactive</span>}</td>
                    <td className="text-end">
                      <button className={`btn btn-sm ${s.active ? 'btn-outline-danger' : 'btn-outline-success'}`} disabled={busyId === s.id}
                        onClick={() => toggleSkill(s)}>
                        {busyId === s.id ? 'Saving...' : (s.active ? 'Deactivate' : 'Activate')}
                      </button>
                    </td>
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
