import React from 'react'

export default function EmptyState({ icon, title, subtitle }) {
  return (
    <div className="se-empty-state">
      {icon && <div className="fs-1 mb-2">{icon}</div>}
      <h5>{title}</h5>
      {subtitle && <p className="mb-0">{subtitle}</p>}
    </div>
  )
}
