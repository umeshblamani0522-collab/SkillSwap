import React from 'react'

export default function MatchBadge({ percentage }) {
  const value = Math.round(percentage ?? 0)
  let bg = 'bg-secondary'
  if (value >= 80) bg = 'bg-success'
  else if (value >= 60) bg = 'bg-primary'
  else if (value >= 40) bg = 'bg-warning text-dark'
  else bg = 'bg-danger'

  return <span className={`se-match-badge text-white ${bg}`}>{value}% match</span>
}
