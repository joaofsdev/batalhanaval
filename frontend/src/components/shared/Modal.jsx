import { useEffect, useCallback } from 'react';

const Modal = ({ open, onClose, children }) => {
  const handleKeyDown = useCallback(
    (e) => {
      if (e.key === 'Escape') onClose?.();
    },
    [onClose]
  );

  useEffect(() => {
    if (!open) return;
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [open, handleKeyDown]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-[200] flex items-center justify-center bg-black/70 backdrop-blur-sm"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
    >
      <div
        className="relative w-full max-w-lg mx-4 bg-surface-container border border-outline-variant shadow-[0_8px_40px_rgba(0,0,0,0.6)] animate-in fade-in"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          onClick={onClose}
          className="absolute top-3 right-3 text-on-surface-variant hover:text-on-surface transition-colors z-10"
          aria-label="Fechar"
        >
          <span className="material-symbols-outlined text-lg">close</span>
        </button>

        {children}
      </div>
    </div>
  );
};

export default Modal;
