const OpponentInfo = ({ username }) => {
  if (!username) return null;

  return (
    <div className="flex items-center gap-3 px-4 py-3 border border-outline-variant bg-surface-container">
      <div className="w-10 h-10 rounded-full border border-error bg-error/10 flex items-center justify-center font-label-caps text-label-caps text-error">
        {username[0]?.toUpperCase() || '?'}
      </div>
      <div className="flex-1">
        <p className="font-mono-data text-mono-data text-on-surface">
          {username.toUpperCase()}
        </p>
        <p className="font-label-caps text-label-caps text-error text-[10px]">OPONENTE</p>
      </div>
      <span className="material-symbols-outlined text-error text-sm">swords</span>
    </div>
  );
};

export default OpponentInfo;
