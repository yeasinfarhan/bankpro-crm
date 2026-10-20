import { BrowserRouter } from 'react-router'
import AuthProvider from './features/auth/AuthProvider'
import ToastProvider from './components/feedback/ToastProvider'
import AppRoutes from './routes/AppRoutes'

export default function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </ToastProvider>
    </BrowserRouter>
  )
}
