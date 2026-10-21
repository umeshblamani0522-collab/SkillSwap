import React, { useEffect, useState } from 'react'
import { FaCalendarAlt, FaPlus, FaTrash } from 'react-icons/fa'
import { availabilityService } from '../../services/availability'
import { useToast } from '../../components/common/toast'
import LoadingSpinner from '../../components/common/loading-spinner'

const DAYS = ['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY']

export default function Availability() {
  const { push } = useToast()
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [form, setForm] = useState({ dayOfWeek: 'MONDAY', startTime: '18:00', endTime: '20:00', timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC' })

  const load = async () => {
    try { setItems(await availabilityService.list()) }
    catch (err) { push(err.response?.data?.message || 'Could not load availability', 'danger') }
    finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const add = async (e) => {
    e.preventDefault(); setSaving(true)
    try {
      const created = await availabilityService.add(form)
      setItems(prev => [...prev, created].sort((a,b) => `${a.dayOfWeek}-${a.startTime}`.localeCompare(`${b.dayOfWeek}-${b.startTime}`)))
      push('Availability added')
    } catch (err) { push(err.response?.data?.message || 'Could not add availability', 'danger') }
    finally { setSaving(false) }
  }

  const remove = async (id) => {
    try { await availabilityService.remove(id); setItems(prev => prev.filter(x => x.id !== id)); push('Availability removed') }
    catch (err) { push(err.response?.data?.message || 'Could not remove availability', 'danger') }
  }

  if (loading) return <LoadingSpinner />

  return (
    <div className="container-fluid p-0">
      <div className="page-hero dashboard-hero mb-4">
        <div>
          <span className="eyebrow muted">Matching preferences</span>
          <h1>Availability</h1>
          <p>Set the recurring time windows when you are normally available. SkillSwap uses these windows when calculating compatibility.</p>
        </div>
        <div className="se-icon-box primary rounded-circle"><FaCalendarAlt /></div>
      </div>

      <div className="row g-4">
        <div className="col-lg-5">
          <div className="content-card p-4">
            <h2 className="h5 fw-bold mb-3">Add a time window</h2>
            <form onSubmit={add} className="d-flex flex-column gap-3">
              <label className="form-label mb-0">Day
                <select className="form-select mt-1" value={form.dayOfWeek} onChange={e => setForm({...form, dayOfWeek: e.target.value})}>
                  {DAYS.map(day => <option key={day} value={day}>{day[0] + day.slice(1).toLowerCase()}</option>)}
                </select>
              </label>
              <div className="row g-3">
                <div className="col-6"><label className="form-label mb-0">Start<input className="form-control mt-1" type="time" required value={form.startTime} onChange={e => setForm({...form, startTime: e.target.value})} /></label></div>
                <div className="col-6"><label className="form-label mb-0">End<input className="form-control mt-1" type="time" required value={form.endTime} onChange={e => setForm({...form, endTime: e.target.value})} /></label></div>
              </div>
              <label className="form-label mb-0">Timezone
                <input className="form-control mt-1" maxLength={60} value={form.timezone} onChange={e => setForm({...form, timezone: e.target.value})} />
              </label>
              <button className="se-btn se-btn-primary" disabled={saving}><FaPlus /> {saving ? 'Adding…' : 'Add availability'}</button>
            </form>
          </div>
        </div>
        <div className="col-lg-7">
          <div className="content-card p-4 h-100">
            <div className="d-flex justify-content-between align-items-center mb-3"><h2 className="h5 fw-bold mb-0">Current schedule</h2><span className="badge text-bg-light">{items.length} windows</span></div>
            {items.length === 0 ? <div className="text-center text-muted py-5">No availability added yet.</div> : items.map(item => (
              <div key={item.id} className="d-flex align-items-center justify-content-between border rounded-4 p-3 mb-2">
                <div><div className="fw-semibold">{item.dayOfWeek[0] + item.dayOfWeek.slice(1).toLowerCase()}</div><div className="text-muted small">{item.startTime} – {item.endTime} · {item.timezone}</div></div>
                <button className="btn btn-light text-danger rounded-circle" title="Remove" onClick={() => remove(item.id)}><FaTrash /></button>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
