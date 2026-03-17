import { useEffect, useMemo, useState } from 'react'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts'
import apiClient from '../api/client'
import { useAuth } from '../context/AuthContext'
import PageShell from '../components/PageShell'

const PIE_COLORS = ['#0c7489', '#f28f3b']

const emptyQuestion = (index) => ({
  questionNumber: index + 1,
  questionType: 'MCQ',
  expectedAnswer: '',
  topic: ''
})

export default function TeacherDashboard() {
  const { user } = useAuth()

  const [assignments, setAssignments] = useState([])
  const [submissions, setSubmissions] = useState([])
  const [analytics, setAnalytics] = useState({
    averageScore: 0,
    highestScore: 0,
    highestScorer: null,
    totalSubmissions: 0,
    weakTopics: []
  })

  const [form, setForm] = useState({
    title: '',
    numberOfQuestions: 3,
    answerKey: [emptyQuestion(0), emptyQuestion(1), emptyQuestion(2)]
  })
  const [questionImageFile, setQuestionImageFile] = useState(null)
  const [questionImageInputKey, setQuestionImageInputKey] = useState(0)

  const [selectedAssignmentId, setSelectedAssignmentId] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const pieData = useMemo(
    () => [
      { name: 'Average', value: analytics.averageScore || 0 },
      { name: 'Highest', value: analytics.highestScore || 0 }
    ],
    [analytics]
  )

  const weakTopicData = analytics.weakTopics || []

  useEffect(() => {
    loadInitialData()
  }, [])

  const loadInitialData = async () => {
    setLoading(true)
    setError('')
    try {
      const [assignmentsRes, analyticsRes] = await Promise.all([
        apiClient.get('/api/assignments/my'),
        apiClient.get('/api/analytics/teacher')
      ])

      const fetchedAssignments = assignmentsRes.data
      setAssignments(fetchedAssignments)
      setAnalytics(analyticsRes.data)

      if (fetchedAssignments.length > 0) {
        const firstId = fetchedAssignments[0].id
        setSelectedAssignmentId(firstId)
        await loadSubmissions(firstId)
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load dashboard data.')
    } finally {
      setLoading(false)
    }
  }

  const loadSubmissions = async (assignmentId) => {
    try {
      const response = await apiClient.get(`/api/submissions/assignment/${assignmentId}`)
      setSubmissions(response.data)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load submissions.')
    }
  }

  const updateQuestionCount = (count) => {
    const numeric = Math.max(1, Number(count) || 1)
    setForm((prev) => {
      const next = [...prev.answerKey]
      if (next.length < numeric) {
        for (let i = next.length; i < numeric; i += 1) {
          next.push(emptyQuestion(i))
        }
      } else {
        next.length = numeric
      }

      return {
        ...prev,
        numberOfQuestions: numeric,
        answerKey: next.map((item, index) => ({ ...item, questionNumber: index + 1 }))
      }
    })
  }

  const updateQuestionField = (index, field, value) => {
    setForm((prev) => {
      const next = [...prev.answerKey]
      next[index] = {
        ...next[index],
        [field]: value
      }
      return { ...prev, answerKey: next }
    })
  }

  const handleCreateAssignment = async (event) => {
    event.preventDefault()
    setSaving(true)
    setError('')

    try {
      const payload = {
        title: form.title,
        numberOfQuestions: form.numberOfQuestions,
        answerKey: form.answerKey
      }
      const formData = new FormData()
      formData.append(
        'request',
        new Blob([JSON.stringify(payload)], { type: 'application/json' })
      )
      if (questionImageFile) {
        formData.append('questionImage', questionImageFile)
      }

      await apiClient.post('/api/assignments/with-image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })

      setForm({
        title: '',
        numberOfQuestions: 3,
        answerKey: [emptyQuestion(0), emptyQuestion(1), emptyQuestion(2)]
      })
      setQuestionImageFile(null)
      setQuestionImageInputKey((prev) => prev + 1)

      await loadInitialData()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create assignment.')
    } finally {
      setSaving(false)
    }
  }

  const selectAssignment = async (assignmentId) => {
    setSelectedAssignmentId(assignmentId)
    await loadSubmissions(assignmentId)
  }

  if (loading) {
    return (
      <PageShell title="Teacher Dashboard">
        <p className="panel">Loading dashboard...</p>
      </PageShell>
    )
  }

  return (
    <PageShell title="Teacher Dashboard">
      <p className="muted-line">Logged in as {user?.name}</p>
      {error && <p className="alert error">{error}</p>}

      <section className="grid two">
        <article className="panel">
          <h2>Create Assignment</h2>
          <form className="form-grid" onSubmit={handleCreateAssignment}>
            <label>
              Title
              <input
                type="text"
                required
                value={form.title}
                onChange={(e) => setForm((prev) => ({ ...prev, title: e.target.value }))}
                placeholder="Algebra Test 1"
              />
            </label>

            <label>
              Number of Questions
              <input
                type="number"
                min="1"
                value={form.numberOfQuestions}
                onChange={(e) => updateQuestionCount(e.target.value)}
              />
            </label>

            <div className="question-builder">
              {form.answerKey.map((question, index) => (
                <div className="question-row" key={question.questionNumber}>
                  <p>Q{question.questionNumber}</p>
                  <select
                    value={question.questionType}
                    onChange={(e) => updateQuestionField(index, 'questionType', e.target.value)}
                  >
                    <option value="MCQ">MCQ</option>
                    <option value="SHORT_ANSWER">Short Answer</option>
                  </select>
                  <input
                    type="text"
                    placeholder="Expected answer"
                    value={question.expectedAnswer}
                    onChange={(e) => updateQuestionField(index, 'expectedAnswer', e.target.value)}
                    required
                  />
                  <input
                    type="text"
                    placeholder="Topic"
                    value={question.topic}
                    onChange={(e) => updateQuestionField(index, 'topic', e.target.value)}
                    required
                  />
                </div>
              ))}
            </div>

            <label>
              Question Set Image (Optional)
              <input
                key={questionImageInputKey}
                type="file"
                accept="image/*"
                onChange={(event) => setQuestionImageFile(event.target.files?.[0] || null)}
              />
            </label>
            <p className="muted-line">
              Students will see this question image in their submission page.
            </p>

            <button type="submit" className="btn" disabled={saving}>
              {saving ? 'Saving...' : 'Create Assignment'}
            </button>
          </form>
        </article>

        <article className="panel">
          <h2>Performance Analytics</h2>
          <div className="stats-grid">
            <div className="stat-card">
              <p>Average Score</p>
              <strong>{analytics.averageScore}%</strong>
            </div>
            <div className="stat-card">
              <p>Highest Score</p>
              <strong>{analytics.highestScore}%</strong>
            </div>
            <div className="stat-card">
              <p>Highest Scorer</p>
              <strong>{analytics.highestScorer || 'N/A'}</strong>
            </div>
            <div className="stat-card">
              <p>Total Submissions</p>
              <strong>{analytics.totalSubmissions}</strong>
            </div>
          </div>

          <div className="chart-wrap">
            <h3>Average vs Highest</h3>
            <ResponsiveContainer width="100%" height={220}>
              <PieChart>
                <Pie data={pieData} dataKey="value" nameKey="name" outerRadius={80}>
                  {pieData.map((entry, index) => (
                    <Cell key={entry.name} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>

          <div className="chart-wrap">
            <h3>Weak Topics</h3>
            <ResponsiveContainer width="100%" height={240}>
              <BarChart data={weakTopicData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="topic" />
                <YAxis allowDecimals={false} />
                <Tooltip />
                <Bar dataKey="incorrectCount" fill="#ef6f6c" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </article>
      </section>

      <section className="grid two">
        <article className="panel">
          <h2>Your Assignments</h2>
          <div className="list-wrap">
            {assignments.length === 0 && <p className="muted-line">No assignments yet.</p>}
            {assignments.map((assignment) => (
              <button
                key={assignment.id}
                type="button"
                className={assignment.id === selectedAssignmentId ? 'list-item active' : 'list-item'}
                onClick={() => selectAssignment(assignment.id)}
              >
                <span>{assignment.title}</span>
                <small>
                  {assignment.numberOfQuestions} questions
                  {assignment.questionImageAvailable ? ' • Image attached' : ''}
                </small>
              </button>
            ))}
          </div>
        </article>

        <article className="panel">
          <h2>Student Submissions</h2>
          {submissions.length === 0 ? (
            <p className="muted-line">No submissions for selected assignment.</p>
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Student</th>
                    <th>Submitted At</th>
                    <th>Score</th>
                    <th>Result</th>
                  </tr>
                </thead>
                <tbody>
                  {submissions.map((row) => (
                    <tr key={row.submissionId}>
                      <td>{row.studentName}</td>
                      <td>{new Date(row.submittedAt).toLocaleString()}</td>
                      <td>{row.percentage == null ? '-' : `${row.percentage}%`}</td>
                      <td>
                        <a href={`/result/${row.submissionId}`}>View Result</a>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </article>
      </section>
    </PageShell>
  )
}
