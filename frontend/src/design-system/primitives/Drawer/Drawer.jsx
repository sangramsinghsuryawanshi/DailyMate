import { useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'
import './Drawer.css'

/**
 * DailyMate Drawer Primitive
 * Supports positions: left, right, bottom (mobile sheet)
 */
export function Drawer({
  isOpen,
  onClose,
  title,
  description,
  children,
  position = 'right',
  size = 'md',
  showCloseButton = true,
  className = '',
}) {
  const drawerRef = useRef(null)

  useEffect(() => {
    if (isOpen) {
      const handleKeyDown = (e) => {
        if (e.key === 'Escape') onClose?.()
      }
      document.addEventListener('keydown', handleKeyDown)
      document.body.style.overflow = 'hidden'

      return () => {
        document.removeEventListener('keydown', handleKeyDown)
        document.body.style.overflow = ''
      }
    }
  }, [isOpen, onClose])

  if (!isOpen) return null

  return createPortal(
    <div className="dm-drawer-backdrop" onClick={onClose}>
      <div
        ref={drawerRef}
        className={`dm-drawer dm-drawer--${position} dm-drawer--${size} ${className}`.trim()}
        role="dialog"
        aria-modal="true"
        aria-labelledby={title ? 'dm-drawer-title' : undefined}
        tabIndex={-1}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="dm-drawer__header">
          <div>
            {title && <h2 id="dm-drawer-title" className="dm-drawer__title">{title}</h2>}
            {description && <p className="dm-drawer__description">{description}</p>}
          </div>
          {showCloseButton && (
            <button
              type="button"
              onClick={onClose}
              className="dm-drawer__close"
              aria-label="Close panel"
            >
              ✕
            </button>
          )}
        </div>

        <div className="dm-drawer__body">{children}</div>
      </div>
    </div>,
    document.body
  )
}

export function DrawerFooter({ children, className = '' }) {
  return <div className={`dm-drawer__footer ${className}`.trim()}>{children}</div>
}

export default Drawer
