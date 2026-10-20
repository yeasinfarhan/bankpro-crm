import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import LoginPage from './LoginPage'
import { renderWithAuth, USERS } from '../../test/renderWithAuth'

function renderLogin(auth) {
  return renderWithAuth(
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<p>Dashboard home</p>} />
      <Route path="/team" element={<p>Team page</p>} />
    </Routes>,
    { route: '/login', auth },
  )
}

describe('LoginPage', () => {
  it('shows validation errors and does not call the API when fields are empty or invalid', async () => {
    const login = vi.fn()
    renderLogin({ login })

    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(await screen.findByText('Email is required')).toBeInTheDocument()
    expect(screen.getByText('Password is required')).toBeInTheDocument()

    await userEvent.type(screen.getByLabelText('Email'), 'not-an-email')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(await screen.findByText('Enter a valid email address')).toBeInTheDocument()
    expect(login).not.toHaveBeenCalled()
  })

  it('signs in with trimmed email and goes to the dashboard', async () => {
    const login = vi.fn().mockResolvedValue(USERS.officer)
    renderLogin({ login })

    await userEvent.type(screen.getByLabelText('Email'), '  nusrat.jahan@bankpro.demo ')
    await userEvent.type(screen.getByLabelText('Password'), 'BankPro@2026')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(login).toHaveBeenCalledWith('nusrat.jahan@bankpro.demo', 'BankPro@2026')
    expect(await screen.findByText('Dashboard home')).toBeInTheDocument()
  })

  it('does not send an officer to a remembered page they cannot open', async () => {
    const login = vi.fn().mockResolvedValue(USERS.officer)
    renderWithAuth(
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<p>Dashboard home</p>} />
        <Route path="/team" element={<p>Team page</p>} />
      </Routes>,
      { route: { pathname: '/login', state: { from: '/team' } }, auth: { login } },
    )
    await userEvent.type(screen.getByLabelText('Email'), 'nusrat.jahan@bankpro.demo')
    await userEvent.type(screen.getByLabelText('Password'), 'BankPro@2026')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(await screen.findByText('Dashboard home')).toBeInTheDocument()
  })

  it('shows the server message when credentials are rejected', async () => {
    const login = vi.fn().mockRejectedValue({
      response: { status: 401, data: { message: 'Invalid email or password.' } },
    })
    renderLogin({ login })

    await userEvent.type(screen.getByLabelText('Email'), 'nusrat.jahan@bankpro.demo')
    await userEvent.type(screen.getByLabelText('Password'), 'wrong')
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password.')
  })

  it('fills a demo account when clicked', async () => {
    renderLogin({ login: vi.fn() })
    await userEvent.click(screen.getByRole('button', { name: /Branch Manager · Motijheel/ }))
    expect(screen.getByLabelText('Email')).toHaveValue('manager.motijheel@bankpro.demo')
    expect(screen.getByLabelText('Password')).toHaveValue('BankPro@2026')
  })

  it('shows the session-expired notice passed from the auth context', () => {
    renderLogin({ notice: 'Your session has expired. Please sign in again.' })
    expect(screen.getByRole('status')).toHaveTextContent('Your session has expired')
  })
})
