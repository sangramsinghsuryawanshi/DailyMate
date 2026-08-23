import { createContext, useContext, useState, useCallback } from 'react'
import { createPortal } from 'react-dom'
import './Toast.css'

const ToastContext = createContext({
  showToast: () => {},
  success: () => {},
  error: () => {},
  warning: () => {},
  info: () => {},
})

let toastIdCounter = 0

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  const showToast = useCallback(({ type = 'info', title, message, duration = 4000 }) => {
    const id = ++toastIdCounter
    const newToast = { id, type, title, message }

    setToasts((prev) => [...prev, newToast])

    if (duration > 0) {
      setTimeout(() => {
        removeToast(id)
      }, duration)
    }

    return id
  }, [removeToast])

  const success = useCallback((message, title) => showToast({ type: 'success', message, title }), [showToast])
  const error = useCallback((message, title) => showToast({ type: 'danger', message, title }), [showToast])
  const warning = useCallback((message, title) => showToast({ type: 'warning', message, title }), [showToast])
  const info = useCallback((message, title) => showToast({ type: 'info', message, title }), [showToast])

  return (
    <ToastContext.Provider value={{ showToast, success, error, warning, info }}>
      {children}
      {createPortal(
        <div className="dm-toast-viewport" aria-live="polite">
          {toasts.map((toast) => (
            <div
              key={toast.id}
              className={`dm-toast dm-toast--${toast.type}`}
              role="alert"
            >
              <div className="dm-toast__content">
                {toast.title && <strong className="dm-toast__title">{toast.title}</strong>}
                <p className="dm-toast__message">{toast.message}</p>
              </div>
              <button
                type="button"
                className="dm-toast__close"
                onClick={() => removeToast(toast.id)}
                aria-label="Dismiss notification"
              >
                ✕
              </button>
            </div>
          ))}
        </div>,
        document.body
      )}
    </ToastContext.Provider>
  )
}

export function useToast() {
  return useContext(ToastContext)
}

export default ToastProvider
