import { useEffect, useState } from 'react';

const STYLES = {
  error: 'bg-red-600',
  success: 'bg-green-600',
  info: 'bg-blue-600',
};

const Toast = ({ message, type = 'info', onClose }) => {
  const [visible, setVisible] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      setVisible(false);
      onClose?.();
    }, 4000);
    return () => clearTimeout(timer);
  }, [onClose]);

  if (!visible) return null;

  return (
    <div
      className={`fixed top-4 right-4 z-50 px-4 py-3 rounded shadow-lg text-white ${STYLES[type]}`}
    >
      {message}
    </div>
  );
};

export default Toast;
