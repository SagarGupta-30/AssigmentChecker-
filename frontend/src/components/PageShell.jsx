import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function PageShell({ title, children }) {
  const { user, isAuthenticated, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  const links = []
  if (isAuthenticated) {
    if (user?.role === 'TEACHER') {
      links.push({ to: '/teacher', label: 'Teacher Dashboard' })
    }
    if (user?.role === 'STUDENT') {
      links.push({ to: '/student', label: 'Student Dashboard' })
    }
  } else {
    links.push({ to: '/login', label: 'Login' })
    links.push({ to: '/register', label: 'Register' })
  }

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="page-bg">
      <header className="topbar">
        <div>
          <p className="brand-kicker">AI Evaluation Suite</p>
          <h1>{title}</h1>
        </div>
        <div className="topbar-actions">
          <nav>
            {links.map((link) => (
              <Link
                key={link.to}
                className={location.pathname === link.to ? 'nav-link active' : 'nav-link'}
                to={link.to}
              >
                {link.label}
              </Link>
            ))}
          </nav>
          {isAuthenticated && (
            <button type="button" className="btn ghost" onClick={handleLogout}>
              Logout
            </button>
          )}
        </div>
      </header>
      <main className="container">{children}</main>
    </div>
  )
}
