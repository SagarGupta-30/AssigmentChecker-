import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import apiClient from '../api/client'
import PageShell from '../components/PageShell'

const statusClass = {
  CORRECT: 'status correct',
  WRONG: 'status wrong',
  UNATTEMPTED: 'status pending'
}

export default function ResultPage() {
  const { submissionId } = useParams()
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    loadResult()
  }, [submissionId])

  const loadResult = async () => {
    setLoading(true)
    setError('')
    try {
      const response = await apiClient.get(`/api/submissions/${submissionId}/result`)
      setResult(response.data)
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to load result.')
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <PageShell title="Result">
        <p className="panel">Loading result...</p>
      </PageShell>
    )
  }

  return (
    <PageShell title="Evaluation Result">
      {error && <p className="alert error">{error}</p>}
      {!result && !error && <p className="panel">No result available.</p>}

      {result && (
        <>
          <section className="panel stats-grid">
            <div className="stat-card">
              <p>Correct</p>
              <strong>{result.correct}</strong>
            </div>
            <div className="stat-card">
              <p>Wrong</p>
              <strong>{result.wrong}</strong>
            </div>
            <div className="stat-card">
              <p>Not Attempted</p>
              <strong>{result.notAttempted}</strong>
            </div>
            <div className="stat-card">
              <p>Percentage</p>
              <strong>{result.percentage}%</strong>
            </div>
          </section>

          <section className="panel">
            <h2>Question-wise Breakdown</h2>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Q.No</th>
                    <th>Topic</th>
                    <th>Expected</th>
                    <th>Your Answer</th>
                    <th>Status</th>
                    <th>Explanation</th>
                  </tr>
                </thead>
                <tbody>
                  {result.questions.map((question) => (
                    <tr key={question.questionNumber}>
                      <td>{question.questionNumber}</td>
                      <td>{question.topic}</td>
                      <td>{question.expectedAnswer}</td>
                      <td>{question.studentAnswer || '-'}</td>
                      <td>
                        <span className={statusClass[question.status] || 'status'}>{question.status}</span>
                      </td>
                      <td>{question.explanation}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </>
      )}
    </PageShell>
  )
}
