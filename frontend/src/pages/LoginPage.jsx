import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import PageShell from '../components/PageShell'
import { getApiErrorMessage } from '../utils/apiError'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()

  const [form, setForm] = useState({ email: '', password: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleChange = (event) => {
    setForm((prev) => ({ ...prev, [event.target.name]: event.target.value }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setLoading(true)
    setError('')

    try {
      const data = await login(form)
      navigate(data.user.role === 'TEACHER' ? '/teacher' : '/student')
    } catch (err) {
      setError(getApiErrorMessage(err, 'Login failed. Please check your credentials.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <PageShell title="AI Assignment Checker">
      <section className="auth-wrap">
        <div className="panel auth-panel">
          <h2>Welcome Back</h2>
          <p>Log in as teacher or student to continue.</p>

          {error && <p className="alert error">{error}</p>}

          <form onSubmit={handleSubmit} className="form-grid">
            <label>
              Email
              <input
                type="email"
                name="email"
                value={form.email}
                onChange={handleChange}
                required
                placeholder="you@example.com"
              />
            </label>

            <label>
              Password
              <input
                type="password"
                name="password"
                value={form.password}
                onChange={handleChange}
                required
                placeholder="Enter password"
              />
            </label>

            <button type="submit" className="btn" disabled={loading}>
              {loading ? 'Signing in...' : 'Login'}
            </button>
          </form>

          <p className="muted-line">
            New user? <Link to="/register">Create account</Link>
          </p>
        </div>
      </section>
    </PageShell>
  )
}
