const TurnIndicator = ({ isMyTurn, opponentName }) => (
  <div
    className={`text-center py-3 px-4 rounded mb-4 font-medium ${
      isMyTurn ? 'bg-green-600/20 text-green-400 animate-pulse' : 'bg-slate-700 text-slate-400'
    }`}
  >
    {isMyTurn ? 'Sua vez de atirar! 🎯' : `Vez de ${opponentName || 'oponente'}...`}
  </div>
);

export default TurnIndicator;
