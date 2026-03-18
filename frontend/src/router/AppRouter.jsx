import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import AuthBootstrap from './AuthBootstrap.jsx'
import routes from './routes.jsx'

const router = createBrowserRouter(routes)

function AppRouter() {
  return (
    <AuthBootstrap>
      <RouterProvider router={router} />
    </AuthBootstrap>
  )
}

export default AppRouter
