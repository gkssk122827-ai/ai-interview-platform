import BaseLayout from '../components/layout/BaseLayout.jsx'
import AdminPage from '../pages/AdminPage.jsx'
import AdminJobPostingEditorPage from '../pages/AdminJobPostingEditorPage.jsx'
import ApplicationDocumentPage from '../pages/ApplicationDocumentPage.jsx'
import BookStorePage from '../pages/BookStorePage.jsx'
import CartPage from '../pages/CartPage.jsx'
import CoverLetterPage from '../pages/CoverLetterPage.jsx'
import DashboardPage from '../pages/DashboardPage.jsx'
import InterviewResultPage from '../pages/InterviewResultPage.jsx'
import InterviewSessionPage from '../pages/InterviewSessionPage.jsx'
import InterviewSetupPage from '../pages/InterviewSetupPage.jsx'
import JobPostingDetailPage from '../pages/JobPostingDetailPage.jsx'
import JobPostingPage from '../pages/JobPostingPage.jsx'
import LearningPage from '../pages/LearningPage.jsx'
import LearningSessionPage from '../pages/LearningSessionPage.jsx'
import LoginPage from '../pages/LoginPage.jsx'
import NotFoundPage from '../pages/NotFoundPage.jsx'
import OrderPage from '../pages/OrderPage.jsx'
import RegisterPage from '../pages/RegisterPage.jsx'
import ResumePage from '../pages/ResumePage.jsx'
import SignupPage from '../pages/SignupPage.jsx'
import { IndexRedirect, ProtectedRoute, PublicOnlyRoute, RoleRoute } from './RouteGuards.jsx'

const routes = [
  {
    path: '/',
    element: <BaseLayout />,
    children: [
      { index: true, element: <IndexRedirect /> },
      {
        element: <PublicOnlyRoute />,
        children: [
          { path: 'login', element: <LoginPage /> },
          { path: 'signup', element: <SignupPage /> },
          { path: 'auth/login', element: <LoginPage /> },
          { path: 'auth/register', element: <RegisterPage /> },
        ],
      },
      {
        element: <ProtectedRoute />,
        children: [
          { path: 'dashboard', element: <DashboardPage /> },
          { path: 'profile-documents', element: <ApplicationDocumentPage /> },
          { path: 'resume', element: <ResumePage /> },
          { path: 'cover-letter', element: <CoverLetterPage /> },
          { path: 'job-posting', element: <JobPostingPage /> },
          { path: 'job-posting/:jobPostingId', element: <JobPostingDetailPage /> },
          { path: 'interview/setup', element: <InterviewSetupPage /> },
          { path: 'interview/session', element: <InterviewSessionPage /> },
          { path: 'interview/result', element: <InterviewResultPage /> },
          { path: 'learning', element: <LearningPage /> },
          { path: 'learning/session', element: <LearningSessionPage /> },
          { path: 'books', element: <BookStorePage /> },
          { path: 'cart', element: <CartPage /> },
          { path: 'orders', element: <OrderPage /> },
          {
            element: <RoleRoute allowedRoles={['ADMIN']} />,
            children: [
              { path: 'admin', element: <AdminPage /> },
              { path: 'admin/job-postings/new', element: <AdminJobPostingEditorPage /> },
              { path: 'admin/job-postings/:jobPostingId/edit', element: <AdminJobPostingEditorPage /> },
            ],
          },
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]

export default routes
