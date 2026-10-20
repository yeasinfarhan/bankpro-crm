import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useNavigate, useSearchParams } from 'react-router'
import { describe, expect, it } from 'vitest'
import useSearchText from './useSearchText'

function Harness() {
  const [params, setParams] = useSearchParams()
  const navigate = useNavigate()
  const [q, setQ] = useSearchText(params, (text) => setParams(text ? { q: text } : {}, { replace: true }))
  return (
    <>
      <input aria-label="Search" value={q} onChange={(e) => setQ(e.target.value)} />
      <p data-testid="url">{params.toString()}</p>
      <button type="button" onClick={() => navigate('/customers?q=Karim')}>Top-bar search</button>
    </>
  )
}

describe('useSearchText', () => {
  it('writes typing to the URL and picks up URL changes made elsewhere', async () => {
    render(<MemoryRouter initialEntries={['/customers?q=Rafiq']}><Harness /></MemoryRouter>)
    expect(screen.getByLabelText('Search')).toHaveValue('Rafiq')

    await userEvent.clear(screen.getByLabelText('Search'))
    await userEvent.type(screen.getByLabelText('Search'), 'Sohel')
    await waitFor(() => expect(screen.getByTestId('url')).toHaveTextContent('q=Sohel'))

    await userEvent.click(screen.getByRole('button', { name: 'Top-bar search' }))
    expect(screen.getByLabelText('Search')).toHaveValue('Karim')
    // The old text must not be written back over the new URL after the debounce delay.
    await new Promise((r) => setTimeout(r, 500))
    expect(screen.getByTestId('url')).toHaveTextContent('q=Karim')
  })
})
