import { useMemo } from 'react'
import './Pagination.css'

export function Pagination({
  page = 0,
  totalPages = 0,
  totalElements = 0,
  pageSize = 20,
  pageSizeOptions = [10, 20, 50, 100],
  onPageChange,
  onPageSizeChange,
  disabled = false,
  isLoading = false,
  showSizePicker = true,
  className = '',
}) {
  if (totalElements <= 0 && totalPages <= 1) {
    return null
  }

  // Calculate collapsed page range (e.g. 1 2 3 ... 10)
  const pageNumbers = useMemo(() => {
    if (totalPages <= 7) {
      return Array.from({ length: totalPages }, (_, i) => i)
    }

    const pages = []
    const current = page

    // Always include first page
    pages.push(0)

    if (current > 3) {
      pages.push('ellipsis-start')
    }

    const start = Math.max(1, current - 1)
    const end = Math.min(totalPages - 2, current + 1)

    for (let i = start; i <= end; i++) {
      pages.push(i)
    }

    if (current < totalPages - 4) {
      pages.push('ellipsis-end')
    }

    // Always include last page
    if (totalPages > 1) {
      pages.push(totalPages - 1)
    }

    return pages
  }, [page, totalPages])

  const startItem = page * pageSize + 1
  const endItem = Math.min((page + 1) * pageSize, totalElements)

  return (
    <nav
      className={`dm-pagination ${className}`}
      aria-label="Pagination Navigation"
      role="navigation"
    >
      {/* Items Summary Info */}
      <div className="dm-pagination__info">
        {totalElements > 0 ? (
          <span>
            Showing <strong>{startItem}</strong>–<strong>{endItem}</strong> of{' '}
            <strong>{totalElements}</strong> items
          </span>
        ) : (
          <span>Page {page + 1} of {totalPages || 1}</span>
        )}
      </div>

      {/* Navigation Controls */}
      <div className="dm-pagination__controls">
        <button
          type="button"
          className="dm-pagination__btn dm-pagination__btn--prev"
          onClick={() => onPageChange?.(page - 1)}
          disabled={page <= 0 || disabled || isLoading}
          aria-label="Previous Page"
        >
          ← Previous
        </button>

        {/* Desktop Page Numbers */}
        {pageNumbers.map((p, idx) => {
          if (p === 'ellipsis-start' || p === 'ellipsis-end') {
            return (
              <span key={`ellipsis-${idx}`} className="dm-pagination__ellipsis">
                …
              </span>
            )
          }

          const pageNum = Number(p)
          const isActive = pageNum === page

          return (
            <button
              key={pageNum}
              type="button"
              className={`dm-pagination__btn dm-pagination__btn-number ${
                isActive ? 'dm-pagination__btn--active' : ''
              }`}
              onClick={() => onPageChange?.(pageNum)}
              disabled={disabled || isLoading}
              aria-label={`Page ${pageNum + 1}`}
              aria-current={isActive ? 'page' : undefined}
            >
              {pageNum + 1}
            </button>
          )
        })}

        {/* Mobile View: Page X of Y */}
        <span className="dm-pagination__mobile-summary">
          Page {page + 1} of {totalPages || 1}
        </span>

        <button
          type="button"
          className="dm-pagination__btn dm-pagination__btn--next"
          onClick={() => onPageChange?.(page + 1)}
          disabled={page >= totalPages - 1 || disabled || isLoading}
          aria-label="Next Page"
        >
          Next →
        </button>
      </div>

      {/* Page Size Selector */}
      {showSizePicker && onPageSizeChange && (
        <div className="dm-pagination__size-picker">
          <label htmlFor="dm-pagination-page-size" style={{ fontSize: '0.8125rem' }}>
            Show:
          </label>
          <select
            id="dm-pagination-page-size"
            className="dm-pagination__select"
            value={pageSize}
            onChange={(e) => {
              onPageSizeChange(Number(e.target.value))
              onPageChange?.(0) // Reset to first page
            }}
            disabled={disabled || isLoading}
            aria-label="Items per page"
          >
            {pageSizeOptions.map((opt) => (
              <option key={opt} value={opt}>
                {opt} / page
              </option>
            ))}
          </select>
        </div>
      )}
    </nav>
  )
}

export default Pagination
