import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import PageShell from '../components/PageShell'
import { getApiErrorMessage } from '../utils/apiError'

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()

  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    role: 'STUDENT'
  })
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
      const data = await register(form)
      navigate(data.user.role === 'TEACHER' ? '/teacher' : '/student')
    } catch (err) {
      setError(getApiErrorMessage(err, 'Unable to register right now.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <PageShell title="Create Account">
      <section className="auth-wrap">
        <div className="panel auth-panel">
          <h2>Join AI Assignment Checker</h2>
          <p>Create a teacher or student account.</p>

          {error && <p className="alert error">{error}</p>}

          <form onSubmit={handleSubmit} className="form-grid">
            <label>
              Name
              <input
                type="text"
                name="name"
                value={form.name}
                onChange={handleChange}
                required
                placeholder="Your full name"
              />
            </label>

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
                minLength={8}
                placeholder="At least 8 chars, include letter + number"
              />
            </label>

            <label>
              Role
              <select name="role" value={form.role} onChange={handleChange}>
                <option value="STUDENT">Student</option>
                <option value="TEACHER">Teacher</option>
              </select>
            </label>

            <button type="submit" className="btn" disabled={loading}>
              {loading ? 'Creating account...' : 'Register'}
            </button>
          </form>

          <p className="muted-line">
            Already registered? <Link to="/login">Login here</Link>
          </p>
        </div>
      </section>
    </PageShell>
  )
}
