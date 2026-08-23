import { useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'
import './Modal.css'

/**
 * DailyMate Modal Dialog Primitive
 * Implements WCAG focus management, ESC key close, and portal rendering.
 */
export function Modal({
  isOpen,
  onClose,
  title,
  description,
  children,
  size = 'md',
  showCloseButton = true,
  closeOnBackdrop = true,
  className = '',
}) {
  const modalRef = useRef(null)
  const previousActiveElement = useRef(null)

  useEffect(() => {
    if (isOpen) {
      previousActiveElement.current = document.activeElement

      const handleKeyDown = (e) => {
        if (e.key === 'Escape') {
          onClose?.()
        }
      }

      document.addEventListener('keydown', handleKeyDown)
      document.body.style.overflow = 'hidden'

      // Focus modal container if nothing inside is already focused
      if (modalRef.current && !modalRef.current.contains(document.activeElement)) {
        modalRef.current.focus()
      }

      return () => {
        document.removeEventListener('keydown', handleKeyDown)
        document.body.style.overflow = ''
        if (previousActiveElement.current) {
          previousActiveElement.current.focus()
        }
      }
    }
  }, [isOpen, onClose])

  if (!isOpen) return null

  return createPortal(
    <div
      className="dm-modal-backdrop"
      onClick={closeOnBackdrop ? onClose : undefined}
    >
      <div
        ref={modalRef}
        className={`dm-modal dm-modal--${size} ${className}`.trim()}
        role="dialog"
        aria-modal="true"
        aria-labelledby={title ? 'dm-modal-title' : undefined}
        aria-describedby={description ? 'dm-modal-description' : undefined}
        tabIndex={-1}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="dm-modal__header">
          <div>
            {title && (
              <h2 id="dm-modal-title" className="dm-modal__title">
                {title}
              </h2>
            )}
            {description && (
              <p id="dm-modal-description" className="dm-modal__description">
                {description}
              </p>
            )}
          </div>
          {showCloseButton && (
            <button
              type="button"
              onClick={onClose}
              className="dm-modal__close"
              aria-label="Close dialog"
            >
              ✕
            </button>
          )}
        </div>

        <div className="dm-modal__body">{children}</div>
      </div>
    </div>,
    document.body
  )
}

export function ModalFooter({ children, className = '' }) {
  return <div className={`dm-modal__footer ${className}`.trim()}>{children}</div>
}

export default Modal
