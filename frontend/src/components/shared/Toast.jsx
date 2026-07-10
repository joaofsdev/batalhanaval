import { useEffect, useState } from 'react';

const ICONS = {
  success: 'check_circle',
  error: 'error',
  info: 'info',
};

const BORDER_COLORS = {
  success: 'border-primary',
  error: 'border-error',
  info: 'border-secondary',
};

const ICON_COLORS = {
  success: 'text-primary',
  error: 'text-error',
  info: 'text-secondary',
};

const BAR_COLORS = {
  success: 'bg-primary',
  error: 'bg-error',
  info: 'bg-secondary',
};

const Toast = ({ message, type = 'info', onClose }) => {
  const [visible, setVisible] = useState(true);
  const [isClosing, setIsClosing] = useState(false);

  useEffect(() => {
    setIsClosing(true);
    const timer = setTimeout(() => {
      setVisible(false);
      onClose?.();
    }, 4000);
    return () => clearTimeout(timer);
  }, [onClose]);

  if (!visible) return null;

  return (
    <div className={`fixed top-4 right-4 z-[100] flex items-start gap-3 p-4 bg-surface-container border-l-4 max-w-sm shadow-[0_4px_20px_rgba(0,0,0,0.5)] transition-all duration-300 font-mono-data text-mono-data text-on-surface ${BORDER_COLORS[type] || BORDER_COLORS.info}`}>
      <span className={`material-symbols-outlined text-sm ${ICON_COLORS[type] || ICON_COLORS.info}`}>
        {ICONS[type] || ICONS.info}
      </span>
      <p className="flex-1 text-[12px] leading-tight uppercase">{message}</p>

      <div className="absolute bottom-0 left-0 h-0.5 bg-outline-variant w-full">
        <div
          className={`h-full transition-all duration-[4000ms] ease-linear ${BAR_COLORS[type] || BAR_COLORS.info}`}
          style={{ width: isClosing ? '0%' : '100%' }}
        />
      </div>
    </div>
  );
};

export default Toast;
