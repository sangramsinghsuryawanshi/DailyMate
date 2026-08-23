import { useEffect, useRef } from 'react'
import './SearchBar.css'

/**
 * DailyMate SearchBar Primitive
 * Supports shortcut (⌘K / Ctrl+K), clear action, and instant filter dispatch.
 */
export function SearchBar({
  value,
  onChange,
  onClear,
  placeholder = 'Search DailyMate...',
  shortcut = true,
  className = '',
  ...props
}) {
  const inputRef = useRef(null)

  useEffect(() => {
    if (!shortcut) return

    const handleKeyDown = (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault()
        inputRef.current?.focus()
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [shortcut])

  return (
    <div className={`dm-searchbar ${className}`.trim()}>
      <span className="dm-searchbar__icon" aria-hidden="true">
        🔍
      </span>
      <input
        ref={inputRef}
        type="search"
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        aria-label={placeholder}
        className="dm-searchbar__input"
        {...props}
      />
      {value ? (
        <button
          type="button"
          onClick={() => {
            onClear?.()
            inputRef.current?.focus()
          }}
          className="dm-searchbar__clear"
          aria-label="Clear search"
        >
          ✕
        </button>
      ) : (
        shortcut && (
          <kbd className="dm-searchbar__kbd" title="Press ⌘K or Ctrl+K to search">
            ⌘K
          </kbd>
        )
      )}
    </div>
  )
}

export default SearchBar
