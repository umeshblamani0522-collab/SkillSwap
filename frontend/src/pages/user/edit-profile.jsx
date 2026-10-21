import React, { useEffect, useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/auth'
import { userService } from '../../services/users'
import { uploadService } from '../../services/uploads'
import { useToast } from '../../components/common/toast'
import { FaUserEdit, FaSave, FaArrowLeft, FaCamera } from 'react-icons/fa'

export default function EditProfile() {
  const { user, refreshProfile } = useAuth()
  const { push } = useToast()
  const navigate = useNavigate()
  const fileInputRef = useRef(null)

  const [form, setForm] = useState({
    fullName: '', phone: '', bio: '', location: '', profilePictureUrl: '', languages: '', communicationLanguage: '', experienceYears: '',
  })
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)

  useEffect(() => {
    if (user) {
      setForm({
        fullName: user.fullName || '',
        phone: user.phone || '',
        bio: user.bio || '',
        location: user.location || '',
        profilePictureUrl: user.profilePictureUrl || '',
        languages: user.languages || '',
        communicationLanguage: user.communicationLanguage || '',
        experienceYears: user.experienceYears ?? '',
      })
    }
  }, [user])

  const handleFileChange = async (e) => {
    const file = e.target.files[0]
    if (!file) return

    setUploading(true)
    try {
      const data = await uploadService.uploadFile(file)
      setForm({ ...form, profilePictureUrl: data.url })
      push('Photo uploaded successfully')
    } catch (err) {
      push('Failed to upload photo', 'danger')
    } finally {
      setUploading(false)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      await userService.updateMe({ ...form, experienceYears: form.experienceYears === '' ? null : Number(form.experienceYears) })
      await refreshProfile()
      push('Profile updated successfully')
      navigate('/profile')
    } catch (err) {
      push(err.response?.data?.message || 'Failed to update profile', 'danger')
      setSaving(false)
    }
  }

  return (
    <div className="container-fluid p-0">
      <div className="se-dash-header animate-slide-up mb-5 d-flex justify-content-between align-items-center flex-wrap gap-3">
        <div>
          <h2 className="mb-1 fw-bold">Edit <span className="se-gradient-text">Profile</span></h2>
          <p className="text-muted mb-0">Update your personal details and how you appear to others.</p>
        </div>
        <div className="d-flex align-items-center gap-3">
          <button className="btn btn-light rounded-pill px-4 py-2 fw-semibold border shadow-sm transition-all" onClick={() => navigate('/profile')}>
            <FaArrowLeft className="me-2" /> Back
          </button>
          <div className="se-icon-box primary rounded-circle d-none d-sm-flex" style={{ width: '64px', height: '64px', fontSize: '2rem' }}>
            <FaUserEdit />
          </div>
        </div>
      </div>

      <div className="se-card p-4 p-md-5 animate-slide-up delay-100 border-0 shadow-sm mx-auto" style={{ maxWidth: 800 }}>
        <form onSubmit={handleSubmit} className="d-flex flex-column gap-4">

          <div className="d-flex flex-column align-items-center mb-4 border-bottom pb-4">
            <div
              className="position-relative"
              style={{ width: '120px', height: '120px', cursor: 'pointer' }}
              onClick={() => fileInputRef.current?.click()}
            >
              <img
                src={form.profilePictureUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(user?.username || 'U')}&size=120&background=4f46e5&color=fff`}
                alt="Profile"
                className={`rounded-circle shadow-sm border border-4 border-white transition-all ${uploading ? 'opacity-50' : 'hover-shadow-md'}`}
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
              />
              <div
                className="position-absolute bottom-0 end-0 bg-primary text-white rounded-circle d-flex align-items-center justify-content-center shadow-sm"
                style={{ width: '36px', height: '36px', border: '3px solid white' }}
              >
                <FaCamera size={14} />
              </div>
              <input
                type="file"
                ref={fileInputRef}
                className="d-none"
                accept="image/jpeg,image/png"
                onChange={handleFileChange}
              />
            </div>
            <div className="text-muted small mt-2 fw-medium">Click to change photo</div>
            {uploading && <div className="text-primary small mt-1 fw-bold">Uploading...</div>}
          </div>

          <div className="row g-4">
            <div className="col-12">
              <label className="form-label small fw-bold text-uppercase tracking-wide text-muted">Full Name</label>
              <input className="form-control bg-light border-0 shadow-none py-2 px-3 fs-6" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} placeholder="John Doe" />
            </div>

            <div className="col-md-6">
              <label className="form-label small fw-bold text-uppercase tracking-wide text-muted">Phone Number</label>
              <input className="form-control bg-light border-0 shadow-none py-2 px-3 fs-6" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} placeholder="+1 234 567 890" />
            </div>

            <div className="col-md-6">
              <label className="form-label small fw-bold text-uppercase tracking-wide text-muted">Location</label>
              <input className="form-control bg-light border-0 shadow-none py-2 px-3 fs-6" value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} placeholder="City, Country" />
            </div>
          </div>

          <div>
            <label className="form-label small fw-bold text-uppercase tracking-wide text-muted">Bio</label>
            <textarea className="form-control bg-light border-0 shadow-none py-3 px-3 fs-6" rows={5} value={form.bio} onChange={(e) => setForm({ ...form, bio: e.target.value })} placeholder="Tell the community a little about yourself..." />
          </div>

          <div className="row g-4">
            <div className="col-md-6">
              <label className="form-label small fw-bold text-uppercase tracking-wide text-muted">Languages</label>
              <input className="form-control bg-light border-0 shadow-none py-2 px-3 fs-6" placeholder="English, Spanish" value={form.languages}
                onChange={(e) => setForm({ ...form, languages: e.target.value })} />
              <div className="form-text mt-2 small text-muted">Languages you know (comma-separated)</div>
            </div>
            <div className="col-md-6">
              <label className="form-label small fw-bold text-uppercase tracking-wide text-muted">Communication Language</label>
              <select className="form-select bg-light border-0 shadow-none py-2 px-3 fs-6" value={form.communicationLanguage}
                onChange={(e) => setForm({ ...form, communicationLanguage: e.target.value })}>
                <option value="">Select your preferred language</option>
                <option value="English">English</option>
                <option value="Hindi">Hindi</option>
                <option value="Kannada">Kannada</option>
                <option value="Telugu">Telugu</option>
                <option value="Tamil">Tamil</option>
                <option value="Malayalam">Malayalam</option>
                <option value="Marathi">Marathi</option>
                <option value="Bengali">Bengali</option>
                <option value="Urdu">Urdu</option>
                <option value="Other">Other</option>
              </select>
              <div className="form-text mt-2 small text-muted">Used to help partners understand how you prefer to communicate.</div>
            </div>
          </div>

          <div className="row g-4">
            <div className="col-md-6">
              <label className="form-label small fw-bold text-uppercase tracking-wide text-muted">Years of Experience</label>
              <input type="number" min="0" className="form-control bg-light border-0 shadow-none py-2 px-3 fs-6" value={form.experienceYears}
                onChange={(e) => setForm({ ...form, experienceYears: e.target.value })} placeholder="e.g. 5" />
            </div>
          </div>

          <div className="pt-3 border-top mt-2 d-flex justify-content-end">
            <button type="submit" className="btn se-btn-primary rounded-pill px-5 py-3 fw-semibold shadow-sm d-flex align-items-center gap-2" disabled={saving || uploading}>
              <FaSave /> {saving ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
