import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import apiClient from '../api/client'
import { useAuth } from '../context/AuthContext'
import PageShell from '../components/PageShell'

export default function StudentDashboard() {
  const { user } = useAuth()
  const navigate = useNavigate()

  const [assignments, setAssignments] = useState([])
  const [submissions, setSubmissions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    setLoading(true)
    setError('')
    try {
      const [assignmentsRes, submissionsRes] = await Promise.all([
        apiClient.get('/api/assignments/available'),
        apiClient.get('/api/submissions/my')
      ])
      setAssignments(assignmentsRes.data)
      setSubmissions(submissionsRes.data)
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to load student dashboard.')
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <PageShell title="Student Dashboard">
        <p className="panel">Loading dashboard...</p>
      </PageShell>
    )
  }

  return (
    <PageShell title="Student Dashboard">
      <p className="muted-line">Welcome, {user?.name}</p>
      {error && <p className="alert error">{error}</p>}

      <section className="panel">
        <h2>Available Assignments</h2>
        <div className="card-grid">
          {assignments.length === 0 && <p className="muted-line">No assignments available right now.</p>}
          {assignments.map((assignment) => (
            <article className="mini-card" key={assignment.id}>
              <h3>{assignment.title}</h3>
              <p>{assignment.numberOfQuestions} questions</p>
              <p>By {assignment.teacherName}</p>
              <button
                type="button"
                className="btn"
                onClick={() => navigate(`/assignment/${assignment.id}/submit`)}
              >
                Start Submission
              </button>
            </article>
          ))}
        </div>
      </section>

      <section className="panel">
        <h2>My Submissions</h2>
        {submissions.length === 0 ? (
          <p className="muted-line">You have not submitted anything yet.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Assignment</th>
                  <th>Submitted At</th>
                  <th>Score</th>
                  <th>Result</th>
                </tr>
              </thead>
              <tbody>
                {submissions.map((item) => (
                  <tr key={item.submissionId}>
                    <td>{item.assignmentTitle}</td>
                    <td>{new Date(item.submittedAt).toLocaleString()}</td>
                    <td>{item.percentage == null ? '-' : `${item.percentage}%`}</td>
                    <td>
                      <button
                        type="button"
                        className="btn small"
                        onClick={() => navigate(`/result/${item.submissionId}`)}
                      >
                        View Result
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </PageShell>
  )
}
