import Modal, { ModalFooter } from '../../primitives/Modal/Modal'
import Button from '../../primitives/Button/Button'

/**
 * DailyMate ConfirmationDialog Pattern
 * Used for high-impact and destructive actions.
 */
export function ConfirmationDialog({
  isOpen,
  onClose,
  onConfirm,
  title = 'Confirm Action',
  description = 'Are you sure you want to proceed?',
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  variant = 'danger', // 'danger' | 'primary' | 'warning'
  isLoading = false,
  children,
}) {
  return (
    <Modal
      isOpen={isOpen}
      onClose={isLoading ? undefined : onClose}
      title={title}
      description={description}
      size="sm"
    >
      {children && <div style={{ marginBottom: '1rem' }}>{children}</div>}

      <ModalFooter>
        <Button variant="ghost" onClick={onClose} disabled={isLoading}>
          {cancelLabel}
        </Button>
        <Button
          variant={variant}
          onClick={onConfirm}
          isLoading={isLoading}
        >
          {confirmLabel}
        </Button>
      </ModalFooter>
    </Modal>
  )
}

export default ConfirmationDialog
