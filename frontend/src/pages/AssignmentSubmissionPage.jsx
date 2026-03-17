import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import apiClient from '../api/client'
import PageShell from '../components/PageShell'

const createEmptyAnswers = (count) =>
  Array.from({ length: count }, (_, index) => ({
    questionNumber: index + 1,
    answer: ''
  }))

export default function AssignmentSubmissionPage() {
  const { assignmentId } = useParams()
  const navigate = useNavigate()

  const [assignment, setAssignment] = useState(null)
  const [answers, setAnswers] = useState([])
  const [imageFile, setImageFile] = useState(null)
  const [questionImageUrl, setQuestionImageUrl] = useState('')
  const [questionImageLoading, setQuestionImageLoading] = useState(false)
  const [questionImageError, setQuestionImageError] = useState('')
  const [loading, setLoading] = useState(true)
  const [extracting, setExtracting] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [ocrMessage, setOcrMessage] = useState('')

  useEffect(() => {
    loadAssignment()
  }, [assignmentId])

  useEffect(
    () => () => {
      if (questionImageUrl) {
        URL.revokeObjectURL(questionImageUrl)
      }
    },
    [questionImageUrl]
  )

  const loadAssignment = async () => {
    setLoading(true)
    setError('')
    setQuestionImageError('')
    try {
      const response = await apiClient.get(`/api/assignments/${assignmentId}`)
      setAssignment(response.data)
      setAnswers(createEmptyAnswers(response.data.numberOfQuestions))

      if (response.data.questionImageAvailable) {
        await loadQuestionImage()
      } else {
        setQuestionImageUrl((prev) => {
          if (prev) URL.revokeObjectURL(prev)
          return ''
        })
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to load assignment.')
    } finally {
      setLoading(false)
    }
  }

  const loadQuestionImage = async () => {
    setQuestionImageLoading(true)
    setQuestionImageError('')

    try {
      const response = await apiClient.get(`/api/assignments/${assignmentId}/question-image`, {
        responseType: 'blob'
      })
      const nextUrl = URL.createObjectURL(response.data)
      setQuestionImageUrl((prev) => {
        if (prev) URL.revokeObjectURL(prev)
        return nextUrl
      })
    } catch (err) {
      setQuestionImageError(err.response?.data?.message || 'Unable to load question image.')
    } finally {
      setQuestionImageLoading(false)
    }
  }

  const updateAnswer = (index, value) => {
    setAnswers((prev) => {
      const next = [...prev]
      next[index] = { ...next[index], answer: value }
      return next
    })
  }

  const extractFromImage = async () => {
    if (!imageFile) {
      setError('Please choose an image first.')
      return
    }

    setExtracting(true)
    setError('')
    setOcrMessage('')

    try {
      const formData = new FormData()
      formData.append('file', imageFile)

      const response = await apiClient.post('/api/submissions/ocr/extract', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })

      const parsed = response.data.parsedAnswers || []
      const parsedMap = new Map(parsed.map((item) => [item.questionNumber, item.answer]))

      setAnswers((prev) =>
        prev.map((answer) => ({
          ...answer,
          answer: parsedMap.get(answer.questionNumber) || answer.answer
        }))
      )

      if (parsed.length === 0) {
        setOcrMessage('OCR completed, but no numbered answers were detected.')
      } else {
        setOcrMessage(`OCR extracted ${parsed.length} answers. Please review before submit.`)
      }
    } catch (err) {
      setError(err.response?.data?.message || 'OCR extraction failed.')
    } finally {
      setExtracting(false)
    }
  }

  const submitAssignment = async (event) => {
    event.preventDefault()
    setSubmitting(true)
    setError('')

    try {
      let response
      if (imageFile) {
        const formData = new FormData()
        formData.append('file', imageFile)
        formData.append('answersJson', JSON.stringify(answers))

        response = await apiClient.post(
          `/api/submissions/assignment/${assignmentId}/with-image`,
          formData,
          { headers: { 'Content-Type': 'multipart/form-data' } }
        )
      } else {
        response = await apiClient.post(`/api/submissions/assignment/${assignmentId}`, { answers })
      }

      navigate(`/result/${response.data.submissionId}`)
    } catch (err) {
      setError(err.response?.data?.message || 'Submission failed.')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <PageShell title="Assignment Submission">
        <p className="panel">Loading assignment...</p>
      </PageShell>
    )
  }

  return (
    <PageShell title="Assignment Submission">
      {error && <p className="alert error">{error}</p>}
      {ocrMessage && <p className="alert success">{ocrMessage}</p>}

      <section className="panel">
        <h2>{assignment?.title}</h2>
        <p className="muted-line">Total Questions: {assignment?.numberOfQuestions}</p>

        {assignment?.questionImageAvailable && (
          <div className="question-image-wrap">
            <h3>Question Set Image</h3>
            {questionImageLoading && <p className="muted-line">Loading question image...</p>}
            {questionImageError && <p className="alert error">{questionImageError}</p>}
            {!questionImageLoading && questionImageUrl && (
              <img src={questionImageUrl} alt="Question set uploaded by teacher" className="question-image" />
            )}
          </div>
        )}

        <div className="upload-row">
          <label className="upload-file-label">
            Upload Answer Sheet (Optional)
            <input
              type="file"
              accept="image/*"
              onChange={(event) => setImageFile(event.target.files?.[0] || null)}
            />
          </label>
          {imageFile && <span className="muted-line">{imageFile.name}</span>}
          <button type="button" className="btn ghost" onClick={extractFromImage} disabled={extracting}>
            {extracting ? 'Extracting...' : 'Extract Answers from Image'}
          </button>
        </div>

        <form onSubmit={submitAssignment} className="form-grid">
          <div className="answer-grid">
            {answers.map((item, index) => (
              <label key={item.questionNumber}>
                Question {item.questionNumber}
                <textarea
                  value={item.answer}
                  onChange={(event) => updateAnswer(index, event.target.value)}
                  placeholder="Type your answer here"
                  rows={2}
                />
              </label>
            ))}
          </div>

          <button type="submit" className="btn" disabled={submitting}>
            {submitting ? 'Submitting...' : 'Submit Assignment'}
          </button>
        </form>
      </section>
    </PageShell>
  )
}
