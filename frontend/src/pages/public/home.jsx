import React from 'react'
import { Link } from 'react-router-dom'
import {
  FaArrowRight,
  FaCalendarCheck,
  FaComments,
  FaCoins,
  FaExchangeAlt,
  FaGraduationCap,
  FaSearch,
  FaShieldAlt,
  FaStar,
  FaUserFriends,
} from 'react-icons/fa'

const features = [
  {
    icon: <FaUserFriends />,
    title: 'Skill matching',
    text: 'Connect with people who can teach what you want to learn and learn what you can teach.',
  },
  {
    icon: <FaCalendarCheck />,
    title: 'Structured sessions',
    text: 'Schedule sessions at your convenience and keep every learning interaction organised.',
  },
  {
    icon: <FaComments />,
    title: 'Real-time communication',
    text: 'Chat before, during and after sessions so every exchange stays in context.',
  },
  {
    icon: <FaCoins />,
    title: 'Credit-based learning',
    text: 'Earn credits when you teach and spend them when you learn, creating a balanced exchange.',
  },
]

const steps = [
  {
    number: '01',
    icon: <FaGraduationCap />,
    title: 'Build your profile',
    text: 'Add the skills you can teach and the skills you want to learn.',
  },
  {
    number: '02',
    icon: <FaSearch />,
    title: 'Discover people',
    text: 'Find peers with complementary skills and matching interests.',
  },
  {
    number: '03',
    icon: <FaExchangeAlt />,
    title: 'Complete the exchange',
    text: 'Request, schedule, chat, learn and build your reputation.',
  },
]

export default function Landing() {
  return (
    <div className="landing-page landing-reference">
      <section className="landing-hero reference-hero">
        <div className="reference-hero-bg" aria-hidden="true" />
        <div className="reference-hero-overlay" aria-hidden="true" />

        <div className="se-container reference-hero-content">
          <div className="reference-hero-copy">
            <div className="reference-eyebrow">
              <span className="reference-dot" />
              Your learning workspace
            </div>
            <h1>Learn with people.<br />Grow by teaching.</h1>
            <p>
              Find relevant peers, exchange practical skills, and keep every learning interaction in one place.
            </p>
            <div className="reference-actions">
              <Link to="/register" className="se-btn se-btn-primary se-btn-lg">
                Create your account <FaArrowRight />
              </Link>
              <Link to="/login" className="reference-secondary-btn">
                Sign in
              </Link>
            </div>
            <div className="reference-proof">
              <span><FaShieldAlt /> Secure authentication</span>
              <span><FaUserFriends /> Skill-based discovery</span>
              <span><FaExchangeAlt /> Structured exchanges</span>
            </div>
          </div>
        </div>
      </section>

      <section className="reference-feature-panel" aria-label="Core SkillSwap capabilities">
        <div className="se-container reference-feature-grid">
          {features.map((feature) => (
            <article className="reference-feature" key={feature.title}>
              <span className="reference-feature-icon">{feature.icon}</span>
              <div>
                <h3>{feature.title}</h3>
                <p>{feature.text}</p>
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className="landing-section reference-workflow-section" id="how-it-works">
        <div className="se-container">
          <div className="reference-section-heading">
            <span className="eyebrow muted">How it works</span>
            <h2>From skill profile to successful exchange.</h2>
          </div>

          <div className="reference-steps">
            {steps.map((step, index) => (
              <React.Fragment key={step.number}>
                <article className="reference-step">
                  <div className="reference-step-icon">{step.icon}</div>
                  <span className="reference-step-number">{step.number}</span>
                  <h3>{step.title}</h3>
                  <p>{step.text}</p>
                </article>
                {index < steps.length - 1 && <div className="reference-step-connector" aria-hidden="true" />}
              </React.Fragment>
            ))}
          </div>
        </div>
      </section>

      <section className="landing-cta reference-cta-section">
        <div className="se-container reference-cta">
          <div>
            <span className="eyebrow">Ready to exchange skills?</span>
            <h2>Create your profile. Find your people. Start learning.</h2>
            <p>Build your skill profile, discover compatible peers, and turn knowledge into meaningful exchanges.</p>
          </div>
          <Link to="/register" className="reference-cta-button">
            Get started <FaArrowRight />
          </Link>
        </div>
      </section>

      <footer className="public-footer reference-footer">
        <div className="se-container reference-footer-main">
          <div>
            <Link className="brand-lockup footer-brand" to="/" aria-label="SkillSwap home">
              <span className="brand-mark"><FaGraduationCap /></span>
              <span><strong>SkillSwap</strong><small>Peer learning network</small></span>
            </Link>
            <p>Exchange practical knowledge through structured peer learning.</p>
          </div>
          <div className="reference-footer-links">
            <span className="footer-label">Platform</span>
            <Link to="/">Home</Link>
            <a href="#how-it-works">How it works</a>
            <Link to="/login">Sign in</Link>
            <Link to="/register">Get started</Link>
          </div>
        </div>
        <div className="se-container footer-bottom">
          <span>© {new Date().getFullYear()} SkillSwap</span>
          <span>Peer learning through structured skill exchange.</span>
        </div>
      </footer>
    </div>
  )
}
