import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Navigate, useLocation, useNavigate } from 'react-router'
import { AlertCircle, Eye, EyeOff, Info, Loader2 } from 'lucide-react'
import Logo from '../../components/ui/Logo'
import { useAuth } from './AuthContext'
import { getErrorMessage, getFieldErrors } from '../../api/client'
import { DEMO_ACCOUNTS, DEMO_PASSWORD } from './demoAccounts'
import { safeRedirectFor } from '../../routes/navConfig'
import useServerWake from './useServerWake'


const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export default function LoginPage() {
  const { status, user, login, notice } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [serverError, setServerError] = useState(null)
  const [showPassword, setShowPassword] = useState(false)
  const server = useServerWake()

  const {
    register,
    handleSubmit,
    setValue,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({ defaultValues: { email: '', password: '' } })

  const from = location.state?.from

  // Already signed in (or just signed in): go to the remembered page if allowed, else the dashboard.
  if (status === 'authenticated') {
    return <Navigate to={safeRedirectFor(user, from)} replace />
  }

  const onSubmit = async ({ email, password }) => {
    setServerError(null)
    try {
      const signedIn = await login(email.trim(), password)
      navigate(safeRedirectFor(signedIn, from), { replace: true })
    } catch (error) {
      Object.entries(getFieldErrors(error)).forEach(([field, message]) => setError(field, { message }))
      setServerError(getErrorMessage(error))
    }
  }

  const fillDemo = (email) => {
    setValue('email', email, { shouldValidate: true })
    setValue('password', DEMO_PASSWORD, { shouldValidate: true })
    setServerError(null)
  }

  return (
    <div className="flex min-h-screen">
      {/* Brand panel (desktop only) */}
      <section className="hidden w-[44%] flex-col justify-between bg-navy-900 p-10 text-white lg:flex">
        <Logo inverted />
        <div>
          <h2 className="text-2xl font-semibold leading-snug">Smarter Customer Relationships.<br />Better Banking Sales.</h2>
          <p className="mt-3 max-w-sm text-sm text-blue-100/80">
            One workspace for branch teams to manage customers, sales leads, follow-ups and targets.
          </p>
        </div>
        <p className="text-xs text-blue-100/60">Portfolio demonstration · fictional bank · synthetic data only</p>
      </section>

      {/* Sign-in form */}
      <main className="flex flex-1 items-center justify-center px-4 py-10">
        <div className="w-full max-w-sm">
          <div className="lg:hidden">
            <Logo />
          </div>
          <h1 className="mt-6 text-xl font-semibold text-ink lg:mt-0">Sign in to BankPro</h1>
          <p className="mt-1 text-sm text-ink-muted">Use your employee email and password.</p>

          {server === 'waking' && (
            <div role="status" className="mt-5 flex items-start gap-2 rounded-lg bg-warning-50 p-3 text-sm text-warning-600">
              <Loader2 className="mt-0.5 h-4 w-4 shrink-0 animate-spin" aria-hidden="true" />
              <span>Starting the demo server… Free hosting puts it to sleep when nobody is using it, so the first visit can take up to a minute. You can sign in now — it will wait.</span>
            </div>
          )}
          {server === 'unreachable' && !serverError && (
            <div role="alert" className="mt-5 flex items-start gap-2 rounded-lg bg-danger-50 p-3 text-sm text-danger-600">
              <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
              The server could not be reached. Check your connection and reload the page.
            </div>
          )}
          {notice && !serverError && (
            <div role="status" className="mt-5 flex items-start gap-2 rounded-lg bg-royal-50 p-3 text-sm text-royal-700">
              <Info className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
              {notice}
            </div>
          )}
          {serverError && (
            <div role="alert" className="mt-5 flex items-start gap-2 rounded-lg bg-danger-50 p-3 text-sm text-danger-600">
              <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
              {serverError}
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} noValidate className="mt-6 space-y-4">
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-ink">Email</label>
              <input
                id="email"
                type="email"
                autoComplete="username"
                aria-invalid={errors.email ? 'true' : 'false'}
                aria-describedby={errors.email ? 'email-error' : undefined}
                className="mt-1 block w-full rounded-lg border border-line bg-white px-3 py-2 text-sm shadow-sm focus:border-royal-500 focus:outline-none focus:ring-2 focus:ring-royal-100 aria-[invalid=true]:border-danger-600"
                {...register('email', {
                  required: 'Email is required',
                  validate: (v) => EMAIL_PATTERN.test(v.trim()) || 'Enter a valid email address',
                })}
              />
              {errors.email && <p id="email-error" className="mt-1 text-xs text-danger-600">{errors.email.message}</p>}
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-ink">Password</label>
              <div className="relative mt-1">
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  aria-invalid={errors.password ? 'true' : 'false'}
                  aria-describedby={errors.password ? 'password-error' : undefined}
                  className="block w-full rounded-lg border border-line bg-white px-3 py-2 pr-10 text-sm shadow-sm focus:border-royal-500 focus:outline-none focus:ring-2 focus:ring-royal-100 aria-[invalid=true]:border-danger-600"
                  {...register('password', { required: 'Password is required' })}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((v) => !v)}
                  className="absolute inset-y-0 right-0 flex items-center px-3 text-ink-muted hover:text-ink"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
              {errors.password && <p id="password-error" className="mt-1 text-xs text-danger-600">{errors.password.message}</p>}
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="flex w-full items-center justify-center gap-2 rounded-lg bg-navy-900 px-4 py-2.5 text-sm font-medium text-white hover:bg-navy-800 disabled:opacity-60"
            >
              {isSubmitting && <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />}
              {isSubmitting ? 'Signing in…' : 'Sign in'}
            </button>
          </form>

          <section aria-labelledby="demo-heading" className="mt-8 rounded-xl border border-line bg-white p-4">
            <h2 id="demo-heading" className="text-xs font-semibold uppercase tracking-wide text-ink-muted">
              Demo accounts
            </h2>
            <p className="mt-1 text-xs text-ink-muted">
              Password for all: <code className="rounded bg-gray-100 px-1">{DEMO_PASSWORD}</code>
            </p>
            <ul className="mt-3 space-y-1">
              {DEMO_ACCOUNTS.map((a) => (
                <li key={a.email}>
                  <button
                    type="button"
                    onClick={() => fillDemo(a.email)}
                    className="w-full rounded-md px-2 py-1.5 text-left text-sm hover:bg-royal-50"
                  >
                    <span className="font-medium text-ink">{a.label}</span>
                    <span className="block text-xs text-ink-muted">{a.email}</span>
                  </button>
                </li>
              ))}
            </ul>
          </section>
        </div>
      </main>
    </div>
  )
}
