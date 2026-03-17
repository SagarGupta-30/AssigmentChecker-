import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import TeacherDashboard from './pages/TeacherDashboard'
import StudentDashboard from './pages/StudentDashboard'
import AssignmentSubmissionPage from './pages/AssignmentSubmissionPage'
import ResultPage from './pages/ResultPage'
import ProtectedRoute from './components/ProtectedRoute'

function HomeRedirect() {
  const { isAuthenticated, user } = useAuth()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return user?.role === 'TEACHER'
    ? <Navigate to="/teacher" replace />
    : <Navigate to="/student" replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route
        path="/teacher"
        element={(
          <ProtectedRoute allowedRoles={['TEACHER']}>
            <TeacherDashboard />
          </ProtectedRoute>
        )}
      />

      <Route
        path="/student"
        element={(
          <ProtectedRoute allowedRoles={['STUDENT']}>
            <StudentDashboard />
          </ProtectedRoute>
        )}
      />

      <Route
        path="/assignment/:assignmentId/submit"
        element={(
          <ProtectedRoute allowedRoles={['STUDENT']}>
            <AssignmentSubmissionPage />
          </ProtectedRoute>
        )}
      />

      <Route
        path="/result/:submissionId"
        element={(
          <ProtectedRoute allowedRoles={['STUDENT', 'TEACHER']}>
            <ResultPage />
          </ProtectedRoute>
        )}
      />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
