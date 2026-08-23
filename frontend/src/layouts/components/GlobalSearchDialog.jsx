import { useState, useEffect, useMemo, useRef } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Modal } from '../../design-system'
import { searchDailyMate } from '../../services/searchService'
import './GlobalSearchDialog.css'

export function GlobalSearchDialog({
  isOpen,
  onClose,
  providers = [],
  expenses = [],
  medicines = [],
  emergencyContacts = [],
  bloodRequests = [],
  events = [],
  searchSuggestions = [],
}) {
  const [query, setQuery] = useState('')
  const [debouncedQuery, setDebouncedQuery] = useState('')
  const [selectedIndex, setSelectedIndex] = useState(0)
  const inputRef = useRef(null)
  const navigate = useNavigate()

  // 300ms Debounce for query search
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(query)
    }, 300)
    return () => clearTimeout(timer)
  }, [query])

  const results = useMemo(() => {
    return searchDailyMate(debouncedQuery, {
      providers,
      expenses,
      medicines,
      emergencyContacts,
      bloodRequests,
      events,
      extraItems: searchSuggestions,
    })
  }, [debouncedQuery, providers, expenses, medicines, emergencyContacts, bloodRequests, events, searchSuggestions])

  useEffect(() => {
    if (isOpen) {
      setQuery('')
      setDebouncedQuery('')
      setSelectedIndex(0)
      setTimeout(() => inputRef.current?.focus(), 50)
    }
  }, [isOpen])

  const handleKeyDown = (e) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setSelectedIndex((prev) => (prev + 1) % Math.max(results.length, 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setSelectedIndex((prev) => (prev - 1 + results.length) % Math.max(results.length, 1))
    } else if (e.key === 'Enter') {
      e.preventDefault()
      if (results[selectedIndex]) {
        navigate(results[selectedIndex].to)
        onClose()
      }
    }
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      size="md"
      showCloseButton={false}
      className="dm-search-modal"
    >
      <div className="dm-search-dialog" onKeyDown={handleKeyDown}>
        <div className="dm-search-dialog__input-row">
          <span className="dm-search-dialog__icon" aria-hidden="true">
            🔍
          </span>
          <input
            ref={inputRef}
            type="search"
            value={query}
            onChange={(e) => {
              setQuery(e.target.value)
              setSelectedIndex(0)
            }}
            placeholder="Search pages, expenses, medicines, services, community..."
            className="dm-search-dialog__input"
            aria-label="Global search input"
          />
          <kbd className="dm-search-dialog__esc" onClick={onClose} title="Press ESC to close">
            ESC
          </kbd>
        </div>

        <div className="dm-search-dialog__results" role="listbox">
          {!query.trim() ? (
            <div className="dm-search-dialog__quick-links">
              <span className="dm-search-dialog__section-label">Suggested Destinations</span>
              <div className="dm-search-dialog__tag-grid">
                {searchSuggestions.slice(0, 6).map((item) => (
                  <Link
                    key={item.id}
                    to={item.to}
                    onClick={onClose}
                    className="dm-search-dialog__tag"
                  >
                    {item.title}
                  </Link>
                ))}
              </div>
            </div>
          ) : results.length === 0 ? (
            <div className="dm-search-dialog__empty">
              <p>No results found for "{query}".</p>
              <small>Try searching for "expenses", "reminders", "blood", or "plumber".</small>
            </div>
          ) : (
            results.map((item, idx) => (
              <Link
                key={item.id || idx}
                to={item.to}
                onClick={onClose}
                className={`dm-search-dialog__item ${idx === selectedIndex ? 'selected' : ''}`}
                role="option"
                aria-selected={idx === selectedIndex}
              >
                <span className="dm-search-dialog__item-icon">{item.icon || '📄'}</span>
                <div className="dm-search-dialog__item-text">
                  <strong className="dm-search-dialog__item-title">{item.title}</strong>
                  {item.description && (
                    <span className="dm-search-dialog__item-desc">{item.description}</span>
                  )}
                </div>
                {item.domain && (
                  <span className="dm-search-dialog__item-badge">{item.domain}</span>
                )}
              </Link>
            ))
          )}
        </div>
      </div>
    </Modal>
  )
}

export default GlobalSearchDialog
