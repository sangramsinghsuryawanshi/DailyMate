import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import Pagination from './Pagination'

describe('Pagination Component', () => {
  it('renders nothing when totalElements is 0 and totalPages is <= 1', () => {
    const { container } = render(
      <Pagination page={0} totalPages={0} totalElements={0} />
    )
    expect(container.firstChild).toBeNull()
  })

  it('renders correctly with pagination info and controls', () => {
    render(
      <Pagination
        page={0}
        totalPages={5}
        totalElements={100}
        pageSize={20}
        onPageChange={vi.fn()}
      />
    )

    expect(screen.getByText(/Showing/i)).toBeInTheDocument()
    expect(screen.getByText('100')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Page 1' })).toBeInTheDocument()

    // Previous is disabled on first page
    expect(screen.getByRole('button', { name: 'Previous Page' })).toBeDisabled()
    // Next is enabled
    expect(screen.getByRole('button', { name: 'Next Page' })).not.toBeDisabled()
  })

  it('calls onPageChange when clicking Next or a page number', () => {
    const handlePageChange = vi.fn()
    render(
      <Pagination
        page={1}
        totalPages={5}
        totalElements={100}
        pageSize={20}
        onPageChange={handlePageChange}
      />
    )

    fireEvent.click(screen.getByRole('button', { name: 'Next Page' }))
    expect(handlePageChange).toHaveBeenCalledWith(2)

    fireEvent.click(screen.getByRole('button', { name: 'Previous Page' }))
    expect(handlePageChange).toHaveBeenCalledWith(0)

    fireEvent.click(screen.getByRole('button', { name: 'Page 4' }))
    expect(handlePageChange).toHaveBeenCalledWith(3)
  })

  it('disables Next button on last page', () => {
    render(
      <Pagination
        page={4}
        totalPages={5}
        totalElements={100}
        pageSize={20}
        onPageChange={vi.fn()}
      />
    )

    expect(screen.getByRole('button', { name: 'Next Page' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Previous Page' })).not.toBeDisabled()
  })

  it('calls onPageSizeChange when selecting a new page size', () => {
    const handlePageSizeChange = vi.fn()
    const handlePageChange = vi.fn()

    render(
      <Pagination
        page={2}
        totalPages={10}
        totalElements={200}
        pageSize={20}
        onPageChange={handlePageChange}
        onPageSizeChange={handlePageSizeChange}
      />
    )

    const select = screen.getByLabelText('Items per page')
    fireEvent.change(select, { target: { value: '50' } })

    expect(handlePageSizeChange).toHaveBeenCalledWith(50)
    expect(handlePageChange).toHaveBeenCalledWith(0) // Resets to first page
  })
})
